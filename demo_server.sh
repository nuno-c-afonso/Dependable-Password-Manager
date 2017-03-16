#!/bin/bash

echo "Usage: ./demo_server.sh <server ip> <server port>"
echo "Default IP: localhost"
echo "Default port: 8080"

ip="localhost"
port="8080"

# Dealing with arguments
if [ "$#" -gt 1 ]; then
  ip="$1"
  port="$2"
else
    if [ "$#" -eq 1 ]; then
        ip="$1"
    fi
fi

# Installs the handlers
cd Handlers
mvn clean install
cd ..

# Starts the server
cd Server
mvn clean install exec:java
cd ..
