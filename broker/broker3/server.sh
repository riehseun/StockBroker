#!/bin/bash
# server.sh
ECE419_HOME=/cad2/ece419s/
JAVA_HOME=${ECE419_HOME}/java/jdk1.6.0/

# arguments to OnlineBroker
# $1 = hostname of BrokerLookupServer
# $2 = port where BrokerLookupServer is listening // lookup port
# $3 = port where I will be listening // broker port
# $4 = my name ("nasdaq" or "tse")

#${JAVA_HOME}/bin/java OnlineBroker localhost 3333 4444 nasdaq
${JAVA_HOME}/bin/java OnlineBroker localhost 3333 2222 tse






