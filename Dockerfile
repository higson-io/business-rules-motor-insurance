FROM openjdk:11-slim
MAINTAINER Marcin Michalak <marcin.michalak@decerto.pl>

ENV JAVA_OPTS="$JAVA_OPTS -Dlog4j.configurationFile=root/conf/log4j2.xml"

COPY ./target/motor-insurance-1.0-SNAPSHOT.war /app/motor-insurance.war
COPY ./docker/app.properties /root/conf/hyperon-demo-app.properties
COPY ./src/main/resources/log4j.xml /root/conf/log4j.xml

EXPOSE 48080

WORKDIR /app

CMD ./motor-insurance.war
