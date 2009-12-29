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
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import org.apache.log4j.Logger;

public class Race extends Thread 
{
	static Logger logger = Logger.getLogger(Race.class.getName());
	static Vector<Race> handlers = new Vector<Race>();
	private List<Rider> riders;
	private List<Integer> placings; 
	private String raceID;
	private String raceName;
	private Date duration;
	private BufferedReader in;
	private PrintWriter out;
	private Socket clientSock;
	  	

	public Race(Socket _sock)
	{
	
		logger.debug("Firing up the Client Sock..");
		clientSock = _sock;
		try {
			in = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
			out = new PrintWriter(new OutputStreamWriter(clientSock.getOutputStream()));
			
		} catch (IOException e) {
			logger.error(e);
		}

	}
	
	
	public Race()
	{
		
	}
	
	public void addRacer(Rider _rider)
	{
		synchronized(handlers)
		{
			riders.add(_rider);
			//Notify Everyone we have a new racer in town
			GoldenServerMessage.newRacer(handlers);
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
				GoldenServerMessage.newRacer(handlers);
			}
			
			while(true)
			{
				strIn = in.readLine();
				if(strIn == null)
					return;
				for(int i = 0; i < handlers.size(); i++) 
				{	
					synchronized(handlers) 
					{
						Race handler = (Race)handlers.elementAt(i);
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
	        	logger.error(ioe);
			    
			}     
	        finally 
			    {
			    	synchronized(handlers) 
			    	{
			    		handlers.removeElement(this);
			    		//GoldenServerMessage.dropRider();
			    	}
			    }
			}
	}


	public List<Rider> getRiders() {
		return riders;
	}


	public void setRiders(List<Rider> riders) {
		this.riders = riders;
	}


	public List<Integer> getPlacings() {
		return placings;
	}


	public void setPlacings(List<Integer> placings) {
		this.placings = placings;
	}



	public String getRaceID() {
		return raceID;
	}

	public void setRaceID(String raceID) {
		this.raceID = raceID;
	}

	public String getRaceName() {
		return raceName;
	}

	public void setRaceName(String raceName) {
		this.raceName = raceName;
	}

	public Date getDuration() {
		return duration;
	}


	public void setDuration(Date duration) {
		this.duration = duration;
	}

	public boolean exists(String _raceID)
	{
		if(_raceID.equals(this.getRaceID()))
			return true;
		else
			return false;
	}
   
}

