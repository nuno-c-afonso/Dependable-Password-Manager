/*IF EXISTS (SELECT * FROM sys.databases WHERE name='sec_dpm')*/
DROP DATABASE IF EXISTS sec_dpm;

CREATE DATABASE sec_dpm;
USE sec_dpm;

CREATE TABLE users(
	id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
	publickey VARBINARY(4400) NOT NULL
);

CREATE TABLE devices(
	id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
	deviceID BINARY(32) NOT NULL,
	userID INT NOT NULL,
	UNIQUE (deviceID),
	FOREIGN KEY (userID)
		REFERENCES users(id)
		ON DELETE CASCADE
);

CREATE TABLE sessions(
	deviceID INT NOT NULL,
	nonce BINARY(64),
	PRIMARY KEY (deviceID, nonce),
	FOREIGN KEY (deviceID)
		REFERENCES devices(id)
		ON DELETE CASCADE
);

CREATE TABLE passwords(
	deviceID INT NOT NULL,
	username VARBINARY(50),
	domain VARBINARY(100),
	password VARBINARY(50) NOT NULL,
	tmstamp INT NOT NULL,
	signature VARBINARY(512),
	PRIMARY KEY (deviceID,username,domain,tmstamp),
	FOREIGN KEY (deviceID)
		REFERENCES devices(id)
		ON DELETE CASCADE
);

CREATE USER IF NOT EXISTS 'dpm_account'@'localhost' IDENTIFIED BY 'FDvlalaland129&&';
CREATE USER IF NOT EXISTS 'dpm_account1'@'localhost' IDENTIFIED BY 'FDvlalaland129&&';

GRANT SELECT,INSERT,UPDATE ON  sec_dpm.users TO 'dpm_account'@'localhost';
GRANT SELECT,INSERT,UPDATE ON  sec_dpm.passwords TO 'dpm_account'@'localhost';
GRANT SELECT,INSERT,UPDATE ON  sec_dpm.sessions TO 'dpm_account'@'localhost';
GRANT SELECT,INSERT,UPDATE ON  sec_dpm.devices TO 'dpm_account'@'localhost';

GRANT SELECT,INSERT,UPDATE ON  sec_dpm.users TO 'dpm_account1'@'localhost';
GRANT SELECT,INSERT,UPDATE ON  sec_dpm.passwords TO 'dpm_account1'@'localhost';
GRANT SELECT,INSERT,UPDATE ON  sec_dpm.sessions TO 'dpm_account1'@'localhost';
GRANT SELECT,INSERT,UPDATE ON  sec_dpm.devices TO 'dpm_account1'@'localhost';

-- Will only work on tables with SELECT privilege
GRANT LOCK TABLES ON * TO 'dpm_account'@'localhost';
GRANT LOCK TABLES ON * TO 'dpm_account1'@'localhost';
