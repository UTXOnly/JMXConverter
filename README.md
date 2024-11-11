# JMXConverter

JMXConverter is a Java program that connects to a running Java application, collects JMX metrics, and generates an [OpenTelemetry JMX configuration file](https://opentelemetry.io/blog/2023/jmx-metric-insight/#further-capabilities-of-the-module) for the OpenTelemetry Java agent.


* Automatically Generates OpenTelemetry Configuration: Collects JMX metrics from a target Java application and formats them for OpenTelemetry monitoring.
  * Connects via JMX RMI connection scheme
  * Collects every readable Mbean and `int` + `long` type 
  * Infers metric type and unit from MBean description
* Testable Docker Sandbox: A Docker Compose sandbox is provided to test this setup with a sample web server and Datadog exporter. Swap in your preferred OpenTelemetry exporter if needed.

#### Installation
Clone the repository:

```bash
git clone https://github.com/UTXOnly/JMXConverter.git
cd <repository-folder>
```




### Usage


Start the docker compose stack to get the sample Java application running first as this program generates the config file by polling a running JMX server.


```bash
docker-compose up
```

#### Note

The sampe Java application needs to be running in order to run the JMX collector. There is a blank `jmx_metrics_config.yaml` file that is built into the Docker image as it is inclued in the container `ENTRYPOINT`. The application will start with the blank metric config file and only populates when the `JMX_converter` is run. Once the file is generated you can bring down the docker compose stack. Then, delete the application image and bring the docker compose stack back up to rebuild the docker container.

The JMX metric configuation file is evaluated at application startup, so unfortunately the inconvenient procedure mentioned above is necessary.

The opentelemetry collector is configured with the Datadog exporter, but you can add your own if needed. To use the Datadog exporter, create a `.env` file in the Repo's `/docker` directory.

**For example:**
```
DD_API_KEY=<YOUR_API_KEY>
DD_SITE=<YOUR_SITE>
```


#### Compile JMX_converter:

```bash
javac JMXConverter.java
```

To run JMX_remote provide the hostname and port of the remote JMX server as arguments. This will create an OpenTelemetry-compatible YAML configuration file called `jmx_metrics_config.yaml` in the `/docker` directory to be used by the sandbox environment.



```bash
java JMXConverter <host> <port> <output_filepath>
```    

##### Example:

```bash
java JMXConverter localhost 9000 ../docker/jmx_metrics_config.yaml # Use this filepath to put config file in properlocation for sandbox testing
```


**Example Configuration Output**

The generated `jmx_metrics_config.yaml` will look like this:

```yaml
rules:
  - bean: 'java.lang:name=Metaspace,type=MemoryPool'
    mapping:
      UsageThresholdCount:
        metric: jmx.usage_threshold_count
        type: counter
        desc: "UsageThresholdCount"
        unit: '{count}'
      UsageThreshold:
        metric: jmx.usage_threshold
        type: gauge
        desc: "UsageThreshold"
        unit: '{unit}'
  - bean: 'java.lang:name=CodeHeap ''profiled nmethods'',type=MemoryPool'
    mapping:
      UsageThresholdCount:
        metric: jmx.usage_threshold_count
        type: counter
        desc: "UsageThresholdCount"
        unit: '{count}'
      UsageThreshold:
        metric: jmx.usage_threshold
        type: gauge
        desc: "UsageThreshold"
        unit: '{unit}'

```

#### Troubleshooting
* **YAML Parsing Errors:** Ensure that unit fields are formatted as strings (e.g., unit: '{count}').
* **ClassCastException:** This error typically occurs if the YAML format is incorrect. Verify that special characters in bean names are handled correctly.
* **NoClassDefFoundError:** If using a JAR file, ensure all .class files, including inner classes, are properly included.

### TO DO
* [ ] CLI menu
* [ ] Support Datadog JMX integration configuration files
* [ ] Collect metric prefix from user input

