#!/bin/sh

NOW=$(date +"%Y_%m_%d__%H_%M_%S")
STORE_PASS="ins3cur3"
KEY_PASS="1nsecure"
D_NAME="CN=SEC,OU=DEI,O=IST,L=Lisbon,S=Lisbon,C=PT"
SUBJ="/CN=SEC/OU=DEI/O=IST/L=Lisbon/C=PT"
KEYS_VALIDITY=90
OUTPUT_FOLDER="keys"

echo "\n\n-->Description: this scrip import the certeficates in the same dir as the script wich name(without .cer) is given into the client keystores that should be located at ./keys/client/client.jks"
echo "\n-->Usage: import_server_cert.sh certeficateFileWithoutTermination [certeficate2] "
for cert_file in $*
do
	echo "Importing the signed certificate of $cert_file to the client at ./keys/client/client.jks"
	keytool -import -keystore "./keys/client/client.jks" -file "$cert_file.cer" -alias $cert_file -storepass $STORE_PASS -keypass $KEY_PASS -noprompt
done
