# Hyperon Motor-Insurance Demo App

This is a sample application to demonstrate capabilities of [Hyperon.io](https://hyperon.io) library (Java Business Rules Engine (BRE)/Java Pricing Engine). 

The application demonstrates responsive quotations for Car/Motor Insurance based on decision tables and Rhino functions (for math calculations).
Each change in the input data on left panel (eg. Drivers age/Drivers Sex/Number of accidents in the last 5 years) triggers recalculations for 
all coverages in 3 pricing options (Bronze/Silver/Gold).
There is no need for **"Re-Calculate"** button as calculations are made instantly which significantly improves UX for final customer.  

Hyperon.io tutorials are availble [here](http://hyperon.io/tutorials/getting-started).

## On-line demo

This application can be accessed on-line at: [http://motor-insurance-demo.hyperon.io/](http://motor-insurance-demo.hyperon.io/)

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

In file ```hyperon-demo-app.properties``` set ```hyperon.database.url``` to point Hyperon Studio H2 database file, e.g.:
```text
hyperon.database.url=jdbc:h2:/srv/hyperon-studio-1.6.50/database/hyperon.demo;AUTO_SERVER=TRUE;MVCC=TRUE;IFEXISTS=TRUE
```
or on Windows
```text
hyperon.database.url=jdbc:h2:c:/hyperon-studio-1.6.50/database/hyperon.demo;AUTO_SERVER=TRUE;MVCC=TRUE;IFEXISTS=TRUE
```

## Running

Execute below maven command to run Spring Boot.

```text
mvn clean package
mvn spring-boot:run
```

Application will be accessible on port 48080. If you need to use other port change it in ```hyperon-demo-app.properties``` -> ```server.port```.
URL: [http://localhost:48080/](http://localhost:48080/)

## Running with Docker
This demo application can be run in docker container based on provided Dockerfile.
For building image execute code below:
```text
docker build -t motor-demo .
```
If image is build, then application can be run in docker container like:
```text
docker run -p 38080:8080 
    -e mpp.database.url=<jdbc_url_to_running_db>
    -e mpp.database.dialect=<choose>
    -e mpp.database.username=<db_username>
    -e mpp.database.password=<db_password>
    -e mpp.environment.id=hyperon_docker
    motor-demo
```
OR application can be run with bundle-h2-demo and hyperon-studio images
using docker-compose based on docker-compose.yml. Simply run:
```text
docker-compose up
```
* By default Hyperon Studio will be available at: [host]:38080/hyperon/app
* By default Demo application will be available at: [host]:48080

## Executable war

Enable maven profile "all_in_one" to build fully executable war archive, eg.

```text
mvn clean package -Pall_in_one
```

The resulting war archive can be executed from shell:

```text
./target/motor-insurance-1.0-SNAPSHOT.war
```
For more details about Spring Boot executables refer to 
[https://docs.spring.io/spring-boot/docs/current/reference/html/deployment-install.html](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment-install.html)

## Configuration files locations 

Configuration files are searched for properties in following order (last matching overrides):
1. classpath:hyperon-demo-app.properties 
2. file:${user.home}/conf/hyperon-demo-app.properties

# Feedback

If you have any feedback regarding this App or Hyperon.io library do not hesitate to contact: [hyperon@decerto.pl](mailto:hyperon@decerto.pl).