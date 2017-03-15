#!/bin/bash

PS3='Please enter your choice: '
options=("Reset" "Exit")

# Changes the server handler chain
function cp_chain {
    echo "Updating handler chain..."
    cd Server/src/main/resources/$1
    \cp handler-chain.xml ..
    cd ../../../..
    mvn clean compile
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
