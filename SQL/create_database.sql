/*IF EXISTS (SELECT * FROM sys.databases WHERE name='sec_dpm')*/
DROP DATABASE IF EXISTS sec_dpm;

CREATE DATABASE sec_dpm;
USE sec_dpm;

CREATE TABLE users(
id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
publickey VARBINARY(4400) NOT NULL
);


CREATE TABLE passwords(
	userID int ,
	username CHARACTER(50) ,
	domain CHARACTER(100),
	password CHARACTER(50) NOT NULL,
	PRIMARY KEY (userID,username,domain),
	FOREIGN KEY(userID)
		REFERENCES users(id)
		ON DELETE CASCADE
);

CREATE TABLE ivs(
	userID INT NOT NULL,
	domainIV BINARY(16) NOT NULL,
	usernameIV BINARY(16) NOT NULL,
	passwordIV BINARY(16) NOT NULL,
	PRIMARY KEY (userID),
	FOREIGN KEY(userID)
		REFERENCES users(id)
		ON DELETE CASCADE
);

CREATE USER 'dpm_account'@'localhost' IDENTIFIED BY 'FDvlalaland129&&';

GRANT SELECT,INSERT,UPDATE ON  sec_dpm.users TO 'dpm_account'@'localhost';
GRANT SELECT,INSERT,UPDATE ON  sec_dpm.passwords TO 'dpm_account'@'localhost';
GRANT SELECT,INSERT,UPDATE ON  sec_dpm.ivs TO 'dpm_account'@'localhost';

-- Will only work on tables with SELECT privilege
GRANT LOCK TABLES ON * TO 'dpm_account'@'localhost';
