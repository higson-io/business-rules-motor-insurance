FROM openjdk:17-slim-buster
MAINTAINER Cezary Miernik <cezary.miernik@decerto.com>

ENV JAVA_OPTS="$JAVA_OPTS -Dlog4j.configurationFile=root/conf/log4j2.xml"

COPY ./target/motor-insurance.war /app/motor-insurance.war
COPY ./docker/app.properties /root/conf/higson-demo-app.properties
COPY ./src/main/resources/log4j.xml /root/conf/log4j.xml

EXPOSE 48080

WORKDIR /app

CMD ./motor-insurance.war
