# SEC
Dependable Password Manager

# Developed by group 10
* 79035 - Nuno Afonso
* 79039 - Diogo Silva
* 87785 - Roberto Ponte


# Installation of the tools

## Full installation
1. Please run the provided install.sh script;
2. On a text editor, open the file $HOME/.bashrc;
3. At the end of the file, add the line: export PATH=$HOME/apache-maven-3.3.9/bin:$PATH;
4. To update the settings, run: source $HOME/.bashrc.

## Maven
1. Go to Proj-SEC16-17/Tools;
2. Run: unzip apache-maven-3.3.9-bin.zip 'apache-maven-3.3.9/*' -d $HOME;
3. Run: export PATH=$HOME/apache-maven-3.3.9/bin:$PATH;
4. Confirm the installation by running in a new shell: mvn -v.

## MySQL
* This installation was done on an Ubuntu-based machine.
* The root's password is: **secroot2017**.

1. Run: sudo apt-get update;
2. Run: sudo apt-get install mysql-server;
3. Run: sudo mysql_secure_installation;
4. To confirm the installation, run: service mysql status;
5. To login as root, run: mysql -u root -p;
6. Go to SQL directory;
7. To configure the MySQL server, run: mysql -u root -p < create_database.sql.

## Java Cryptography Extension (JCE) unlimited strength jurisdiction policy
1. Go to Proj-SEC16-17/Tools;
2. Run: unzip jce_policy-8.zip;
3. Go to UnlimitedJCEPolicyJDK8;
4. Run: mv *.jar $JAVA_HOME/jre/lib/security;
5. Delete the UnlimitedJCEPolicyJDK8 directory.

# Running the project

* The following steps allow to run the project only on one local machine;
* However, it can also be run with a remote server and remote clients.

## Generation of the keystores
1. If there is no Proj-SEC16-17/keys directory, run: ./gen_keys.sh http://localhost:8080/ws.API/endpoint client.

## Handlers
1. On Proj-SEC16-17/Handlers, run: mvn clean install.

## Server
1. Start by making the installation of the Handlers;
2. Go to Proj-SEC16-17/Server;
3. Run: mvn clean install;
4. To start the server, run: mvn exec:java.

## Client
1. Start by making the installation of the Server;
2. Check if the server is currently running;
3. On Proj-SEC16-17/Client, run: mvn clean install exec:java.

## Demo of the attacks

1. Confirm that the keystores were previously generated;
2. Run: ./demo_server.sh;
3. On other terminal, run: ./demo_client_handlers.sh;
4. Navigate through the menus to perform the different attacks;
5. On each attack, start by making two register operations to register the public keys of the user and the attacker.
# Thanks.
