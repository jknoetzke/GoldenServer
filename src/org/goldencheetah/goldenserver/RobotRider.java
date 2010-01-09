/* 
 * Copyright (c) 2010 Steve Gribble [ gribble {at} cs.washington.edu ]
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

// standard java imports
import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.charset.Charset;

// imports from .jar's in lib/
import org.apache.log4j.Logger;


public class RobotRider {
    private static Logger logger =
        Logger.getLogger(GoldenServer.class.getName());
    public static String protoversion = "0.1";

    private static String raceid = null;
    private static String hostname = null;
    private static int port = -1;

    private Socket gssocket = null;
    private PrintWriter writer = null;
    private BufferedReader reader = null;

    public void go() throws IOException {
        Charset charset = Charset.forName("US-ASCII");

        while(true) {
            gssocket = new Socket(hostname, port);
            writer = new PrintWriter(new OutputStreamWriter(
                                     gssocket.getOutputStream(), charset),
                                     true);
            reader = new BufferedReader(new InputStreamReader(
                                        gssocket.getInputStream(), charset)
                                        );

            // figure out my rider's name and connect
            Random rm = new Random();
            int rideruid = rm.nextInt();
            if (rideruid < 0) rideruid *= -1;
            String ridername = "robotrider" + rideruid;
            String riderid = doConnect(ridername);

            // ride
            if (riderid != null) {
                doRide(riderid);
            }

            // finish up
            try { Thread.sleep(2000); } catch (InterruptedException ie) {}
            closeSock();
        }
    }

    private String doConnect(String name) {
        ProtocolHandler.HelloMessage hm =
            new ProtocolHandler.HelloMessage("0.1", raceid, name, 250, (float) 75.0);
        writer.print(hm.toString());
        writer.flush();

        ProtocolHandler.ProtocolMessage pm = getNextMessage();
        if (pm == null)
            return null;
        if (!(pm instanceof ProtocolHandler.HelloSucceedMessage))
            return null;

        ProtocolHandler.HelloSucceedMessage hsm =
            (ProtocolHandler.HelloSucceedMessage) pm;
        return hsm.riderid;
    }

    private void doRide(String riderid) {
        boolean done = false;
        Random rm = new Random();
        float distance_km = (float) 0.0;

        try {
            while (!done) {
                try {Thread.sleep(500);} catch (InterruptedException ie) {}
                int nextrand;

                nextrand = rm.nextInt();
                if (nextrand < 0) nextrand *= -1;
                int nextpower = 50 + (nextrand % 200);
                int nextcadence = 75 + (rm.nextInt() % 20);
                int nextheartrate = 130 + (nextrand % 20);
                float nextspeed = (float) 2000.0 + (nextrand % 20);
                distance_km = distance_km + 
                    (float)(nextspeed * (1.0/(2.0*3600.0)));
                ProtocolHandler.TelemetryMessage tm = new
                    ProtocolHandler.TelemetryMessage(raceid, riderid,
                                                     nextpower, nextcadence,
                                                     distance_km, nextheartrate,
                                                     nextspeed);
                writer.print(tm.toString());
                writer.flush();

                if (!reader.ready())
                    continue;

                while (reader.ready()) {
                    ProtocolHandler.ProtocolMessage nm =
                        getNextMessage();
                    if (nm == null) {
                        done = true;
                        break;
                    }
                    System.out.print(nm.toString());
                    if (nm instanceof ProtocolHandler.RaceConcludedMessage) {
                        ProtocolHandler.GoodbyeMessage gm =
                            new ProtocolHandler.GoodbyeMessage(raceid, riderid);
                        writer.print(gm.toString());
                        writer.flush();
                        done = true;
                        break;
                    }
                }
            }
        } catch (IOException ioe) {
        }
        return;
    }

    // a convenience routine to grab the next line from input, return
    // a ProtocolHandler.ProtocolMessage.  returns null on error.
    private ProtocolHandler.ProtocolMessage getNextMessage() {
        ProtocolHandler.ProtocolMessage pm = null;
        String nextline = null;

        try {
            nextline = reader.readLine();
            if (nextline == null) {
                return null;
            }
            pm = ProtocolHandler.parseLine(nextline);
            if (pm == null) {
                return null;
            }
        } catch (IOException ioe) {
            return null;
        }
        return pm;
    }

    // convenience routine to close sockets and buffered reader/writer
    private void closeSock() {
        if (reader != null) {
            try { reader.close(); } catch (IOException ioe) {}
            reader = null;
        }
        if (writer != null) {
            writer.close();
            writer = null;
        }
        if (gssocket != null) {
            try { gssocket.close(); } catch (IOException ioe) {}
            gssocket = null;
        }
    }

    public static void usage() {
        System.out.println("usage: java org.goldencheetah.goldenserver.RobotRider " +
                           "<raceid> <server_hostname> <server_port>");
        System.exit(-1);
    }

    public static void main(String[] args) {
        // pull in arguments
        if (args.length != 3) {
            usage();
        }
        RobotRider.raceid = args[0];
        RobotRider.hostname = args[1];
        try {
            RobotRider.port = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            usage();
        }
        if ((RobotRider.port < 1) || (RobotRider.port > 65535)) {
            usage();
        }

        System.out.println("Starting Robotrider...");
        System.out.println("   raceid: " + RobotRider.raceid);
        System.out.println("   server hostname: " + RobotRider.hostname);
        System.out.println("   server port: " + RobotRider.port);
        RobotRider rr = new RobotRider();
        try {
            rr.go();
        } catch (IOException ioe) {
            System.out.println("IO exception in go(): " + ioe);
            ioe.printStackTrace();
        }
    }
}
