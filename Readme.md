# Hyperon Motor-Insurance Demo App

This a sample application to demonstrate capabilities of Hyperon.io library. 

Hyperon.io tutorials are availble [here](http://hyperon.io/tutorials/getting-started).

## Prerequisites

Make sure you have at least:

#### Java 1.8

#### Maven 3.3 

To install go to:

https://maven.apache.org/download.cgi

Previous Maven versions might work as well but this was not checked. 

#### NodeJS 4.4.1

To install go to:

1. [Windows or Mac OS](https://nodejs.org/en/download/current/)

2. [Linux](https://github.com/nodesource/distributions)

To update:
```text
npm install npm@latest -g
```

#### Hyperon Studio 1.5.22

1. Go to:

http://hyperon.io/download
 
2. download bundle, unpack it to the directory of your choice and run it as described [here](http://hyperon.io/tutorials/deploying-hyperon-studio). 

## Setup

Make sure that both commands ```mvn``` and ```npm``` are accesible through system path. If not, add them.

In file ```app.properties``` set ```hyperon.database.url``` to point Hyperon Studio H2 database file, e.g.:
```text
hyperon.database.url=jdbc:h2:/srv/hyperon-studio-1.5.88/database/hyperon.demo.motor;AUTO_SERVER=TRUE;MVCC=TRUE;IFEXISTS=TRUE
```
or on Windows
```text
hyperon.database.url=jdbc:h2:c:/hyperon-studio-1.5.88/database/hyperon.demo.motor;AUTO_SERVER=TRUE;MVCC=TRUE;IFEXISTS=TRUE
```

## Running

Execute below maven command to run Spring Boot.

```text
mvn clean package
mvn spring-boot:run
```

Application will be accessible on port 48080. If you need to use other port change it in ```app.properties``` -> ```server.port```.
URL: [http://localhost:48080/](http://localhost:48080/)
