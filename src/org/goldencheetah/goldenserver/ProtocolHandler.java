/* 
 * Copyright (c) 2010 Steve Gribble  [gribble {at} cs.washington.edu]
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

import java.lang.*;
import java.util.regex.*;

/*
 * This class handles the dirty work of parsing protocol messages
 * received by GS or GC.  The class has a single static method,
 * parseLine(), which parses a protocol line and returns a fully
 * populated ProtocolMessage subclass of the right type, or null
 * if parsing failed.  The ProtocolMessage subclasses each have
 * a toString() method which produces a text string appropriate
 * for squirting into a socket.
 *
 * See the main() method for test cases and examples for how
 * to use this.
 */
public class ProtocolHandler {
    /*
     * This abstract class represents a GS/GC protocol message.  There
     * is one subclass defined below for each message type.
     */
    public static abstract class ProtocolMessage {
        public abstract String toString();
    }

    /*
     * Given a string, parses that string and returns an appropriate
     * ProtocolMessage subclass with the field values parsed out.
     * Returns "null" if the line could not be parsed correctly.
     */
    public static ProtocolMessage parseLine(String line) {
        // sanity check argument
        if (line == null)
            return null;

        // depending on what the first word in the message is, invoke
        // the rest of the parsing in the appropriate ProtocolMessage
        // subclass's constructor.
        try {
            if (line.startsWith("hello ")) {
                return new HelloMessage(line);
            } else if (line.startsWith("hellofail ")) {
                return new HelloFailMessage(line);
            } else if (line.startsWith("hellosucceed ")) {
                return new HelloSucceedMessage(line);
            } else if (line.startsWith("clientlist ")) {
                return new ClientListMessage(line);
            } else if (line.startsWith("client ")) {
                return new ClientMessage(line);
            } else if (line.startsWith("telemetry ")) {
                return new TelemetryMessage(line);
            } else if (line.startsWith("standings ")) {
                return new StandingsMessage(line);
            } else if (line.startsWith("racer ")) {
                return new RacerMessage(line);
            } else if (line.startsWith("raceconcluded ")) {
                return new RaceConcludedMessage(line);
            } else if (line.startsWith("result ")) {
                return new ResultMessage(line);
            } else if (line.startsWith("goodbye ")) {
                return new GoodbyeMessage(line);
            }
        } catch (java.text.ParseException e) {
            // XXX - log error here.
        }

        // Nothing matches, or ParseException thrown.  Is bogus line,
        // so return null.
        return null;
    }

    /*
     * A HelloMessage is sent from a client to the server
     * upon connection.
     */
    public static class HelloMessage extends ProtocolMessage {
        // the regexp we use to parse out the message; precompile for speed.
        private static Pattern regexp =
            // e.g., hello 0.1 raceid='18d1a1bcd104ee116a772310bbc61211' ridername='Steve G' ftp='213' weight='75.8'
            Pattern.compile("hello\\s+(\\d+\\.\\d+)\\s+raceid='([0-9a-fA-F]+)'\\s+ridername='([a-zA-Z ]+)'\\s+ftp='([0-9]+)'\\s+weight='([0-9.]+)'");

        public HelloMessage(String line) throws java.text.ParseException {
            // parse the line here, using regexp
            Matcher matcher = regexp.matcher(line);
            boolean matchfound = matcher.find();

            if (!matchfound) {
                throw new java.text.ParseException("hello", 0);
            }
            this.protoversion = matcher.group(1);
            this.raceid = matcher.group(2).toLowerCase();
            this.ridername = matcher.group(3);
            try {
                this.ftp_watts = Integer.parseInt(matcher.group(4));
            } catch (NumberFormatException e) {
                throw new java.text.ParseException("hello", 1);
            }
            try {
                this.weight_kg = Float.parseFloat(matcher.group(5));
            } catch (NumberFormatException e) {
                throw new java.text.ParseException("hello", 2);
            }
        }

        public HelloMessage(String protoversion, String raceid, String ridername,
                            int ftp_watts, float weight_kg) {
            this.protoversion = protoversion;
            this.raceid = raceid.toLowerCase();
            this.ridername = ridername;
            this.ftp_watts = ftp_watts;
            this.weight_kg = weight_kg;
        }

        public String toString() {
            return String.format("hello %s raceid='%s' ridername='%s' ftp='%d' weight='%.2f'\n",
                                 this.protoversion, this.raceid, this.ridername,
                                 this.ftp_watts, this.weight_kg);
        }

        // public fields from parsed message
        public String protoversion;
        public String raceid;
        public String ridername;
        public int ftp_watts;
        public float weight_kg;
    }

    /* 
     * A HelloFail is sent by the server to the client when
     * things go wrong on handshake.
     */
    public static class HelloFailMessage extends ProtocolMessage {
        // the regexp we use to parse out the message; precompile for speed.
        private static Pattern regexp =
            // e.g., hellofail 0.1 nosuchrace raceid='18d1a1bcd104ee116a772310bbc61211'
            Pattern.compile("hellofail\\s+(\\d+\\.\\d+)\\s+(\\S+)\\s+raceid='([0-9a-fA-F]+)'");

        public HelloFailMessage(String line) throws java.text.ParseException {
            // parse the line here, using regexp
            Matcher matcher = regexp.matcher(line);
            boolean matchfound = matcher.find();

            if (!matchfound) {
                throw new java.text.ParseException("hellofail", 0);
            }
            this.protoversion = matcher.group(1);
            this.errmessage = matcher.group(2).toLowerCase();
            this.raceid = matcher.group(3).toLowerCase();
        }

        public HelloFailMessage(String protoversion, String errmessage, String raceid) {
            this.protoversion = protoversion;
            this.errmessage = errmessage.toLowerCase();
            this.raceid = raceid.toLowerCase();
        }

        public String toString() {
            return String.format("hellofail %s %s raceid='%s'\n",
                                 this.protoversion, this.errmessage, this.raceid);
        }

        // public fields from parsed message
        public String protoversion;
        public String errmessage;
        public String raceid;
    }

    /* 
     * A HelloSucceed is sent by the server to the client when
     * things go well on handshake.
     */
    public static class HelloSucceedMessage extends ProtocolMessage {
        // the regexp we use to parse out the message; precompile for speed.
        private static Pattern regexp =
            // e.g., hellosucceed 0.1 raceid='18d1a1bcd104ee116a772310bbc61211' riderid='123212321232123a' racedistance='180.0'
            Pattern.compile("hellosucceed\\s+(\\d+\\.\\d+)\\s+raceid='([0-9a-fA-F]+)'\\s+riderid='([0-9a-fA-F]+)'\\s+racedistance='([0-9.]+)'");

        public HelloSucceedMessage(String line) throws java.text.ParseException {
            // parse the line here, using regexp
            Matcher matcher = regexp.matcher(line);
            boolean matchfound = matcher.find();

            if (!matchfound) {
                throw new java.text.ParseException("hellosucceed", 0);
            }
            this.protoversion = matcher.group(1);
            this.raceid = matcher.group(2).toLowerCase();
            this.riderid = matcher.group(3).toLowerCase();
            try {
                this.racedistance_km = Float.parseFloat(matcher.group(4));
            } catch (NumberFormatException e) {
                throw new java.text.ParseException("hellosucceed", 1);
            }
        }

        public HelloSucceedMessage(String protoversion, String raceid, String riderid,
                                   float racedistance_km) {
            this.protoversion = protoversion;
            this.raceid = raceid.toLowerCase();
            this.riderid = riderid.toLowerCase();
            this.racedistance_km = racedistance_km;
        }

        public String toString() {
            return String.format("hellosucceed %s raceid='%s' riderid='%s' racedistance='%.2f'\n",
                                 this.protoversion, this.raceid, this.riderid, this.racedistance_km);
        }

        // public fields from parsed message
        public String protoversion;
        public String raceid;
        public String riderid;
        public float racedistance_km;
    }

    /* 
     * A ClientList is sent by the server to the client to inform it
     * of membership updates.
     */
    public static class ClientListMessage extends ProtocolMessage {
        // the regexp we use to parse out the message; precompile for speed.
        private static Pattern regexp =
            // e.g., clientlist raceid='18d1a1bcd104ee116a772310bbc61211' numclients='5'
            Pattern.compile("clientlist\\s+raceid='([0-9a-fA-F]+)'\\s+numclients='([0-9]+)'");

        public ClientListMessage(String line) throws java.text.ParseException {
            // parse the line here, using regexp
            Matcher matcher = regexp.matcher(line);
            boolean matchfound = matcher.find();

            if (!matchfound) {
                throw new java.text.ParseException("clientlist", 0);
            }
            this.raceid = matcher.group(1).toLowerCase();
            try {
                this.numclients = Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException e) {
                throw new java.text.ParseException("clientlist", 2);
            }
        }

        public ClientListMessage(String raceid, int numclients) {
            this.raceid = raceid.toLowerCase();
            this.numclients = numclients;
        }

        public String toString() {
            return String.format("clientlist raceid='%s' numclients='%d'\n",
                                 this.raceid, this.numclients);
        }

        // public fields from parsed message
        public String raceid;
        public int numclients;
    }

    /* 
     * A Client is sent by the server to the client as part of
     * of a ClientList membership update.
     */
    public static class ClientMessage extends ProtocolMessage {
        // the regexp we use to parse out the message; precompile for speed.
        private static Pattern regexp =
            // e.g., client ridername='Steve G' riderid='123212321232123a' ftp='213' weight='75.8'
            Pattern.compile("client\\s+ridername='([a-zA-Z ]+)'\\s+riderid='([0-9a-fA-F]+)'\\s+ftp='([0-9]+)'\\s+weight='([0-9.]+)'");

        public ClientMessage(String line) throws java.text.ParseException {
            // parse the line here, using regexp
            Matcher matcher = regexp.matcher(line);
            boolean matchfound = matcher.find();

            if (!matchfound) {
                throw new java.text.ParseException("client", 0);
            }
            this.ridername = matcher.group(1);
            this.riderid = matcher.group(2).toLowerCase();
            try {
                this.ftp_watts = Integer.parseInt(matcher.group(3));
            } catch (NumberFormatException e) {
                throw new java.text.ParseException("client", 1);
            }
            try {
                this.weight_kg = Float.parseFloat(matcher.group(4));
            } catch (NumberFormatException e) {
                throw new java.text.ParseException("client", 2);
            }
        }

        public ClientMessage(String ridername, String riderid, int ftp_watts, float weight_kg) {
            this.ridername = ridername;
            this.riderid = riderid;
            this.ftp_watts = ftp_watts;
            this.weight_kg = weight_kg;
        }

        public String toString() {
            return String.format("client ridername='%s' riderid='%s' ftp='%d' weight='%.2f'\n",
                                 this.ridername, this.riderid, this.ftp_watts, this.weight_kg);
        }

        // public fields from parsed message
        public String ridername;
        public String riderid;
        public int ftp_watts;
        public float weight_kg;
    }

    /* 
     * A Telemetry is sent by the client to the server to give it
     * a telemetry update.
     */
    public static class TelemetryMessage extends ProtocolMessage {
        // the regexp we use to parse out the message; precompile for speed.
        private static Pattern regexp =
            // telemetry raceid='18d1a1bcd104ee116a772310bbc61211' riderid='123212321232123a' power='250' cadence='85' distance='5.41' heartrate='155' speed='31.5'
            Pattern.compile("telemetry\\s+raceid='([0-9a-fA-F]+)'\\s+riderid='([0-9a-fA-F]+)'\\s+power='([0-9]+)'\\s+cadence='([0-9]+)'\\s+distance='([0-9.]+)'\\s+heartrate='([0-9]+)'\\s+speed='([0-9.]+)'");

        public TelemetryMessage(String line) throws java.text.ParseException {
            // parse the line here, using regexp
            Matcher matcher = regexp.matcher(line);
            boolean matchfound = matcher.find();

            if (!matchfound) {
                throw new java.text.ParseException("telemetry", 0);
            }
            this.raceid=matcher.group(1);
            this.riderid = matcher.group(2).toLowerCase();
            try {
                this.power_watts = Integer.parseInt(matcher.group(3));
            } catch (NumberFormatException e) {
                throw new java.text.ParseException("telemetry", 1);
            }
            try {
                this.cadence_rpm = Integer.parseInt(matcher.group(4));
            } catch (NumberFormatException e) {
                throw new java.text.ParseException("telemetry", 2);
            }
            try {
                this.distance_km = Float.parseFloat(matcher.group(5));
            } catch (NumberFormatException e) {
                throw new java.text.ParseException("telemetry", 3);
            }
            try {
                this.heartrate_bpm = Integer.parseInt(matcher.group(6));
            } catch (NumberFormatException e) {
                throw new java.text.ParseException("telemetry", 4);
            }
            try {
                this.speed_kph = Float.parseFloat(matcher.group(7));
            } catch (NumberFormatException e) {
                throw new java.text.ParseException("telemetry", 5);
            }
        }

        public TelemetryMessage(String raceid, String riderid, int power_watts, int cadence_rpm,
                                float distance_km, int heartrate_bpm, float speed_kph) {
            this.raceid = raceid;
            this.riderid = riderid;
            this.power_watts = power_watts;
            this.cadence_rpm = cadence_rpm;
            this.distance_km = distance_km;
            this.heartrate_bpm = heartrate_bpm;
            this.speed_kph = speed_kph;
        }

        public String toString() {
            return String.format("telemetry raceid='%s' riderid='%s' power='%d' cadence='%d' distance='%.2f' heartrate='%d' speed='%.2f'\n",
                                 this.raceid, this.riderid, this.power_watts, this.cadence_rpm,
                                 this.distance_km, this.heartrate_bpm, this.speed_kph);
        }

        // public fields from parsed message
        public String raceid;
        public String riderid;
        public int power_watts;
        public int cadence_rpm;
        public float distance_km;
        public int heartrate_bpm;
        public float speed_kph;
    }

    /* 
     * A Standings is sent by the server to the client to inform it
     * of the current standings.
     */
    public static class StandingsMessage extends ProtocolMessage {
        // the regexp we use to parse out the message; precompile for speed.
        private static Pattern regexp =
            // e.g., standings raceid='18d1a1bcd104ee116a772310bbc61211' numclients='5'
            Pattern.compile("standings\\s+raceid='([0-9a-fA-F]+)'\\s+numclients='([0-9]+)'");

        public StandingsMessage(String line) throws java.text.ParseException {
            // parse the line here, using regexp
            Matcher matcher = regexp.matcher(line);
            boolean matchfound = matcher.find();

            if (!matchfound) {
                throw new java.text.ParseException("standings", 0);
            }
            this.raceid = matcher.group(1).toLowerCase();
            try {
                this.numclients = Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException e) {
                throw new java.text.ParseException("standings", 2);
            }
        }

        public StandingsMessage(String raceid, int numclients) {
            this.raceid = raceid.toLowerCase();
            this.numclients = numclients;
        }

        public String toString() {
            return String.format("standings raceid='%s' numclients='%d'\n",
                                 this.raceid, this.numclients);
        }

        // public fields from parsed message
        public String raceid;
        public int numclients;
    }

    /* 
     * A Racer message is sent by server to the client as part of
     * a standings update.
     */
    public static class RacerMessage extends ProtocolMessage {
        // the regexp we use to parse out the message; precompile for speed.
        private static Pattern regexp =
            // racer riderid='123212321232123a' power='250' cadence='85' distance='5.41' heartrate='155' speed='31.5' place='1'
            Pattern.compile("racer\\s+riderid='([0-9a-fA-F]+)'\\s+power='([0-9]+)'\\s+cadence='([0-9]+)'\\s+distance='([0-9.]+)'\\s+heartrate='([0-9]+)'\\s+speed='([0-9.]+)'\\s+place='([0-9]+)'");

        public RacerMessage(String line) throws java.text.ParseException {
            // parse the line here, using regexp
            Matcher matcher = regexp.matcher(line);
            boolean matchfound = matcher.find();

            if (!matchfound) {
                throw new java.text.ParseException("racer", 0);
            }
            this.riderid = matcher.group(1).toLowerCase();
            try {
                this.power_watts = Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException e) {
                throw new java.text.ParseException("racer", 1);
            }
            try {
                this.cadence_rpm = Integer.parseInt(matcher.group(3));
            } catch (NumberFormatException e) {
                throw new java.text.ParseException("racer", 2);
            }
            try {
                this.distance_km = Float.parseFloat(matcher.group(4));
            } catch (NumberFormatException e) {
                throw new java.text.ParseException("racer", 3);
            }
            try {
                this.heartrate_bpm = Integer.parseInt(matcher.group(5));
            } catch (NumberFormatException e) {
                throw new java.text.ParseException("racer", 4);
            }
            try {
                this.speed_kph = Float.parseFloat(matcher.group(6));
            } catch (NumberFormatException e) {
                throw new java.text.ParseException("racer", 5);
            }
            try {
                this.place = Integer.parseInt(matcher.group(7));
            } catch (NumberFormatException e) {
                throw new java.text.ParseException("racer", 6);
            }
        }

        public RacerMessage(String riderid, int power_watts, int cadence_rpm,
                            float distance_km, int heartrate_bpm, float speed_kph,
                            int place) {
            this.riderid = riderid;
            this.power_watts = power_watts;
            this.cadence_rpm = cadence_rpm;
            this.distance_km = distance_km;
            this.heartrate_bpm = heartrate_bpm;
            this.speed_kph = speed_kph;
            this.place = place;
        }

        public String toString() {
            return String.format("racer riderid='%s' power='%d' cadence='%d' distance='%.2f' heartrate='%d' speed='%.2f' place='%d'\n",
                                 this.riderid, this.power_watts, this.cadence_rpm,
                                 this.distance_km, this.heartrate_bpm, this.speed_kph,
                                 this.place);
        }

        // public fields from parsed message
        public String riderid;
        public int power_watts;
        public int cadence_rpm;
        public float distance_km;
        public int heartrate_bpm;
        public float speed_kph;
        public int place;
    }

    /* 
     * A RaceConcluded is sent by the server to the client to inform it
     * that the race has finished and the final standings.
     */
    public static class RaceConcludedMessage extends ProtocolMessage {
        // the regexp we use to parse out the message; precompile for speed.
        private static Pattern regexp =
            // e.g., raceconcluded raceid='18d1a1bcd104ee116a772310bbc61211' numclients='5'
            Pattern.compile("raceconcluded\\s+raceid='([0-9a-fA-F]+)'\\s+numclients='([0-9]+)'");

        public RaceConcludedMessage(String line) throws java.text.ParseException {
            // parse the line here, using regexp
            Matcher matcher = regexp.matcher(line);
            boolean matchfound = matcher.find();

            if (!matchfound) {
                throw new java.text.ParseException("raceconcluded", 0);
            }
            this.raceid = matcher.group(1).toLowerCase();
            try {
                this.numclients = Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException e) {
                throw new java.text.ParseException("raceconcluded", 2);
            }
        }

        public RaceConcludedMessage(String raceid, int numclients) {
            this.raceid = raceid.toLowerCase();
            this.numclients = numclients;
        }

        public String toString() {
            return String.format("raceconcluded raceid='%s' numclients='%d'\n",
                                 this.raceid, this.numclients);
        }

        // public fields from parsed message
        public String raceid;
        public int numclients;
    }

    /* 
     * A ResultMessage message is sent by server to the client as part of
     * a raceconcluded update.
     */
    public static class ResultMessage extends ProtocolMessage {
        // the regexp we use to parse out the message; precompile for speed.
        private static Pattern regexp =
            // result riderid='123212321232123a' distance='5.41' place='1'
            Pattern.compile("result\\s+riderid='([0-9a-fA-F]+)'\\s+distance='([0-9.]+)'\\s+place='([0-9]+)'");

        public ResultMessage(String line) throws java.text.ParseException {
            // parse the line here, using regexp
            Matcher matcher = regexp.matcher(line);
            boolean matchfound = matcher.find();

            if (!matchfound) {
                throw new java.text.ParseException("result", 0);
            }
            this.riderid = matcher.group(1).toLowerCase();
            try {
                this.distance_km = Float.parseFloat(matcher.group(2));
            } catch (NumberFormatException e) {
                throw new java.text.ParseException("result", 1);
            }
            try {
                this.place = Integer.parseInt(matcher.group(3));
            } catch (NumberFormatException e) {
                throw new java.text.ParseException("result", 2);
            }
        }

        public ResultMessage(String riderid, float distance_km, int place) {
            this.riderid = riderid;
            this.distance_km = distance_km;
            this.place = place;
        }

        public String toString() {
            return String.format("result riderid='%s' distance='%.2f' place='%d'\n",
                                 this.riderid, this.distance_km, this.place);
        }

        // public fields from parsed message
        public String riderid;
        public float distance_km;
        public int place;
    }

    /* 
     * A Goodbye is sent by the client to the server to sign off cleanly.
     */
    public static class GoodbyeMessage extends ProtocolMessage {
        // the regexp we use to parse out the message; precompile for speed.
        private static Pattern regexp =
            // e.g., goodbye raceid='18d1a1bcd104ee116a772310bbc61211' riderid='123212321232123a'
            Pattern.compile("goodbye\\s+raceid='([0-9a-fA-F]+)'\\s+riderid='([0-9a-fA-F]+)'");

        public GoodbyeMessage(String line) throws java.text.ParseException {
            // parse the line here, using regexp
            Matcher matcher = regexp.matcher(line);
            boolean matchfound = matcher.find();

            if (!matchfound) {
                throw new java.text.ParseException("goodbye", 0);
            }
            this.raceid = matcher.group(1).toLowerCase();
            this.riderid = matcher.group(2).toLowerCase();
        }

        public GoodbyeMessage(String raceid, String riderid) {
            this.raceid = raceid.toLowerCase();
            this.riderid = riderid.toLowerCase();
        }

        public String toString() {
            return String.format("goodbye raceid='%s' riderid='%s'\n",
                                 this.raceid, this.riderid);
        }

        // public fields from parsed message
        public String raceid;
        public String riderid;
    }

    /*
     * Contains unit test code and example usage code for protocol
     * message parsing.
     */
    public static void main(String[] args) {
        // test HelloMessage 
        ProtocolHandler.ProtocolMessage pm = ProtocolHandler.parseLine(
          "hello 0.1 raceid='18d1a1bcd104ee116a772310bbc61211' ridername='Steve G' ftp='213' weight='74.8'\n"
                                                       );
        if ((pm == null) || !(pm instanceof ProtocolHandler.HelloMessage)) {
            System.out.println("Parsing of hello failed!?!\n");
            System.exit(0);
        }
        ProtocolHandler.HelloMessage hm = (ProtocolHandler.HelloMessage) pm;
        String os = hm.toString();
        System.out.print(os);

        // test HelloFailMessage
        pm = ProtocolHandler.parseLine(
           "hellofail 0.1 nosuchrace raceid='18d1a1bcd104ee116a772310bbc61211'\n"
                                       );
        if ((pm == null) || !(pm instanceof ProtocolHandler.HelloFailMessage)) {
            System.out.println("Parsing of hellofail failed!?!\n");
            System.exit(0);
        }
        ProtocolHandler.HelloFailMessage hfm = (ProtocolHandler.HelloFailMessage) pm;
        os = hfm.toString();
        System.out.print(os);

        // test HelloSucceedMessage
        pm = ProtocolHandler.parseLine(
           "hellosucceed 0.1 raceid='18d1a1bcd104ee116a772310bbc61211' riderid='123212321232123a' racedistance='180.0'\n"
                                       );
        if ((pm == null) || !(pm instanceof ProtocolHandler.HelloSucceedMessage)) {
            System.out.println("Parsing of hellosucceed failed!?!\n");
            System.exit(0);
        }
        ProtocolHandler.HelloSucceedMessage hsm = (ProtocolHandler.HelloSucceedMessage) pm;
        os = hsm.toString();
        System.out.print(os);

        // test ClientListMessage
        pm = ProtocolHandler.parseLine(
           "clientlist raceid='18d1a1bcd104ee116a772310bbc61211' numclients='5'\n"
                                       );
        if ((pm == null) || !(pm instanceof ProtocolHandler.ClientListMessage)) {
            System.out.println("Parsing of clientlist failed!?!\n");
            System.exit(0);
        }
        ProtocolHandler.ClientListMessage clm = (ProtocolHandler.ClientListMessage) pm;
        os = clm.toString();
        System.out.print(os);

        // test ClientMessage
        pm = ProtocolHandler.parseLine(
           "client ridername='Steve G' riderid='123212321232123a' ftp='213' weight='75.8'"
                                       );
        if ((pm == null) || !(pm instanceof ProtocolHandler.ClientMessage)) {
            System.out.println("Parsing of client failed!?!\n");
            System.exit(0);
        }
        ProtocolHandler.ClientMessage cm = (ProtocolHandler.ClientMessage) pm;
        os = cm.toString();
        System.out.print(os);

        // test TelemetryMessage
        pm = ProtocolHandler.parseLine(
           "telemetry raceid='18d1a1bcd104ee116a772310bbc61211' riderid='123212321232123a' power='250' cadence='85' distance='5.41' heartrate='155' speed='31.5'\n"
                                       );
        if ((pm == null) || !(pm instanceof ProtocolHandler.TelemetryMessage)) {
            System.out.println("Parsing of telemetry failed!?!\n");
            System.exit(0);
        }
        ProtocolHandler.TelemetryMessage tm = (ProtocolHandler.TelemetryMessage) pm;
        os = tm.toString();
        System.out.print(os);

        // test StandingsMessage
        pm = ProtocolHandler.parseLine(
           "standings raceid='18d1a1bcd104ee116a772310bbc61211' numclients='5'\n"
                                       );
        if ((pm == null) || !(pm instanceof ProtocolHandler.StandingsMessage)) {
            System.out.println("Parsing of standings failed!?!\n");
            System.exit(0);
        }
        ProtocolHandler.StandingsMessage sm = (ProtocolHandler.StandingsMessage) pm;
        os = sm.toString();
        System.out.print(os);

        // test RacerMessage
        pm = ProtocolHandler.parseLine(
           "racer riderid='123212321232123a' power='250' cadence='85' distance='5.41' heartrate='155' speed='31.5' place='1'\n"
                                       );
        if ((pm == null) || !(pm instanceof ProtocolHandler.RacerMessage)) {
            System.out.println("Parsing of racer failed!?!\n");
            System.exit(0);
        }
        ProtocolHandler.RacerMessage rm = (ProtocolHandler.RacerMessage) pm;
        os = rm.toString();
        System.out.print(os);

        // test RaceConcludedMessage
        pm = ProtocolHandler.parseLine(
           "raceconcluded raceid='18d1a1bcd104ee116a772310bbc61211' numclients='5'\n"
                                       );
        if ((pm == null) || !(pm instanceof ProtocolHandler.RaceConcludedMessage)) {
            System.out.println("Parsing of raceconcluded failed!?!\n");
            System.exit(0);
        }
        ProtocolHandler.RaceConcludedMessage rcm = (ProtocolHandler.RaceConcludedMessage) pm;
        os = rcm.toString();
        System.out.print(os);

        // test ResultMessage
        pm = ProtocolHandler.parseLine(
           "result riderid='123212321232123a' distance='5.41' place='1'\n"
                                       );
        if ((pm == null) || !(pm instanceof ProtocolHandler.ResultMessage)) {
            System.out.println("Parsing of result failed!?!\n");
            System.exit(0);
        }
        ProtocolHandler.ResultMessage resm = (ProtocolHandler.ResultMessage) pm;
        os = resm.toString();
        System.out.print(os);

        // test Goodbye
        pm = ProtocolHandler.parseLine(
           "goodbye raceid='18d1a1bcd104ee116a772310bbc61211' riderid='123212321232123a'\n"
                                       );
        if ((pm == null) || !(pm instanceof ProtocolHandler.GoodbyeMessage)) {
            System.out.println("Parsing of goodbye failed!?!\n");
            System.exit(0);
        }
        ProtocolHandler.GoodbyeMessage gbm = (ProtocolHandler.GoodbyeMessage) pm;
        os = gbm.toString();
        System.out.print(os);
    }
}
