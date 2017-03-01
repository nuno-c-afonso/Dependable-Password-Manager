/*Register a new user*/
INSERT INTO users (publickey) VALUES (00000001);

/*Check if user alredy exists*/
SELECT id
FROM users
where publickey = 1;

/*Save password*/
INSERT INTO passwords VALUES (1,'utilizador','dominio.com','palavrapass');


/*Check if password exist/get the password*/
SELECT password
FROM passwords
WHERE userID=1 and username='utilizador' and domain='dominio.com';


/* Update password*/
UPDATE passwords
SET password='NovaPalavraPass'
WHERE userID=1;

