/* 
 * Copyright (c) 2009 Justin Knotzke (jknotzke@shampoo.ca)
 *
 * Contributions from:
 *    Steve Gribble  [ gribble {at} cs.washington.edu ]
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

import java.util.UUID;

import org.apache.log4j.Logger;

/*
 * A Race tracks the metadata of a race, including the race
 * parameters established at creation (raceid, racedistance_km,
 * maxriders) and dynamic information about who is connected and
 * their current positions.
 */
public class Race {
    static Logger logger = Logger.getLogger(Race.class.getName());

    private String raceid;
    private float  racedistance_km;
    private int    maxriders;

    public Race(String raceid, float racedistance_km, int maxriders) {
        this.raceid = raceid;
        this.racedistance_km = racedistance_km;
        this.maxriders = maxriders;
    }

    public String getRaceid() {
        return raceid;
    }

    public float getRacedistanceKm() {
        return racedistance_km;
    }

    public int getMaxriders() {
        return maxriders;
    }
}

