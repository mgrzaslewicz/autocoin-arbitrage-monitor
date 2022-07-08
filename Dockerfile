FROM openjdk:12-jdk-alpine

ADD target/autocoin-arbitrage-monitor*.jar /app/autocoin-arbitrage-monitor.jar

WORKDIR /app
EXPOSE 9011
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","autocoin-arbitrage-monitor.jar"]
