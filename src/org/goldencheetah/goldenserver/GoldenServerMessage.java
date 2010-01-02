package org.goldencheetah.goldenserver;

import java.util.Vector;

import org.apache.log4j.Logger;

public class GoldenServerMessage
{
	static Logger logger = Logger.getLogger(GoldenServerMessage.class.getName());

	public static void newRacer(Vector<Race> handlers)
	{
		for(int i=0; i < handlers.size(); i++)
		{
			Race race = handlers.elementAt(i);
			// race.get
		}
		
	}
	
	public static void droppedRacer()
	{
		
	}
	
	private String getClientList(Vector<Race> handlers)
	{
		StringBuffer clientList = new StringBuffer();
		for(int x=0; x< handlers.size(); x++)
		{
                    // clientList.append(handlers.get(x).getRace().get
		    if(x < (handlers.size() -1))
		    	clientList.append(",");
		}
		
		return clientList.toString();
	}

}
