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

// imports from .jar's in lib/
import org.apache.log4j.Logger;

/*
 * A WebPoller is a thread that periodically wakes up and slurps down
 * a page from GoldenWeb that describes the set of active races.
 */
public class WebPoller extends Thread {
    private static Logger logger =
        Logger.getLogger(WebPoller.class.getName());
    private String url_to_poll;
    private static final int MAX_FAILURES = 360; // 1 hr's worth

    public WebPoller(String gs_url) {
        url_to_poll = gs_url;
        setName("WebPoller");  // sets the thread's name
    }

    /*
     * poll_now() wakes up the polling thread and forces a slurp
     * of the GoldenWeb page.
     */
    public void poll_now() {
        synchronized(url_to_poll) {
            url_to_poll.notifyAll();
        }
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

            // wait for 10 seconds, or woken by poll_now().
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
     * slurp down the web page and update the list of known races
     */
    private boolean slurp() {
        logger.debug("slurping " + url_to_poll);
        
        try {
            URL fetchURL = new URL(url_to_poll);
            BufferedReader in =
                new BufferedReader(new InputStreamReader(fetchURL.openStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                // handle input here
                System.out.println(inputLine);
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
