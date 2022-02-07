#!/usr/bin/env bash

SERVER_IDENTITY=../server/src/main/resources/certs/server_identity.jks
SERVER_TRUSTSTORE=../server/src/main/resources/certs/server_truststore.jks
SERVER_CERTIFICATE=../server/src/main/resources/certs/server_identity.cer
CLIENT_IDENTITY=../client/src/main/resources/certs/client_identity.jks
CLIENT_TRUSTSTORE=../client/src/main/resources/certs/client_truststore.jks
CLIENT_CERTIFICATE=../client/src/main/resources/certs/client_truststore.cer

[[ -f $SERVER_IDENTITY ]] && rm $SERVER_IDENTITY
[[ -f $SERVER_TRUSTSTORE ]] && rm $SERVER_TRUSTSTORE
[[ -f $SERVER_CERTIFICATE ]] && rm $SERVER_CERTIFICATE
[[ -f $CLIENT_IDENTITY ]] && rm $CLIENT_IDENTITY
[[ -f $CLIENT_TRUSTSTORE ]] && rm $CLIENT_TRUSTSTORE
[[ -f $CLIENT_CERTIFICATE ]] && rm $CLIENT_CERTIFICATE

# Create server identity
keytool -v -genkeypair \
  -dname "CN=wy8162,OU=Personal,O=Personal,C=US" \
  -keystore $SERVER_IDENTITY \
  -storepass secret \
  -keypass secret \
  -keyalg RSA \
  -keysize 2048 \
  -alias server \
  -validity 3650 \
  -deststoretype pkcs12 \
  -ext KeyUsage=digitalSignature,dataEncipherment,keyEncipherment,keyAgreement \
  -ext ExtendedKeyUsage=serverAuth,clientAuth \
  -ext SubjectAlternativeName:c=DNS:localhost,DNS:raspberrypi.local,IP:127.0.0.1

# Create the Certificate of the Server
keytool -v -exportcert \
  -file $SERVER_CERTIFICATE \
  -alias server \
  -keystore $SERVER_IDENTITY \
  -storepass secret -rfc

# Create trust store for client which has the server certificate
keytool -v -importcert \
  -file $SERVER_CERTIFICATE \
  -alias server \
  -keystore $CLIENT_TRUSTSTORE \
  -storepass secret \
  -noprompt

# Create client identity
keytool -v -genkeypair \
  -dname "CN=wyang,OU=Personal,O=Personal,C=US" \
  -keystore $CLIENT_IDENTITY \
  -storepass secret \
  -keypass secret \
  -keyalg RSA \
  -keysize 2048 \
  -alias client \
  -validity 3650 \
  -deststoretype pkcs12 \
  -ext KeyUsage=digitalSignature,dataEncipherment,keyEncipherment,keyAgreement \
  -ext ExtendedKeyUsage=serverAuth,clientAuth

# Create client certificate
keytool -v -exportcert \
  -file $CLIENT_CERTIFICATE \
  -alias client \
  -keystore $CLIENT_IDENTITY \
  -storepass secret -rfc

# Create server trust store
keytool -v -importcert  \
  -file $CLIENT_CERTIFICATE \
  -alias client \
  -keystore $SERVER_TRUSTSTORE \
  -storepass secret \
  -noprompt
