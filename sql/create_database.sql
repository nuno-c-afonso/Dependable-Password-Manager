/*IF EXISTS (SELECT * FROM sys.databases WHERE name='sec_dpm')*/
DROP DATABASE IF EXISTS sec_dpm;

CREATE DATABASE sec_dpm;
USE sec_dpm;

CREATE TABLE users(
id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
publickey VARBINARY(4096) NOT NULL
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
