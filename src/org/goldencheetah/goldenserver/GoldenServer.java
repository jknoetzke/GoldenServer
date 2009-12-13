/* 
 * Copyright (c) 2006 Justin Knotzke (jknotzke@shampoo.ca)
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class GoldenServer extends Thread {

	private ServerSocket server;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new GoldenServer();

	}

	public GoldenServer() {
		try {
			System.out.println("Starting Server..");
			server = new ServerSocket(6666);
			this.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() 
	{
		Socket clientSocket = null;
		while (true) 
		{
			try {
				System.out.println("Waiting for a connection..");
				clientSocket = server.accept();
				System.out.println("Got a connection!..");
				ClientSocket client = new ClientSocket(clientSocket);
				client.start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
