FROM eclipse-temurin:17-alpine

ADD target/autocoin-arbitrage-monitor*.jar /app/autocoin-arbitrage-monitor.jar

RUN mkdir -p /tmp/jar

RUN unzip /app/autocoin-arbitrage-monitor.jar -d /tmp/jar \
 && mkdir -p /scripts/run \
 && mv /tmp/jar/scripts/docker/* /scripts/run/ \
 && rm -rf /tmp/jar

ADD scripts/docker/deploy/copy-run-scripts.sh /scripts/

WORKDIR /app
RUN mkdir -p /app/data
RUN mkdir -p /app/run

RUN adduser -D nonroot
RUN chown -R nonroot:nonroot /app
USER nonroot

EXPOSE 10021

ENV JVM_ARGS=-Xmx400M

ENTRYPOINT java $JVM_ARGS -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/data -XX:+PrintFlagsFinal -Djava.security.egd=file:/dev/./urandom -jar autocoin-arbitrage-monitor.jar
