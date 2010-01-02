/* 
 * Copyright (c) 2010 Steve Gribble    [ gribble {at} cs.washington.edu ]
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

// standard java imports
import java.net.*;
import java.io.*;
import java.util.regex.*;
import java.lang.*;
import java.util.*;

// imports from .jar's in lib/
import org.apache.log4j.Logger;

/*
 * A WebPoller is a thread that periodically wakes up and slurps down
 * a page from GoldenWeb that describes the set of active races.
 */
public class WebPoller extends Thread {
    private static final int MAX_FAILURES = 360; // 1 hr's worth
    private static Logger    logger =
        Logger.getLogger(WebPoller.class.getName());

    private String                   url_to_poll;
    private Hashtable<String,Race>   currentRaces;

    public WebPoller(String gs_url) {
        url_to_poll = gs_url;
        currentRaces = new Hashtable<String,Race>();
        setName("WebPoller");  // sets the thread's name
    }

    /*
     * looks to see if the WebPoller knows about a race with a given
     * raceid, and if so, returns a reference to the Race structure.
     * If not, forces a poll (which may cause the caller to block for
     * a few seconds), and tries again.  If it still can't find the
     * race, returns null.
     *
     * note that the race structure is refreshed on every poll, so
     * findNewRace should be used just to find a Race for the first
     * time, rather than to keep track of ongoing, active Races with
     * participants.
     */
    public Race findNewRace(String raceid) {
        Race foundRace = null;

        foundRace = (Race) currentRaces.get(raceid);
        if (foundRace == null) {
            poll_now();
            foundRace = (Race) currentRaces.get(raceid);
        }
        return foundRace;
    }

    /*
     * the run() method of the Thread; here's where the WebPoller
     * loops and polls the web page ad infinitum.
     */
    public void run() {
        int num_failures = 0;
        while(true) {
            // try the slurp; log more than MAX_FAILURES consecutive
            // failures to slurp.
            if (slurp()) {
                num_failures = 0;
            } else {
                num_failures++;
                if (num_failures >= MAX_FAILURES) {
                    logger.fatal("" + MAX_FAILURES + " consecutive " +
                                 "failures when polling GoldenWeb url (" +
                                 url_to_poll + "); consider aborting.");
                    num_failures = 0;
                }
            }
            synchronized(currentRaces) {
                currentRaces.notifyAll();
            }

            // wait for 10 seconds, or until woken by poll_now().
            try {
                synchronized(url_to_poll) {
                    url_to_poll.wait(10000);
                }
            } catch (java.lang.InterruptedException ie) {
                // ignore
            }
        }
    }

    /*
     * poll_now() wakes up the polling thread, forces a slurp of the
     * GoldenWeb page, and waits for up to 5 seconds for the slurp to
     * finish.
     */
    private void poll_now() {
        synchronized(currentRaces) {
            synchronized(url_to_poll) {
                url_to_poll.notifyAll();
            }
            try {
                currentRaces.wait(5000);
            } catch (java.lang.InterruptedException ie) {
            }
        }
    }

    /*
     * slurp down the web page and update the list of known races
     * (i.e., repopulate the currentRaces hashtable).
     */
    private boolean slurp() {
        logger.debug("slurping " + url_to_poll);
        
        try {
            URL fetchURL = new URL(url_to_poll);
            BufferedReader in =
                new BufferedReader(new InputStreamReader(fetchURL.openStream()));
            String inputLine;

            // compile regexp that looks for lines of form:
            //       raceid='<raceid>' racedistance='<km>' maxriders='<maxriders>'
            // e.g., raceid='18d1a1bcd104ee116a772310bbc61211' racedistance='40.0' maxriders='10' 
            Pattern regexp =
                Pattern.compile("raceid='([0-9a-fA-F]+)'\\s+racedistance='([0-9.]+)'\\s+maxriders='([0-9.]+)'");

            // get read to allocate and jam races we find into currentRaces
            currentRaces.clear();
            while ((inputLine = in.readLine()) != null) {
                // parse input here
                Matcher matcher = regexp.matcher(inputLine);
                boolean matchfound = matcher.find();
                if (!matchfound) {
                    logger.warn("GoldenWeb contained badly formatted line.  URL: " +
                                url_to_poll + "   line: " + inputLine);
                    continue;
                }
                String raceid = "";
                float  racedistance_km = (float) 0.0;
                int    maxriders = 0;
                try {
                    raceid = matcher.group(1).toLowerCase();
                    racedistance_km = Float.parseFloat(matcher.group(2));
                    maxriders = Integer.parseInt(matcher.group(3));
                } catch (NumberFormatException e) {
                    logger.warn("GoldenWeb contained badly formatted line.  URL: " +
                                url_to_poll + "   line: " + inputLine);
                    continue;
                }

                // add the new Race structure to our Race hashtable
                Race newrace = new Race(raceid, racedistance_km, maxriders);
                if (currentRaces.containsKey(raceid)) {
                    logger.warn("GoldenWeb contained multiple lines with the " +
                                "same raceid (" + raceid + ").  using last.");
                }
                currentRaces.put(raceid, newrace);
            }
            in.close();
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
            System.exit(-1);
        } catch (IOException ioe) {
            return false;
        }
        return true;
    }
}
