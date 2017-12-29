# signalr-client-java
SignalR client java implementation example with grade + spring boot.

Pre-requisites
==============
1- SignalR SDK is required, should be build and present as library in lib folder, the original can be  gathered from github
https://github.com/SignalR/java-client . There is a slight change in the code to make it work, that is to use
ServerSentEventsTransport in AutomaticTransport.initialize method.

2- Java web socket 1.3.1 should be placed in lib folder

What it does / How it works
===========================

1- Find the id's to register to signalr observation (optional, ids can be fed with a file via config)
    . Get id's from an ODATA interface
    . A scheduled thread will re-fetch all the resource id's periodically.

2- For each id send observe request via SignalR library

3- Wait for notifications, when a notification is received save data to a map -- this part should be changed as needed

Configuration
=============
Check the src/main/resources/application.properties file. The parameters can be fed from command line as arguments or
the whole application.properties file can be provided externally.
https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html

Build
=====
./gradlew build

Run
===
java -jar build/libs/signalr-client-0.0.1-SNAPSHOT.jar
