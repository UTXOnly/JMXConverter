# Use OpenJDK 17 base image
FROM openjdk:17-buster

# Install curl for downloading the OpenTelemetry agent
RUN apt-get update -y && apt-get install -y curl

# Set the working directory
WORKDIR /app

# Download the OpenTelemetry agent
RUN curl -Lo /app/opentelemetry-javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.4.0/opentelemetry-javaagent.jar

# Copy the Java source file into the container
COPY JsonServer.java /app/JsonServer.java

# Compile the Java application, including inner classes
RUN javac JsonServer.java

# Package all compiled classes into a JAR
RUN echo "Main-Class: JsonServer" > manifest.txt && jar cvfm JsonServer.jar manifest.txt *.class

# Copy the JMX metrics configuration file
COPY jmx_metrics_config.yaml /app/jmx_metrics_config.yaml

# Define environment variables for JMX configuration
ENV JAVA_OPTS="-Dcom.sun.management.jmxremote \
               -Dcom.sun.management.jmxremote.port=9000 \
               -Dcom.sun.management.jmxremote.authenticate=false \
               -Dcom.sun.management.jmxremote.ssl=false \
               -Dcom.sun.management.jmxremote.rmi.port=9000 \
               -Djava.rmi.server.hostname=127.0.0.1"

# Run the application as a JAR with the OpenTelemetry agent and JMX options
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -javaagent:/app/opentelemetry-javaagent.jar -Dotel.jmx.config=/app/jmx_metrics_config.yaml -jar JsonServer.jar"]

# Expose the necessary ports
EXPOSE 8080 9000
