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

public class ClientHandler extends Thread {
    static Logger logger = Logger.getLogger(ClientHandler.class.getName());
    static Hashtable<String,Race> activeRaces = new Hashtable<String,Race>();

    private BufferedReader in = null;
    private PrintWriter out = null;
    private Socket clientsock = null;
    private Rider rider = null;
    private Race race = null;
    private WebPoller poller = null;

    public ClientHandler(Socket clientsock, WebPoller poller) {
        this.clientsock = clientsock;
        this.poller = poller;
    }

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
                               in, out);
        
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

        // done with this client...

        // XXX do stuff here, like potentially garbage collecting
        // the race if no clients are left in it, and informing other
        // clients of the membership change.
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
        Race ret_race = activeRaces.get(raceid);
        if (ret_race == null) {
            // didn't find it.  enter critical section to try again,
            // and if fail, create race.
            synchronized(activeRaces) {
                ret_race = activeRaces.get(raceid);
                if (ret_race == null) {
                    ret_race = poller.findNewRace(raceid);
                }
                if (ret_race != null) {
                    activeRaces.put(raceid, ret_race);
                }
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
        out.print(hfm.toString());
        out.flush();
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
        out.print(hsm.toString());
        out.flush();
        logger.debug("sent HelloSucceedMessage");
    }

    // convenience routine to handle a TelemetryMessage.
    private boolean handleTelemetry(ProtocolHandler.TelemetryMessage tm) {
        boolean done = false;
        logger.debug("telemetry message from client...");
        // XXX -- handle telemetry here
        return done;
    }

    // convenience routine to handle a GoodbyeMessage.
    private boolean handleGoodbye(ProtocolHandler.GoodbyeMessage gm) {
        boolean done = true;
        logger.debug("got valid goodbye, client disconnecting...");
        return done;
    }
}
