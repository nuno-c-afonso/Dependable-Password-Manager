#!/bin/bash

echo "Usage: ./update_client_handlers.sh <server ip> <server port>"
echo "Default IP: localhost"
echo "Default port: 8080"

# Dealing with arguments
if [ "$#" -gt 1 ]; then
  ip="$1"
  port="$2"
else
    port="8080"
    if [ "$#" -eq 1 ]; then
        ip="$1"
    else
        ip="localhost"
    fi
fi


PS3='Please enter your choice: '
options=("Reset" "Exit")

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
            cp_chain Reset
            ;;
        "Exit")
            echo "Goodbye."
            break
            ;;
    esac

    show_options
done
