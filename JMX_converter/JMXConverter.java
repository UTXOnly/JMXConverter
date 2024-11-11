import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.ObjectName;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JMXConverter {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java RemoteMBeanLister <host> <port> <output_filepath>");
            return;
        }

        String host = args[0];
        String port = args[1];
        String outputFilepath = args[2];

        try {
            // Define a list of allowed types
            List<String> allowedTypes = Arrays.asList("int", "long");

            String url = "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi";
            JMXServiceURL serviceURL = new JMXServiceURL(url);
            JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceURL);
            MBeanServerConnection mBeanServer = jmxConnector.getMBeanServerConnection();

            Map<String, Map<String, Map<String, String>>> metrics = new HashMap<>();

            Set<ObjectName> mBeans = mBeanServer.queryNames(null, null);
            for (ObjectName mBeanName : mBeans) {
                String beanName = mBeanName.toString();

                // Get MBean's attributes
                MBeanInfo mBeanInfo = mBeanServer.getMBeanInfo(mBeanName);
                MBeanAttributeInfo[] attributes = mBeanInfo.getAttributes();

                for (MBeanAttributeInfo attrInfo : attributes) {
                    String attrName = attrInfo.getName();
                    String attrType = attrInfo.getType();
                    String attrDesc = attrInfo.getDescription();

                    // Check if the attribute is readable and its type is in the allowed list
                    if (attrInfo.isReadable() && allowedTypes.contains(attrType)) {
                        try {
                            mBeanServer.getAttribute(mBeanName, attrName);

                            // Infer the metric type and unit
                            String metricType = inferMetricType(attrName, attrType);
                            String unit = inferUnitFromDescription(attrDesc);

                            // Add attribute's metric type, description, and inferred unit to the metrics map
                            metrics.computeIfAbsent(beanName, k -> new HashMap<>())
                                   .put(attrName, Map.of("type", metricType, "desc", attrDesc, "unit", unit));

                        } catch (Exception e) {
                            System.out.println(" (Unable to read value: " + e.getMessage() + ")");
                        }
                    }
                }
            }

            // Write the metrics to OpenTelemetry config format
            otelConfig(metrics, outputFilepath);

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

    private static String inferUnitFromDescription(String description) {
        // Attempt to infer units from keywords in the description
        if (description == null) {
            return "{unit}";  // Default unit if description is null
        }

        description = description.toLowerCase();

        if (description.contains("bytes") || description.contains("size")) {
            return "{bytes}";
        } else if (description.contains("milliseconds") || description.contains("ms") || description.contains("time")) {
            return "{ms}";
        } else if (description.contains("requests") || description.contains("connections")) {
            return "{requests}";
        } else if (description.contains("count") || description.contains("number")) {
            return "{count}";
        }
        return "{unit}";  // Default unit if no keyword is matched
    }

    private static String sanitizeBeanName(String beanName) {
        // Replace single quotes with double single quotes and wrap the entire bean name in single quotes
        beanName = beanName.replace("'", "''");
        return "'" + beanName + "'";
    }



    private static void otelConfig(Map<String, Map<String, Map<String, String>>> metrics, String outputFilepath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFilepath))) {
            writer.println("rules:");
    
            for (Map.Entry<String, Map<String, Map<String, String>>> entry : metrics.entrySet()) {
                String beanName = entry.getKey();
    
                if (!isValidBeanName(beanName)) {
                    System.out.println("Skipping invalid bean name: " + beanName);
                    continue;
                }
                String sanitizedBeanName = sanitizeBeanName(beanName);
    
                writer.println("  - bean: " + sanitizedBeanName);
                writer.println("    mapping:");
    
                for (Map.Entry<String, Map<String, String>> attrEntry : entry.getValue().entrySet()) {
                    String attrName = attrEntry.getKey();
                    Map<String, String> attrDetails = attrEntry.getValue();
                    String metricType = attrDetails.get("type");
                    String description = attrDetails.get("desc");
                    String unit = attrDetails.get("unit");
                    String alias = generateAlias(attrName);
    
                    // Write the OpenTelemetry mapping for each attribute in the correct OTel config file structure
                    writer.println("      " + attrName + ":");
                    writer.println("        metric: " + alias);
                    writer.println("        type: " + metricType);
                    writer.println("        desc: \"" + description + "\"");
                    writer.println("        unit: '" + unit + "'");
                }
            }
    
            System.out.println("OTel config file created successfully.");
    
        } catch (IOException e) {
            System.err.println("Failed to write OTel config file: " + e.getMessage());
        }
    }

    
    private static boolean isValidBeanName(String beanName) {
        try {
            ObjectName objectName = new ObjectName(beanName);
            return !objectName.isPattern();
        } catch (MalformedObjectNameException e) {
            return false;
        }
    }


    private static String generateAlias(String attrName) {
        // Convert camelCase to snake_case by inserting underscores before uppercase letters
        String snakeCaseName = attrName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    
        // Add the "jmx." prefix
        return "jmx." + snakeCaseName;
    }

}
