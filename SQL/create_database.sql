/*IF EXISTS (SELECT * FROM sys.databases WHERE name='sec_dpm')*/
DROP DATABASE IF EXISTS sec_dpm;

CREATE DATABASE sec_dpm;
USE sec_dpm;

CREATE TABLE users(
id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
publickey VARBINARY(4400) NOT NULL
);

CREATE TABLE sessions(
	sessionID INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
	userID INT,
	nonce BINARY(64),
	UNIQUE (userID, nonce),
	FOREIGN KEY (userID)
		REFERENCES users(id)
		ON DELETE CASCADE
);

CREATE TABLE passwords(
	sessionID INT NOT NULL,
	username VARBINARY(50),
	domain VARBINARY(100),
	password VARBINARY(50) NOT NULL,
	counter INT NOT NULL,
	tmstamp INT NOT NULL,
	signature VARBINARY(512),
	PRIMARY KEY (sessionID,counter),
	FOREIGN KEY (sessionID)
		REFERENCES sessions(sessionID)
		ON DELETE CASCADE
);

CREATE USER 'dpm_account'@'localhost' IDENTIFIED BY 'FDvlalaland129&&';

GRANT SELECT,INSERT,UPDATE ON  sec_dpm.users TO 'dpm_account'@'localhost';
GRANT SELECT,INSERT,UPDATE ON  sec_dpm.passwords TO 'dpm_account'@'localhost';
GRANT SELECT,INSERT,UPDATE ON  sec_dpm.sessions TO 'dpm_account'@'localhost';

-- Will only work on tables with SELECT privilege
GRANT LOCK TABLES ON * TO 'dpm_account'@'localhost';
