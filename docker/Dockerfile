# Use OpenJDK 17 base image
FROM openjdk:21-buster

# Install curl for downloading the OpenTelemetry agent
RUN apt-get update -y && apt-get install -y curl

# Set the working directory
WORKDIR /app

# Download the OpenTelemetry agent
RUN curl -Lo /app/opentelemetry-javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.4.0/opentelemetry-javaagent.jar

COPY ./jetty_app jetty_app/

COPY jmx_metrics_config.yaml /app

WORKDIR /app/jetty_app

RUN ./gradlew clean shadowJar

ENV JAVA_OPTS="-Dcom.sun.management.jmxremote \
               -Dcom.sun.management.jmxremote.port=9000 \
               -Dcom.sun.management.jmxremote.authenticate=false \
               -Dcom.sun.management.jmxremote.ssl=false \
               -Dcom.sun.management.jmxremote.rmi.port=9000 \
               -Djava.rmi.server.hostname=127.0.0.1"

ENTRYPOINT [ "sh", "-c", "java ${JAVA_OPTS} -javaagent:../opentelemetry-javaagent.jar -Dotel.jmx.config=/app/jmx_metrics_config.yaml -jar /app/jetty_app/app/build/libs/app-all.jar"]

EXPOSE 8080 9000