package org.goldencheetah.goldenserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Vector;

import org.apache.log4j.Logger;

public class RaceDispatcher extends Thread
{
	static Logger logger = Logger.getLogger(RaceDispatcher.class.getName());
	private BufferedReader in;
	private PrintWriter out;
	private Socket clientSock;
	private Vector<Race> races;


	public void run()
	{
		try
		{
			String strIn;
			
			strIn = in.readLine();
			if(strIn == null)
				return;
			//Which race does this putz want in on ?
			if(strIn.startsWith("<raceid"))
			{
				int start = strIn.indexOf("<raceid='");
				start += 9;
				int end = strIn.indexOf("'", start);
				String raceID = strIn.substring(start, end);
				
				for(int i=0; i<races.size(); i++)
				{
                                    //Race race = races.elementAt(i);
                                    //if(race.equals(raceID))
                                    //	{
                                            // race.addRacer(Rider.createRider());
                                    //	}
				}
				    
		     }
		}
	    catch(IOException ioe) 
		{
			    ioe.printStackTrace();
		} 
	}
	
	public RaceDispatcher(Socket _clientSock)
	{
		races = new Vector<Race>();
		//Fake two races to start.
		//Race race1 = new Race();
		//races.add(race1);
		
		//Race race2 = new Race();
		//races.add(race2);
		
		
		try {
			clientSock = _clientSock;
			in = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
			out = new PrintWriter(new OutputStreamWriter(clientSock.getOutputStream()));
			
		} catch (IOException e) {
			logger.error(e);
		}

	}
	
}
