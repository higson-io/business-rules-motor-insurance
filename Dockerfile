FROM eclipse-temurin:17-jre-jammy
LABEL maintainer="Cezary Miernik <cezary.miernik@decerto.com>"

# Extra JVM options, overridable at runtime: docker run -e JAVA_OPTS="-Xmx512m ..."
ENV JAVA_OPTS=""

COPY ./target/motor-insurance.war /app/motor-insurance.war
COPY ./docker/app.properties /root/conf/higson-demo-app.properties

EXPOSE 48080

WORKDIR /app

CMD ["sh", "-c", "java $JAVA_OPTS -jar motor-insurance.war"]
