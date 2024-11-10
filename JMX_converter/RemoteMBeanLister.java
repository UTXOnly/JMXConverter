import javax.management.*;
import javax.management.remote.*;
import java.io.IOException;
import java.util.Set;

public class RemoteMBeanLister {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java RemoteMBeanLister <hostname> <port>");
            return;
        }

        String hostname = args[0];
        String port = args[1];
        String url = "service:jmx:rmi:///jndi/rmi://" + hostname + ":" + port + "/jmxrmi";

        try {
            // Connect to the remote MBean server
            JMXServiceURL serviceURL = new JMXServiceURL(url);
            JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceURL, null);
            MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();

            // Retrieve all MBean object names
            Set<ObjectName> mBeans = mBeanServerConnection.queryNames(null, null);

            // Iterate over each MBean
            for (ObjectName mBeanName : mBeans) {
                System.out.println("MBean: " + mBeanName);

                // Get MBean's attributes
                MBeanInfo mBeanInfo = mBeanServerConnection.getMBeanInfo(mBeanName);
                MBeanAttributeInfo[] attributes = mBeanInfo.getAttributes();

                for (MBeanAttributeInfo attrInfo : attributes) {
                    String attrName = attrInfo.getName();
                    System.out.print("  Attribute: " + attrName);

                    // Try to get the attribute value if it's readable
                    if (attrInfo.isReadable()) {
                        try {
                            Object attrValue = mBeanServerConnection.getAttribute(mBeanName, attrName);
                            System.out.println(" = " + attrValue);
                            String attType = attrInfo.getType();
                            System.out.println("Type is " + attType);
                        } catch (Exception e) {
                            System.out.println(" (Unable to read value: " + e.getMessage() + ")");
                        }
                    } else {
                        System.out.println(" (Not readable)");
                    }
                }
                System.out.println();
            }

            // Close the connection
            jmxConnector.close();
        } catch (IOException e) {
            System.out.println("Failed to connect to the remote MBean server: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
