# SEC
Dependable Password Manager

#Full installation
* Please run the provided install.sh script.

#Maven
1. Go to Proj-SEC16-17/Tools;
2. Run: unzip apache-maven-3.3.9-bin.zip 'apache-maven-3.3.9/*' -d $HOME;
3. Run: export PATH=$HOME/apache-maven-3.3.9/bin:$PATH
4. Confirm the installation by running in a new shell: mvn -v .

#MySQL
* This installation was done on a Ubuntu-based machine.
* The root's password is: secroot2017.

1. Run: sudo apt-get update.
2. Run: sudo apt-get install mysql-server.
3. Run: sudo mysql_secure_installation.
4. To confirm the installation, run: service mysql status.
5. To login as root, run: mysql -u root -p.
