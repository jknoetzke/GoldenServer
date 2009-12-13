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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;
import java.util.Vector;

public class ClientSocket extends Thread 
{
	  static Vector<ClientSocket> handlers = new Vector<ClientSocket>();
	  private BufferedReader in;
	  private PrintWriter out;
	  private String uuid;
	
	private Socket clientSock;

	public ClientSocket(Socket _clientSock)
	{
	
		System.out.println("Firing up the Client Sock..");
		clientSock = _clientSock;
		try {
			in = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
			out = new PrintWriter(new OutputStreamWriter(clientSock.getOutputStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		}
	
	
	@Override
	public void run() 
	{
		try
		{
			String strIn;
			synchronized(handlers)
			{
				handlers.addElement(this);
			}
			notifyRiderChange(true);

			while(true)
			{
				strIn = in.readLine();
				if(strIn == null)
					return;
				for(int i = 0; i < handlers.size(); i++) 
				{	
					synchronized(handlers) 
					{
						ClientSocket handler = (ClientSocket)handlers.elementAt(i);
						handler.out.println(strIn + "\r");
						handler.out.flush();
					}
				}
			}
		}
	    catch(IOException ioe) 
		{
			    ioe.printStackTrace();
		} 
		finally 
		{
			    try {
				in.close();
				out.close();
				clientSock.close();
		} 
	        catch(IOException ioe) 
			{
	        	System.out.println(ioe);
			    
			}     
	        finally 
			    {
			    	synchronized(handlers) 
			    	{
			    		handlers.removeElement(this);
			    		notifyRiderChange(false);
			    	}
			    }
			}
	}
	
	private void notifyRiderChange(boolean newRider)
	{
		if(newRider)
			uuid = UUID.randomUUID().toString();
		synchronized(handlers) 
		{	
			for(int i = 0; i < handlers.size(); i++) 
			{	
				ClientSocket handler = (ClientSocket)handlers.elementAt(i);
				if(newRider)
					handler.out.println("<newracerid='" + getClientList() +"'" + "/>\r");
				else
					handler.out.println("<racerdropped='" + this.uuid +"'" + "/>\r");
					  
				handler.out.flush();
			}
		}
	}
	
	public String getID() { return uuid; }
	
	
	private String getClientList()
	{
		StringBuffer clientList = new StringBuffer();
		for(int x=0; x< handlers.size(); x++)
		{
			clientList.append(handlers.get(x).getID());
		    if(x < (handlers.size() -1))
		    	clientList.append(",");
		}
		
		return clientList.toString();
	}

}

