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
import java.io.IOException;
import java.net.Socket;

// imports from .jar's in lib/
import org.apache.log4j.Logger;


public class RobotRider {
    private static Logger logger =
        Logger.getLogger(GoldenServer.class.getName());
    public static String protoversion = "0.1";

    private String raceid;
    private String hostname;
    private int port;

    public RobotRider(String raceid, String hostname, int port) {
        this.raceid = raceid;
        this.hostname = hostname;
        this.port = port;
    }

    public void go() throws IOException {
        
    }

    public static void usage() {
        System.out.println("usage: java org.goldencheetah.goldenserver.RobotRider " +
                           "<raceid> <server_hostname> <server_port>");
        System.exit(-1);
    }

    public static void main(String[] args) {
        String raceid = "";
        String hostname = "";
        int    portnum = 0;

        // pull in arguments
        if (args.length != 3) {
            usage();
        }
        raceid = args[0];
        hostname = args[1];
        try {
            portnum = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            usage();
        }
        if ((portnum < 1) || (portnum > 65535)) {
            usage();
        }

        System.out.println("Starting Robotrider...");
        System.out.println("   raceid: " + raceid);
        System.out.println("   server hostname: " + hostname);
        System.out.println("   server port: " + portnum);
        RobotRider rr = new RobotRider(raceid, hostname, portnum);
        try {
            rr.go();
        } catch (IOException ioe) {
            System.out.println("IO exception in go(): " + ioe);
            ioe.printStackTrace();
        }
    }
}
