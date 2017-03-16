#!/bin/bash

echo "Usage: ./update_client_handlers.sh <server ip> <server port>"
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


PS3='Please enter your choice: '
options=("Reset" "SimpleTamperHandler" "ReplayHandler" "DoSHandler" "Exit")

# Changes the server handler chain
function cp_chain {
    echo "Updating handler chain..."
    cd Client/src/jaxws/$1
    \cp APIImplService_handler.xml ..
    cd ../../..
    mvn clean compile exec:java -Dws.url="http://$ip:$port/ws.API/endpoint"
    cd ..
    echo "Done!"
}

# Redirects to the version with the logging function
function wants_logging {
    log=""


    while true; do
        read -p "Show message log (y/n): " yn
        case $yn in
            [Yy] )
                log="Logging"
                break
                ;;
            [Nn] )
                break
                ;;
        esac
    done

    cp_chain "$1$log"
}

# Shows the available options at the end of each loop
function show_options {
    i=1
    for op in ${options[@]}; do
        echo "$i) $op"
        i=$((i+1))
    done
}

# Interactive menu
select opt in "${options[@]}"
do
    case $opt in
        "Reset")
            wants_logging Reset
            ;;
        "SimpleTamperHandler")
            wants_logging SimpleTamperHandler
            ;;
        "ReplayHandler")
            wants_logging ReplayHandler
            ;;
        "DoSHandler")
            wants_logging DoSHandler
            ;;
        "Exit")
            echo "Goodbye."
            break
            ;;
    esac

    show_options
done
