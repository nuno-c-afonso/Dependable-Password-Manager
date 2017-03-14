#!/bin/bash

# Maven
cd Tools
unzip apache-maven-3.3.9-bin.zip 'apache-maven-3.3.9/*' -d $HOME
export PATH=$HOME/apache-maven-3.3.9/bin:$PATH
cd ..

# MySQL
sudo apt-get update
sudo apt-get install mysql-server
sudo mysql_secure_installation
cd SQL
mysql -u root -p < create_database.sql
cd ..
