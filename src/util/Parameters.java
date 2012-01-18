/*
 * Parametros.java
 *
 * Created on 17 de noviembre de 2004, 12:41
 */
package util;

import java.io.*;
import java.util.*;

/**
 * This class is used for managing the properties file used for configure the application.
 * The implementation of the class is made like a Singleton.
 *
 * @author José Santa Lozano
 */
public class Parameters {

    // Properties file name
    private static String PROPERTIES_FILE = "./sip.properties";

    // Unique instance of the class
    private static Parameters instance = null;
    
    //Keys for the properties
    private static final String PORT = "port";
    private static final String PROXY_ADDRESS = "proxy_address";
    private static final String PROXY_PORT = "proxy_port";
    private static final String PROXY_TRANSPORT = "proxy_transport";
    private static final String TRANSPORT = "transport";
    private static final String SIP_URI = "sip_uri";
    
    // Parameters for the application and their default value
    private int port = 12060;
    private String transport = "UDP";
    private String proxyAddress = "155.54.210.134";
    private int proxyPort = 4060;
    private String proxyTransport = "UDP";
    private String sip_uri = "sip:poc@open-ims.test";

    // Private constructor
    private Parameters() {

        // Read the properties file
        recoverParameters();
    }

    /**
     * Gets the unique instance of the class.
     *
     * @return The instance of class.
     */
    public static Parameters getInstance() {

        if (instance == null) {
            instance = new Parameters();
        }
        return instance;
    }

    /**
     * Sets the configuration file name.
     * This call must be made before any other operation with the class.
     *
     * @param The path of the file
     */
    public static void setConfigFile(String configFile) {

        PROPERTIES_FILE = configFile;
    }

    /**
     * Gets the proxy address of the IMS network
     *
     * @return
     */
    public String getProxyAddress() {
        return proxyAddress;
    }


    /**
     * Gets the proxy port of the IMS network
     *
     * @return
     */
    public int getProxyPort() {
        return proxyPort;
    }

    /**
     * Gets the proxy transport of the IMS network
     *
     * @return
     */
    public String getProxyTransport() {
        return proxyTransport;
    }

    /**
     * Gets the port of this PoC server
     * @return 
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets the preferred transport for this PoC server
     * @return 
     */
    public String getTransport() {
        return transport;
    }

    /**
     * Gets the Sip URI of this PoC server
     * @return 
     */
    public String getSip_uri() {
        return sip_uri;
    }

    
    // Recovery the application parameters from the properties file.
    private void recoverParameters() {

        try {
            // Open the file
            InputStream is = new FileInputStream(PROPERTIES_FILE);
            Properties props = new Properties();
            props.load(is);

            // Obtain each property
            if (props.getProperty(PORT) != null) {
                port = Integer.parseInt(props.getProperty(PORT));
            }
            if (props.getProperty(TRANSPORT) != null) {
                transport = props.getProperty(TRANSPORT);
            }
            if (props.getProperty(PROXY_ADDRESS) != null) {
                proxyAddress = props.getProperty(PROXY_ADDRESS);
            }
            if (props.getProperty(PROXY_PORT) != null) {
                proxyPort = Integer.parseInt(props.getProperty(PROXY_PORT));
            }
            if (props.getProperty(PROXY_TRANSPORT) != null) {
                proxyTransport = props.getProperty(PROXY_TRANSPORT);
            }
            if (props.getProperty(SIP_URI) != null) {
                sip_uri = props.getProperty(SIP_URI);
            }
            // Close throws file
            is.close();
        } catch (IOException ex) {
            System.err.println("Error reading properties file: " + ex);
            ex.printStackTrace(System.err);
            //System.exit(-1);
        }
    }
}
