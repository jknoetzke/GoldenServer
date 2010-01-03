/* 
 * Copyright (c) 2009 Justin Knotzke (jknotzke@shampoo.ca)
 *
 * Additional contributions from:
 *     Steve Gribble    [ gribble {at} cs.washington.edu ]
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

import java.io.*;
import java.net.*;
import java.util.*;

/*
 * This class represents a rider connected to the server and
 * participating in a race.
 */
public class Rider {
    private String          ridername;
    private String          riderid;
    private int             ftp_watts;
    private float           weight_kg;
    private BufferedReader  in;
    private PrintWriter     out;

    public static String getRandomRiderid() {
        Random generator = new Random();
        return (Long.toHexString(generator.nextLong())).toLowerCase();
    }

    public Rider(String ridername, int ftp_watts,
                 float weight_kg,
                 BufferedReader in, PrintWriter out) {
        this.ridername = ridername;
        this.riderid = getRandomRiderid();
        this.ftp_watts = ftp_watts;
        this.weight_kg = weight_kg;
        this.in = in;
        this.out = out;
    }
	
    public String getRidername() {
        return ridername;
    }
    public void setRidername(String ridername) {
        this.ridername = ridername;
    }

    public String getRiderid() {
        return riderid;
    }
    public void setRiderid(String riderid) {
        this.riderid = riderid;
    }

    public int getFtpWatts() {
        return ftp_watts;
    }
    public void setFtpWatts(int ftp_watts) {
        this.ftp_watts = ftp_watts;
    }

    public float getWeightKg() {
        return weight_kg;
    }
    public void setWeightKg(float weight_kg) {
        this.weight_kg = weight_kg;
    }

    public BufferedReader getIn() {
        return in;
    }
    public PrintWriter getOut() {
        return out;
    }

    public String toString() {
        return ridername + " (" + riderid + "): weight = " +
            weight_kg + " kg, ftp = " + ftp_watts + " watts";
    }
}
