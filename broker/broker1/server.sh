#!/bin/bash
# server.sh
#JAVA_HOME=/usr/lib/jvm/java-1.6.0-openjdk-amd64
ECE419_HOME=/cad2/ece419s/
JAVA_HOME=${ECE419_HOME}/java/jdk1.6.0/

# arguments to OnlineBroker
# $1 = listening port

${JAVA_HOME}/bin/java OnlineBroker 4444




