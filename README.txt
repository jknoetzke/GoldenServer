GoldenServer -- race and train via GoldenCheetah over "the Internets"

Contributors:
   Justin Knotzke    [jknotzke {at} shampoo.ca]
   Steve Gribble     [gribble {at} cs.washington.edu]
   Mark Liversedge   [liversedge {at} gmail.com]


Welcome to GoldenServer!


LICENSE

This project is covered by the GNU General Public License, version 2.
Please see COPYING for the terms of this license.

Distributed with this project is Apache's log4j .jar file
(lib/log4j-1.2.15.jar).  log4j is covered by the Apache License
version 2.0 (lib/log4j-1.2.15.license.txt).


COMPILING AND RUNNING

This project is Java-based and is compiled/run using ant. If you
haven't installed ant yet, please do so now.

To compile the server, run:

   ant clean-build

To compile and execute the server, run:

   ant

To clean dynamically generated files, run:

   ant clean

We strongly suggest running "ant clean" before doing any
git-related stuff, so that you don't accidentally check in
any non-source files.


PROTOCOL DOCUMENTATION

You can file documentation for the GoldenServer/GoldenCheetah
protocol in doc/protocol_spec.txt.

Java marshaling/unmarshaling code is in:
    src/org/goldencheetah/goldenserver/ProtocolHandler.java

C++ marshaling/unmarshaling code is in the GoldenCheetah distribution,
in the src/ProtocolHandler.cpp and src/ProtocolHandler.h.
