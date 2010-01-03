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

import java.util.*;
import org.apache.log4j.Logger;

/*
 * A Race tracks the metadata of a race, including the race
 * parameters established at creation (raceid, racedistance_km,
 * maxriders) and dynamic information about who is connected and
 * their current positions.
 */
public class Race {
    static Logger logger = Logger.getLogger(Race.class.getName());
    private static final int          TELEMETRY_BROADCAST_PERIOD_MS = 1000;

    private String                    raceid;
    private float                     racedistance_km;
    private int                       maxriders;
    private Hashtable<Rider,Position> riders;
    private long                      last_telemetry_broadcast;

    public Race(String raceid, float racedistance_km, int maxriders) {
        this.raceid = raceid;
        this.racedistance_km = racedistance_km;
        this.maxriders = maxriders;
        this.riders = new Hashtable<Rider,Position>();
        this.last_telemetry_broadcast = System.currentTimeMillis();
    }

    // used to track each rider's position and last telemetry update
    private class Position {
        public float                            raceposition_km;
        public ProtocolHandler.TelemetryMessage last_telemetry_update;
        public long                             last_update_time;

        Position() {
            raceposition_km = (float) 0.0;
            last_update_time = System.currentTimeMillis();
        }
    }

    public synchronized boolean addClient(Rider rider) {
        logger.debug("adding client; new size would be " +
                     (riders.size() + 1));
        if (riders.size() < maxriders) {
            riders.put(rider, new Position());
            return true;
        }
        return false;
    }

    public synchronized boolean telemetryUpdate(
           Rider rider, ProtocolHandler.TelemetryMessage tm) {
        Position old_posn = (Position) riders.get(rider);
        long     old_time = old_posn.last_update_time;
        long     new_time = System.currentTimeMillis();

        if (old_posn == null) {
            logger.error("couldn't find old telemetry in hashtable??!");
            System.exit(-1);
        }
        old_posn.last_telemetry_update = tm;
        old_posn.last_update_time = new_time;
        old_posn.raceposition_km += (float) (
            (((float) (new_time - old_time))/1000.0) *
            (tm.speed_kph / 3600.0));

        if ((new_time - last_telemetry_broadcast) >
            TELEMETRY_BROADCAST_PERIOD_MS) {
            sendTelemetryUpdates();
            last_telemetry_broadcast = new_time;
        }
        return false;
    }

    public synchronized void sendTelemetryUpdates() {
        // allocate space for the standings update messages
        ProtocolHandler.ProtocolMessage[] update =
            new ProtocolHandler.ProtocolMessage[riders.size() + 1];

        // build up the update messages
        update[0] = new ProtocolHandler.StandingsMessage(raceid,
                                                         riders.size());
        Enumeration it = riders.keys();
        int i = 1;
        while (it.hasMoreElements()) {
            Rider nextRider = (Rider) it.nextElement();
            Position posn = (Position) riders.get(nextRider);
            ProtocolHandler.TelemetryMessage tm = posn.last_telemetry_update;

            update[i] =
                new ProtocolHandler.RacerMessage(
                                                 nextRider.getRiderid(),
                                                 tm.power_watts,
                                                 tm.cadence_rpm,
                                                 posn.raceposition_km,
                                                 tm.heartrate_bpm,
                                                 tm.speed_kph,
                                                 1);  // XXX FIX POSITION
            i++;
        }

        // send out the update messages
        it = riders.keys();
        while (it.hasMoreElements()) {
            Rider nextRider = (Rider) it.nextElement();
            nextRider.getWriter().add(update);
        }
        return;
    }

    public synchronized void sendMembershipUpdate() {
        // allocate space for the membership update messages
        ProtocolHandler.ProtocolMessage[] update =
            new ProtocolHandler.ProtocolMessage[riders.size() + 1];

        // build up the update messages
        update[0] = new ProtocolHandler.ClientListMessage(raceid,
                                                          riders.size());
        Enumeration it = riders.keys();
        int i = 1;
        while (it.hasMoreElements()) {
            Rider nextRider = (Rider) it.nextElement();
            update[i] =
                new ProtocolHandler.ClientMessage(nextRider.getRidername(),
                                                  nextRider.getRiderid(),
                                                  nextRider.getFtpWatts(),
                                                  nextRider.getWeightKg());
            i++;
        }

        // send out the update messages
        it = riders.keys();
        while (it.hasMoreElements()) {
            Rider nextRider = (Rider) it.nextElement();
            nextRider.getWriter().add(update);
        }
    }

    public synchronized void dropClient(Rider rider) {
        riders.remove(rider);
    }

    public synchronized int numClients() {
        return riders.size();
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

