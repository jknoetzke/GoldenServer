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

    private String                     raceid;
    private float                      racedistance_km;
    private int                        maxriders;
    private long                       last_telemetry_broadcast;
    private List<Position>             standings;
    private Hashtable<String,Position> riderid_position_index;

    public Race(String raceid, float racedistance_km, int maxriders) {
        this.raceid = raceid;
        this.racedistance_km = racedistance_km;
        this.maxriders = maxriders;
        this.last_telemetry_broadcast = System.currentTimeMillis();
        this.standings =
            Collections.synchronizedList(new LinkedList<Position>());
        this.riderid_position_index = new Hashtable<String,Position>();
    }

    // used to track each rider's position and last telemetry update
    private class Position implements Comparable<Position>{
        public float                            raceposition_km;
        public ProtocolHandler.TelemetryMessage last_telemetry_update;
        public long                             last_update_time;
        public Rider                            rider;

        Position(String raceid, Rider rider) {
            raceposition_km = (float) 0.0;
            last_telemetry_update =
                new ProtocolHandler.TelemetryMessage(raceid,
                                                     rider.getRiderid(),
                                                     0, 0, (float) 0.0,
                                                     0, (float) 0.0);
            last_update_time = System.currentTimeMillis();
            this.rider = rider;
        }

        public int compareTo(Position n) {
            // -1 to sort descending instead of ascending
            return -1 * Float.compare(raceposition_km, n.raceposition_km);
        }
    }

    // add a new rider to the race, if there is room
    public synchronized boolean addClient(Rider rider) {
        logger.debug("adding client; new size would be " +
                     (riderid_position_index.size() + 1));
        if (riderid_position_index.size() < maxriders) {
            Position newp = new Position(raceid, rider);
            riderid_position_index.put(rider.getRiderid(), newp);
            standings.add(newp);
            Collections.sort(standings);
            return true;
        }
        return false;
    }

    // process a telemetry message from a client.  update that
    // client's virtual position in the race, and if it's been more
    // than TELEMETRY_BROADCAST_PERIOD_MS since the last broadcast,
    // then broadcast new rider standings to all rider clients.
    public synchronized boolean telemetryUpdate(
           Rider rider, ProtocolHandler.TelemetryMessage tm) {
        Position rider_posn = riderid_position_index.get(rider.getRiderid());
        standings.remove(rider_posn);
        long     old_time = rider_posn.last_update_time;
        float    old_speed_kph = rider_posn.last_telemetry_update.speed_kph;
        long     new_time = System.currentTimeMillis();

        rider_posn.last_telemetry_update = tm;
        rider_posn.last_update_time = new_time;
        rider_posn.raceposition_km += (float) (
            (((float) (new_time - old_time))/1000.0) *
            (old_speed_kph / 3600.0));
        standings.add(rider_posn);
        Collections.sort(standings);

        if ((new_time - last_telemetry_broadcast) >
            TELEMETRY_BROADCAST_PERIOD_MS) {
            sendTelemetryUpdates();
            last_telemetry_broadcast = new_time;
        }
        return false;
    }

    // broadcast the current race standings to all clients.
    public synchronized void sendTelemetryUpdates() {
        // allocate space for the standings update messages
        int numriders = riderid_position_index.size();
        ProtocolHandler.ProtocolMessage[] update =
            new ProtocolHandler.ProtocolMessage[numriders + 1];

        // build up the update messages
        update[0] = new ProtocolHandler.StandingsMessage(raceid,
                                                         numriders);
        Iterator it = standings.iterator();
        int i = 1;
        while (it.hasNext()) {
            Position posn = (Position) it.next();
            Rider nextRider = posn.rider;
            ProtocolHandler.TelemetryMessage tm = posn.last_telemetry_update;

            update[i] =
                new ProtocolHandler.RacerMessage(
                                                 nextRider.getRiderid(),
                                                 tm.power_watts,
                                                 tm.cadence_rpm,
                                                 posn.raceposition_km,
                                                 tm.heartrate_bpm,
                                                 tm.speed_kph,
                                                 i);
            i++;
        }

        // send out the update messages
        it = standings.iterator();
        while (it.hasNext()) {
            Position posn = (Position) it.next();
            posn.rider.getWriter().add(update);
        }
        return;
    }

    // send out the current race membership to all connected clients.
    public synchronized void sendMembershipUpdate() {
        // allocate space for the membership update messages
        int numriders = riderid_position_index.size();
        ProtocolHandler.ProtocolMessage[] update =
            new ProtocolHandler.ProtocolMessage[numriders + 1];

        // build up the update messages
        update[0] = new ProtocolHandler.ClientListMessage(raceid,
                                                          numriders);
        Iterator it = standings.iterator();
        int i = 1;
        while (it.hasNext()) {
            Position posn = (Position) it.next();
            Rider nextRider = posn.rider;
            update[i] =
                new ProtocolHandler.ClientMessage(nextRider.getRidername(),
                                                  nextRider.getRiderid(),
                                                  nextRider.getFtpWatts(),
                                                  nextRider.getWeightKg());
            i++;
        }

        // send out the update messages
        it = standings.iterator();
        while (it.hasNext()) {
            Position posn = (Position) it.next();
            posn.rider.getWriter().add(update);
        }
        return;
    }

    // remove a client from the race.
    public synchronized void dropClient(Rider rider) {
        Position posn = riderid_position_index.get(rider.getRiderid());
        standings.remove(posn);
        riderid_position_index.remove(rider.getRiderid());
    }

    public synchronized int numClients() {
        return riderid_position_index.size();
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

