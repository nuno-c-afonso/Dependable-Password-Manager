#!/bin/bash

echo "Usage: ./demo_server.sh <server ip> <server port>"
echo "Default IP: localhost"
echo "Starting port: 8080"

ip="localhost"
port="8080"

# Installs the handlers
cd Handlers
mvn clean install
cd ..

# Installs the security functions
cd SecurityFunctions
mvn clean install
cd ..

# Installs the server
cd Server
mvn clean compile
cd ..

# Dealing with arguments
if [ "$#" -gt 1 ]; then
  ip="$1"
  port="$2"
else
    if [ "$#" -eq 1 ]; then
        ip="$1"
    fi
fi

# Gets the number of servers needed for replication
echo ""
read -p "How many faults do you want to tolerate? " n_faults
let "n_servers=3*$n_faults+1"
if [ $n_faults -lt 0 ]; then
  echo "Please enter an integer greater than or equal to 0."
  exit -1
fi

# Asks for the MySQL's root password, to be reused inside the cycle
read -s -p "What's the MySQL's root password? " password

i=0
cmd="client"
while [ $i -lt $n_servers ]
do
  # Creates the different databases
  sql=$(cat "SQL/create_var_database.txt")
  sql="${sql//\$NUM_SERVER\$/$i}"
  echo $sql | mysql -u root -p$password

  # Needed for generating the keystores
  let "tmp_port=$port+$i"
  cmd="$cmd http://$ip:$tmp_port/ws.API/endpoint"
  let "i++"
done

# Creates the keystores for all the entities
rm -rf keys
./gen_keys.sh $cmd

# Starts the server instances in new terminal windows
i=0
while [ $i -lt $n_servers ]
do
  let "tmp_port=$port+$i"
  xterm -xrm 'XTerm.vt100.allowTitleOps: false' -T "Port:$tmp_port" \
    -e "cd Server; mvn compile exec:java -Dws.url=http://$ip:$tmp_port/ws.API/endpoint" &
  let "i++"
done
