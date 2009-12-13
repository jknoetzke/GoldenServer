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
					handler.out.println("<newracerid='" + this.uuid +"'" + "/>\r");
				else
					handler.out.println("<racerdropped='" + this.uuid +"'" + "/>\r");
					  
				handler.out.flush();
			}
		}
	}

}

