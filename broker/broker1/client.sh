#!/bin/bash
# client.sh
#JAVA_HOME=/usr/lib/jvm/java-1.6.0-openjdk-amd64
ECE419_HOME=/cad2/ece419s/
JAVA_HOME=${ECE419_HOME}/java/jdk1.6.0/

# arguments to BrokerClient
# $1 = hostname of where OnlineBroker is located
# $2 = port # where OnlineBroker is listening

${JAVA_HOME}/bin/java BrokerClient localhost 4444



