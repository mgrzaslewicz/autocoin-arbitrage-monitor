FROM eclipse-temurin:17-alpine

ADD target/autocoin-arbitrage-monitor*.jar /app/autocoin-arbitrage-monitor.jar

WORKDIR /app
RUN mkdir -p /app/data
EXPOSE 10021
ENTRYPOINT ["java", "-XX:+ExitOnOutOfMemoryError", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/app/data", "-XX:+PrintFlagsFinal", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "autocoin-arbitrage-monitor.jar"]
