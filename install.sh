#!/bin/bash

# Maven
cd Tools
unzip apache-maven-3.3.9-bin.zip 'apache-maven-3.3.9/*' -d $HOME
export PATH=$HOME/apache-maven-3.3.9/bin:$PATH
cd ..
