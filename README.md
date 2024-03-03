# HOTELIER: an HOTEL advIsor sERvice
## Creazione JAR ed esecuzione
1. javac -source 1.8 -target 1.8 -cp lib/gson-2.10.1.jar -d out src/code/core/*.java src/code/entities/*.java src/code/enums/*.java src/code/exceptions/*.java src/code/utils/*.java src/code/*.java 
2. jar cfm HotelierServer.jar Manifest.txt -C out . 
3. java -jar HotelierServer.jar