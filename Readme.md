# Business Rules for Motor Insurance / demo app

This is a sample application to demonstrate capabilities of Hyperon.io library (Java [Business Rules Engine](https://www.hyperon.io/usecase/business-rules-management-system) (BRE)/Java Pricing Engine). 

The application demonstrates a typical sales path of insurance products, in this particular case Car/Motor insurance. The first step (left panel in this example) customer or agent provides all data required to calculate quotations. Each change in the input data on the left panel (eg. Drivers age/Drivers Sex/Number of accidents in the last 5 years) triggers recalculations for all coverages in 3 pricing options (Bronze/Silver/Gold). There is no need for the "Re-Calculate" button as calculations are made instantly which significantly improves UX for the final customer.

The right panel is completely configured in Hyperon, all the rules dictating what to offer in that case and for what price. 
It's done in different ways to show all capabilities of Hyperon. 

You can see an example of making a decision based on parameters (decision tables), simple ones, and also forwarding decision to the next decision table - because this rule engine allows you to stack decision tables onto each other. 
There are also examples of making a decision based on functions.  The Hyperon allows you to implement functions in Rhino (JavaScript) and Groovy languages. 
Although everything in the right panel needs to be calculated after a change in the left panel, it works smoothly because of the very high performance of Hyperon. 
All the configuration is clear and easy to understand because it is organized in the business domain. Each visible element is modeled as a business object with attributes. These attributes are rules. The sample application calls them and get's a ready decision about what to show in that particular case. After downloading this demo you should start with browsing "Domain configuration" in Hyperon and all the configuration should be clear for you within minutes. 

There is a second similar demo of Hyperon built around vehicle insurance subject link it differs with data storage implementation. It uses Hyperon Persistance - dynamic data model. It's another layer of flexibility you can add to your Java application but it is completely optional as you can see in this example and you can use Hyperon without hyperon persistance. 

If want to learn more about how to use Hyperon, check the [Hyperon tutorials](https://www.hyperon.io/docs/tutorials).

## On-line demo

This application can be accessed on-line at: [https://motor-insurance-demo.hyperon.io/](https://motor-insurance-demo.hyperon.io/)

## Prerequisites

Make sure you have at least:

### Java 11

### Maven 

To install go to:

https://maven.apache.org/download.cgi

Previous Maven versions might work as well but this was not checked. 

#### NodeJS 

To install go to:
1. [Windows or Mac OS](https://nodejs.org/en/download/current/)
2. [Linux](https://github.com/nodesource/distributions)

To update:
```text
npm install npm@latest -g
```

#### Hyperon Studio 2.1.3
1. Go to:  https://www.hyperon.io/docs/download
2. download bundle, unpack it to the directory of your choice and run it as described [here](https://www.hyperon.io/tutorial/installing-hyperon-studio). 

## Setup
Make sure that both commands ```mvn``` and ```npm``` are accessible through system path. If not, add them.
In file ```hyperon-demo-app.properties``` set ```hyperon.database.url``` to point Hyperon Studio H2 database file, e.g.:
```properties
hyperon.database.url=jdbc:h2:/srv/hyperon-studio-1.6.50/database/hyperon.demo;AUTO_SERVER=TRUE;MVCC=TRUE;IFEXISTS=TRUE
```
or on Windows
```properties
hyperon.database.url=jdbc:h2:c:/hyperon-studio-1.6.50/database/hyperon.demo;AUTO_SERVER=TRUE;MVCC=TRUE;IFEXISTS=TRUE
```

## Running
Execute below maven command to run Spring Boot.
```shell
mvn clean package
mvn spring-boot:run
```
Application will be accessible on port 48080. If you need to use other port change it in `hyperon-demo-app.properties` -> `server.port`.
URL: [http://localhost:48080/](http://localhost:48080/demo)

## Running with Docker
Assuming you have docker and docker compose installed, motor-demo docker image is available on a [dockerhub](https://hub.docker.
com/r/hyperonio/motor-demo) as `hyperonio/motor-demo:latest`.   
However, if you want to build this app on your own, we've provided a necessary [Dockerfile](./Dockerfile).
Before building the docker image, you need to build the app using the command:
```shell
mvn clean package -Pall_in_one
```
Then, to build a docker image execute the code below:
```shell
docker build -t hyperonio/motor-demo .
```

If image is build, then application can be run in docker container like:
```shell
docker run -p 48080:48080 \ 
    -e hyperon.database.url=<jdbc_url_to_running_db> \
    -e hyperon.database.dialect=<choose> \
    -e hyperon.database.username=<db_username> \
    -e hyperon.database.password=<db_password> \
    -e hyperon.studio.instance-name=hyperon_docker \
    hyperonio/motor-demo
```

OR application can be run with bundle-h2-demo and hyperon-studio images
using docker-compose based on [docker-compose.yml](./docker-compose.yml). Simply run:
```shell
docker-compose up   
# or (regarding to docker version)
docker compose up
```
* By default Hyperon Studio will be available at: [host]:38080/hyperon/app
* By default Demo application will be available at: [host]:48080/demo
* By default Runtime REST will be available at: [host]:8081/api ([host]:8081/swagger-ui.html)

## Executable war
Enable maven profile "all_in_one" to build fully executable war archive, eg.
```shell
mvn clean package -Pall_in_one
```
The resulting war archive can be executed from shell:
```shell
./target/motor-insurance-1.0-SNAPSHOT.war
```
For more details about Spring Boot executables refer to 
[https://docs.spring.io/spring-boot/docs/current/reference/html/deployment-install.html](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment-install.html)

## Configuration files locations 
Configuration files are searched for properties in following order (last matching overrides):
1. classpath:hyperon-demo-app.properties 
2. file:${user.home}/conf/hyperon-demo-app.properties

# Feedback
If you have any feedback regarding this App or Hyperon.io library do not hesitate to contact: [hyperon@decerto.com](mailto:hyperon@decerto.com).
