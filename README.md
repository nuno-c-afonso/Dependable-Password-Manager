# SEC
Dependable Password Manager

#Full installation
* Please run the provided install.sh script.

##Maven
1. Go to Proj-SEC16-17/Tools;
2. Run: unzip apache-maven-3.3.9-bin.zip 'apache-maven-3.3.9/*' -d $HOME;
3. Run: export PATH=$HOME/apache-maven-3.3.9/bin:$PATH;
4. Confirm the installation by running in a new shell: mvn -v.

##MySQL
* This installation was done on an Ubuntu-based machine.
* The root's password is: **secroot2017**.

1. Run: sudo apt-get update;
2. Run: sudo apt-get install mysql-server;
3. Run: sudo mysql_secure_installation;
4. To confirm the installation, run: service mysql status;
5. To login as root, run: mysql -u root -p;
6. On MySQL server, run: CREATE USER 'sec_dpm'@'localhost' IDENTIFIED BY 'sec_dpm';

##jUDDI
1. Go to Proj-SEC16-17/Tools;
2. Run: unzip juddi-3.3.2_tomcat-7.0.64_9090.zip 'juddi-3.3.2_tomcat-7.0.64_9090/*' -d $HOME;
3. Run: chmod +x $HOME/juddi-3.3.2_tomcat-7.0.64_9090/bin/*.sh;
4. To startup, run: $HOME/juddi-3.3.2_tomcat-7.0.64_9090/bin/startup.sh.

#Server
* To start jUDDI, run: $HOME/juddi-3.3.2_tomcat-7.0.64_9090/bin/startup.sh;
* To start the server, run: mvn compile exec:java -Dexec.args="http://localhost:9090 API http://localhost:8080/ws.API/endpoint".
