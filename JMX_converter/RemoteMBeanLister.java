import javax.management.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RemoteMBeanLister {

    public static void main(String[] args) {
        try {
            // Define a list of allowed types
            List<String> allowedTypes = Arrays.asList("int", "long", "boolean");

            // Connect to the platform MBean server
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            Set<ObjectName> mBeans = mBeanServer.queryNames(null, null);

            // Map to store the MBeans and their attributes that meet the criteria
            Map<String, List<String>> metrics = new HashMap<>();

            // Iterate over each MBean
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

                            // Add attribute to the metrics map
                            metrics.computeIfAbsent(beanName, k -> new java.util.ArrayList<>()).add(attrName);

                        } catch (Exception e) {
                            System.out.println(" (Unable to read value: " + e.getMessage() + ")");
                        }
                    }
                }
            }

            // Write the metrics to metrics.yaml
            writeMetricsYaml(metrics);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeMetricsYaml(Map<String, List<String>> metrics) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("metrics.yaml"))) {
            // Write the Datadog JMX YAML structure
            writer.println("instances:");
            writer.println("  - host: 127.0.0.1");
            writer.println("    name: jmx_instance");
            writer.println("    port: 9999");
            writer.println();
            writer.println("init_config:");
            writer.println("  conf:");
            
            // Group all bean configurations under a single include section
            for (Map.Entry<String, List<String>> entry : metrics.entrySet()) {
                writer.println("      - include:");
                writer.println("          bean: " + entry.getKey());
                writer.println("          attribute:");
    
                for (String attrName : entry.getValue()) {
                    writer.println("            - " + attrName);
                }
            }
    
            System.out.println("metrics.yaml file created successfully with combined configuration.");
    
        } catch (IOException e) {
            System.err.println("Failed to write metrics.yaml: " + e.getMessage());
        }
    }

}
