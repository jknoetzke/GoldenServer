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
