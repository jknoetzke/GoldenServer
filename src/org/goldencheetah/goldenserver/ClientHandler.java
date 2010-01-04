/* 
 * Copyright (c) 2010 Steve Gribble [ gribble {at} cs.washington.edu ]
 *
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */

package org.goldencheetah.goldenserver;

import org.apache.log4j.Logger;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ClientHandler extends Thread {
    static Logger logger = Logger.getLogger(ClientHandler.class.getName());
    static Hashtable<String,Race> activeRaces = new Hashtable<String,Race>();

    private BufferedReader in = null;
    private PrintWriter out = null;
    private Socket clientsock = null;
    private Rider rider = null;
    private Race race = null;
    private WebPoller poller = null;
    private ClientWriter writer = null;
    private Object sync_concluded = new Object();
    private boolean sent_concluded = false;

    public ClientHandler(Socket clientsock, WebPoller poller) {
        this.clientsock = clientsock;
        this.poller = poller;
    }

    /*
     * Set up a thread for each client to handle writing stuff
     * back to the client.  Thread drains from a fixed capacity
     * queue.
     */
    public class ClientWriter extends Thread {
        private PrintWriter out;
        private boolean halt;
        private ArrayBlockingQueue<ProtocolHandler.ProtocolMessage> queue;
        private static final int QCAP = 40;

        public ClientWriter(PrintWriter out) {
            this.out = out;
            this.halt = false;
            this.queue =
                new ArrayBlockingQueue<ProtocolHandler.ProtocolMessage>(QCAP);
        }

        /*
         * Cause the ClientWriter thread to exit on next loop.
         */
        public void selfTerminate() {
            this.halt = true;
        }

        /*
         * Spin until queue is zero, then return.  Added a couple of
         * hack-ish delays; the first makes sure we don't
         * unnecessarily burn the CPU, the second gives the writer a
         * chance to flush the TCP socket before the caller closes it.
         */
        public void spinZero() {
            while (queue.size() > 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
            }
        }

        /*
         * Add a ProtocolMessage to the outgoing queue.  Returns false
         * if queue is full and message was not added.
         */
        public synchronized boolean add(ProtocolHandler.ProtocolMessage m) {
            return queue.offer(m);
        }

        /*
         * Add several ProtocolMessages to the outgoing queue.  Tries to
         * ensure there is enough space for all messages, but a race might
         * cause some and not all to be added.  Returns true if all were
         * added, false if not all were added.
         */
        public synchronized boolean add(ProtocolHandler.ProtocolMessage[] m) {
            if (queue.remainingCapacity() < m.length)
                return false;
            for (int i=0; i<m.length; i++) {
                if (queue.offer(m[i]) == false)
                    return false;
            }
            return true;
        }

        public void run() {
            // drain queue.  if queue is empty, block for up to 1s.
            // after draining queue or blocking, see if we're told to
            // self terminate, and if so, fall out bottom.
            while(!halt) {
                try {
                    ProtocolHandler.ProtocolMessage pm =
                        queue.poll(1000, TimeUnit.MILLISECONDS);
                    while (pm != null) {
                        out.print(pm.toString());
                        out.flush();
                        pm = queue.poll();
                    }
                } catch (java.lang.InterruptedException ie) {
                }
            }
        }
    }


    /*
     * Main run() for thread that pulls messages from the client.
     */
    public void run() {
        // set up the reader/writer
        if (!setupReaderWriter()) {
            closeSock(clientsock);
            return;
        }

        // read the first line, unmarshal it
        ProtocolHandler.HelloMessage hm = handleFirstLine();
        if (hm == null) {
            closeSock(clientsock);
            return;
        }
        rider = new Rider(hm.ridername, hm.ftp_watts, hm.weight_kg,
                               in, writer);
        
        // find the Race this rider wants
        race = findRace(hm.raceid);
        if (race == null) {
            // no such race!
            noSuchRace(hm.raceid);
            closeSock(clientsock);
            return;
        } else {
            // ack the race to the client, move on to loop
            ackRace();
        }

        // add the client to the race, send out a membership
        // update message to all other clients
        if (race.addClient(rider) == false) {
            // race is full; as a hack, send noSuchRace
            noSuchRace(hm.raceid);
            closeSock(clientsock);
            return;
        }
        race.sendMembershipUpdate();

        // loop ad infinitum, pulling in the next client message.
        boolean done = false;
        while(!done) {
            ProtocolHandler.ProtocolMessage pm = getNextMessage();
            if (pm == null) {
                done = true;
                continue;
            }

            // handle the next message here.
            if (pm instanceof ProtocolHandler.TelemetryMessage) {
                done = handleTelemetry((ProtocolHandler.TelemetryMessage) pm);
            } else if (pm instanceof ProtocolHandler.GoodbyeMessage) {
                done = handleGoodbye((ProtocolHandler.GoodbyeMessage) pm);
            } else {
                // unexpected message!
                logger.warn("unexpected message from client: '" +
                            pm.toString() + "'");
                done = true;
            }
        }

        // done with this client...drop the client from the race.
        // garbage collect the race if no clients are left in it, and
        // inform other clients of the membership change.
        race.dropClient(rider);
        synchronized(activeRaces) {
            if (race.numClients() == 0) {
                activeRaces.remove(race.getRaceid());
                race = null;
                logger.debug("race is empty; garbage collected it");
            }
        }
        if (race != null) {
            race.sendMembershipUpdate();
        }

        // clean up the socket and exit.
        closeSock(clientsock);
        return;
    }

    // a convenience routine to close a socket
    private void closeSock(Socket s) {
        if (s != null) {
            try {
                s.close();
            } catch (IOException ioe) {
            }
        }
        if (writer != null) {
            writer.selfTerminate();
        }
    }

    // a convenience routine to set up reader/writers
    private boolean setupReaderWriter() {
        try {
            this.in =
                new BufferedReader(
                    new InputStreamReader(
                        clientsock.getInputStream()));
            this.out =
                new PrintWriter(
                    new OutputStreamWriter(
                        clientsock.getOutputStream()));
            this.writer = new ClientWriter(out);
            this.writer.start();
        } catch (IOException ioe) {
            logger.debug("client connection dropped creating in/out");
            return false;
        }
        return true;
    }

    // a convenience routine to grab the next line from input, return
    // a ProtocolHandler.ProtocolMessage.  returns null on error.
    private ProtocolHandler.ProtocolMessage getNextMessage() {
        ProtocolHandler.ProtocolMessage pm = null;
        String nextline = null;

        try {
            nextline = in.readLine();
            if (nextline == null) {
                logger.debug("client connection dropped...");
                return null;
            }
            pm = ProtocolHandler.parseLine(nextline);
            if (pm == null) {
                logger.warn("bogus line from client: '" + nextline + "'");
                return null;
            }
        } catch (IOException ioe) {
            logger.debug("client connection dropped...");
            return null;
        }
        return pm;
    }

    // a convenience routine to handle the first line
    private ProtocolHandler.HelloMessage handleFirstLine() {
        ProtocolHandler.ProtocolMessage pm = getNextMessage();

        if (pm == null) return null;
        if (!(pm instanceof ProtocolHandler.HelloMessage)) {
            logger.warn("expected HelloMesssage, but got something else");
            return null;
        }
        ProtocolHandler.HelloMessage hm = (ProtocolHandler.HelloMessage) pm;
        return hm;
    }

    // convenience routine to find the right Race, or create it if not
    // already created.
    private Race findRace(String raceid) {
        Race ret_race = null;
        synchronized(activeRaces) {
            ret_race = activeRaces.get(raceid);
            if (ret_race == null) {
                ret_race = poller.findNewRace(raceid);
            }
            if (ret_race != null) {
                activeRaces.put(raceid, ret_race);
            }
        }
        return ret_race;
    }

    // convenience routine to tell client that no such race exists.
    private void noSuchRace(String raceid) {
        ProtocolHandler.HelloFailMessage hfm =
            new ProtocolHandler.HelloFailMessage(
                GoldenServer.protoversion,
                "nosuchrace",
                raceid);
        writer.add(hfm);
        writer.spinZero();
        logger.debug("client asked for race that doesn't exist ('" +
                     raceid + "')");
    }

    // convenience routine to ack the race to the rider.
    private void ackRace() {
        ProtocolHandler.HelloSucceedMessage hsm =
            new ProtocolHandler.HelloSucceedMessage(
                GoldenServer.protoversion,
                race.getRaceid(),
                rider.getRiderid(),
                race.getRacedistanceKm());
        writer.add(hsm);
        logger.debug("sent HelloSucceedMessage");
    }

    // convenience routine to handle a TelemetryMessage.
    private boolean handleTelemetry(ProtocolHandler.TelemetryMessage tm) {
        boolean done;
        logger.debug("telemetry message from client...");
        done = race.telemetryUpdate(rider, tm);
        if (!done) return false;

        synchronized(sync_concluded) {
            if (!sent_concluded)
                race.sendRaceConcluded();
            sent_concluded = true;
        }
        return false;   // don't drop clients until they say goodbye
    }

    // convenience routine to handle a GoodbyeMessage.
    private boolean handleGoodbye(ProtocolHandler.GoodbyeMessage gm) {
        boolean done = true;
        logger.debug("got valid goodbye, client disconnecting...");
        return done;
    }
}
