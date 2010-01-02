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

    private BufferedReader in;
    private PrintWriter out;
    private Socket clientsock;

    public ClientHandler(Socket clientsock) {
        this.clientsock = clientsock;
    }

    public void run() {
        // set up the reader/writer
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
            closeSock(clientsock);
            return;
        }

        // read the first line, unmarshal it
        ProtocolHandler.ProtocolMessage pm = null;
        String firstline = null;
        try {
            firstline = in.readLine();
            if (firstline == null) {
                logger.debug("client connection dropped reading first line");
            } else {
                pm = ProtocolHandler.parseLine(firstline);
            }
        } catch (IOException ioe) {
            logger.debug("client connection dropped reading first line");
            closeSock(clientsock);
            return;
        }
        if ((pm == null) || !(pm instanceof ProtocolHandler.HelloMessage)) {
            logger.warn("bogus first line from client: '" + firstline + "'");
            closeSock(clientsock);
            return;
        }
        ProtocolHandler.HelloMessage hm = (ProtocolHandler.HelloMessage) pm;

        // loop ad infinitum, pulling in the next client message.
        boolean done = false;
        while(!done) {
            String nextline = null;

            try {
                nextline = in.readLine();
                if (nextline == null) {
                    logger.debug("client connection dropped");
                    done = true;
                    continue;
                }
            } catch (IOException ioe) {
                logger.debug("client connection dropped");
                done = true;
                continue;
            }
        }

        // done with this client...
        // XXX do stuff here
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
}
