import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RemoteMBeanLister {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java RemoteMBeanLister <host> <port>");
            return;
        }

        String host = args[0];
        String port = args[1];

        try {
            // Define a list of allowed types
            List<String> allowedTypes = Arrays.asList("int", "long");

            // Connect to the remote MBean server
            String url = "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi";
            JMXServiceURL serviceURL = new JMXServiceURL(url);
            JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceURL);
            MBeanServerConnection mBeanServer = jmxConnector.getMBeanServerConnection();

            // Map to store the MBeans and their attributes that meet the criteria
            Map<String, Map<String, String>> metrics = new HashMap<>();

            // Iterate over each MBean
            Set<ObjectName> mBeans = mBeanServer.queryNames(null, null);
            for (ObjectName mBeanName : mBeans) {
                String beanName = mBeanName.toString();

                // Get MBean's attributes
                MBeanInfo mBeanInfo = mBeanServer.getMBeanInfo(mBeanName);
                MBeanAttributeInfo[] attributes = mBeanInfo.getAttributes();

                for (MBeanAttributeInfo attrInfo : attributes) {
                    String attrName = attrInfo.getName();
                    String attrType = attrInfo.getType();

                    // Check if the attribute is readable and the type is in the allowed list
                    if (attrInfo.isReadable() && allowedTypes.contains(attrType)) {
                        try {
                            // Retrieve the attribute value to confirm it is accessible
                            mBeanServer.getAttribute(mBeanName, attrName);

                            // Infer the metric type
                            String metricType = inferMetricType(attrName, attrType);

                            // Add attribute and inferred metric type to the metrics map
                            metrics.computeIfAbsent(beanName, k -> new HashMap<>()).put(attrName, metricType);

                        } catch (Exception e) {
                            System.out.println(" (Unable to read value: " + e.getMessage() + ")");
                        }
                    }
                }
            }

            // Write the metrics to metrics.yaml, including host and port from the arguments
            writeMetricsYaml(metrics, host, port);

            // Close the JMX connection
            jmxConnector.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String inferMetricType(String attributeName, String attributeType) {
        if (attributeName.toLowerCase().contains("count") || attributeName.toLowerCase().contains("total")) {
            return "counter";
        } else if (attributeName.toLowerCase().contains("usage") || attributeName.toLowerCase().contains("size") ||
                   attributeName.toLowerCase().contains("load")) {
            return "gauge";
        } else if (attributeName.toLowerCase().contains("percentile") || attributeName.toLowerCase().contains("average") ||
                   attributeName.toLowerCase().contains("min") || attributeName.toLowerCase().contains("max")) {
            return "histogram";
        }
        return "gauge";
    }

    private static void writeMetricsYaml(Map<String, Map<String, String>> metrics, String host, String port) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("metrics.yaml"))) {
            // Write the Datadog JMX YAML structure with the specified host and port
            writer.println("instances:");
            writer.println("  - host: " + host);
            writer.println("    name: jmx_instance");
            writer.println("    port: " + port);
            writer.println();
            writer.println("init_config:");
            writer.println("  conf:");

            // Group all attributes under a single `include` without `bean` field and add `alias` and `metric_type`
            writer.println("      - include:");
            writer.println("          attribute:");

            for (Map.Entry<String, Map<String, String>> entry : metrics.entrySet()) {
                for (Map.Entry<String, String> attrEntry : entry.getValue().entrySet()) {
                    String attrName = attrEntry.getKey();
                    String metricType = attrEntry.getValue();

                    // Write the attribute configuration with alias and metric_type
                    writer.println("              " + attrName + ":");
                    writer.println("                  alias: " + generateAlias(entry.getKey(), attrName));
                    writer.println("                  metric_type: " + metricType);
                }
            }

            System.out.println("metrics.yaml file created successfully for remote JVM at " + host + ":" + port);

        } catch (IOException e) {
            System.err.println("Failed to write metrics.yaml: " + e.getMessage());
        }
    }

    private static String generateAlias(String beanName, String attrName) {
        // Generate a unique alias with 'jmx.' prefix based on the bean and attribute name
        String aliasBase = beanName.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
        String aliasAttr = attrName.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
        return "jmx." + aliasBase + "." + aliasAttr;
    }
}
