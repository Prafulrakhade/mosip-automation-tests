color 1e
TITLE MOSIP - Centralized Packet Creator

chdir /d  D:\centralized\mosip-packet-creator

cls

java -Xss8m  -Dfile.encoding=UTF-8 -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=9999,suspend=n -jar mosip-packet-creator-1.2.1-SNAPSHOT.jar --spring.config.location=file:///D:\centralized\mosip-packet-creator\config\application.properties>>D:\centralized\mosip-packet-creator\PacketUtilityRunlog.txt