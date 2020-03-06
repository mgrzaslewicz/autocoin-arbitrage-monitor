FROM adoptopenjdk/openjdk13:alpine

ADD target/autocoin-arbitrage-monitor*.jar /app/autocoin-arbitrage-monitor.jar

WORKDIR /app
EXPOSE 10021
ENTRYPOINT [
"java",
"-XX:+ExitOnOutOfMemoryError",
"-Djava.security.egd=file:/dev/./urandom",
"-jar",
"autocoin-arbitrage-monitor.jar"
]
