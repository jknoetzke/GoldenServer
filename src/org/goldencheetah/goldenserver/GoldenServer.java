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
    public void run(int portnum, String gs_url) {
        ServerSocket server = null;
        Socket clientSocket = null;

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
        System.out.println("usage: java org.goldencheetah.goldenserver.GoldenServer " +
                           "<port> <goldenweb_url>");
        System.exit(-1);
    }

    public static void main(String[] args) {
        int    portnum = 0;
        String gs_url = "";

        // pull in arguments
        if (args.length != 2) {
            usage();
        }
        try {
            portnum = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            usage();
        }
        if ((portnum < 1) || (portnum > 65535)) {
            usage();
        }
        gs_url = args[1];

        System.out.println("Starting GoldenServer; port is:  " + portnum);
        System.out.println("The GoldenWeb polling URL is:    " + gs_url);
        GoldenServer gs = new GoldenServer();
        gs.run(portnum, gs_url);
    }
}
