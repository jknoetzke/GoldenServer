package org.goldencheetah.goldenserver;

import java.util.UUID;

public class Rider
{
	private String name;
	private String antID;
	private String dossard;
	private int ftp;
	
	private Rider()
	{
		
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getAntID() {
		return antID;
	}
	public void setAntID(String antID) {
		this.antID = antID;
	}
	public String getDossard() {
		return dossard;
	}
	public void setId(String dossard) {
		this.dossard = dossard;
	}
	public int getFtp() {
		return ftp;
	}
	public void setFtp(int ftp) {
		this.ftp = ftp;
	}
	public void setDossard(String dossard) {
		this.dossard = dossard;
	}
	
	public static Rider createRider()  
	{ 
		Rider rider = new Rider();
		rider.setDossard(UUID.randomUUID().toString());
		return rider;
	}

}
