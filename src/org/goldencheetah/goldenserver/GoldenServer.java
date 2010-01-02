/* 
 * Copyright (c) 2006 Justin Knotzke (jknotzke@shampoo.ca)
 *
 * Additional contributions from:
 *     Steve Gribble    [ gribble {at} cs.washington.edu ]
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.goldencheetah.goldenserver;

// standard java imports
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

// imports from .jar's in lib/
import org.apache.log4j.Logger;


public class GoldenServer {
    static Logger logger = Logger.getLogger(GoldenServer.class.getName());

    /*
     * run() creates a server socket, and spins waiting for a connection.
     * For each connection that arrives, run() forks off a handler thread
     * to handle it.
     */
    public void run(int portnum) {
        ServerSocket server = null;
        Socket clientSocket = null;

        System.out.println("Starting up the GoldenServer (on port " + portnum + ")...");
        try {
            server = new ServerSocket(portnum);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.exit(1);
        }

        while (true) {
            try {
                logger.debug("waiting for a connection.");
                clientSocket = server.accept();
                logger.debug("got a connection!");
                RaceDispatcher raceDispatch = new RaceDispatcher(clientSocket);
                raceDispatch.start();
            } catch (IOException ioe) {
                logger.error(ioe);
            }
	}
    }

    public static void usage() {
        System.out.println("usage: java org.goldencheetah.goldenserver.GoldenServer [port]");
        System.exit(-1);
    }

    public static void main(String[] args) {
        int portnum = 9133;  // default port is 9133

        // If an argument is passed in, interpret it as a port number.
        // If not, we keep the default port of 9133.
        if (args.length == 1) {
            try {
                portnum = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                usage();
            }
            if ((portnum < 1) || (portnum > 65535)) {
                usage();
            }
        }
        GoldenServer gs = new GoldenServer();
        gs.run(portnum);
    }
}
