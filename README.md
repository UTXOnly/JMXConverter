# JMX_converter
Fetch JMX metrics and create config files for Datadog + OTel

## Testing

Run a test TOmcat server to test this on:

```bash
docker run -d   -p 8080:8080   -p 9000:9000   --name tomcat-jmx   -e CATALINA_OPTS="-Dcom.sun.management.jmxremote \
                    -Dcom.sun.management.jmxremote.port=9000 \
                    -Dcom.sun.management.jmxremote.local.only=false \
                    -Dcom.sun.management.jmxremote.authenticate=false \
                    -Dcom.sun.management.jmxremote.ssl=false \
                    -Dcom.sun.management.jmxremote.rmi.port=9000 \
                    -Djava.rmi.server.hostname=localhost"   tomcat:10.1-jdk11
```
