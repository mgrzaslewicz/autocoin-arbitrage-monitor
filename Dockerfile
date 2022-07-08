FROM adoptopenjdk/openjdk13:alpine

ADD target/autocoin-arbitrage-monitor*.jar /app/autocoin-arbitrage-monitor.jar

WORKDIR /app
RUN mkdir -p /app/data
EXPOSE 10021
ENTRYPOINT ["java", "-XX:+ExitOnOutOfMemoryError", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/app/data", "-Djava.security.egd=file:/dev/./urandom", "-jar", "autocoin-arbitrage-monitor.jar"]
