/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package util;

import java.net.*;
import java.util.*;
import java.text.*;

/**
 * Utility methods and fields to use when working with network addresses.
 *
 * @author Andres Garcia
 */
public class NetworkUtils
{
    private static final Logger logger
        = Logger.getLogger(NetworkUtils.class);
    /**
     * A string containing the "any" local address.
     */
    public static final String IN_ADDR_ANY = "0.0.0.0";

    /**
     * The maximum int value that could correspond to a port nubmer.
     */
    public static final int    MAX_PORT_NUMBER = 65535;

    /**
     * The minimum int value that could correspond to a port nubmer bindable
     * by the SIP Communicator.
     */
    public static final int    MIN_PORT_NUMBER = 1024;


    /**
     * The random port number generator that we use in getRandomPortNumer()
     */
    private static Random portNumberGenerator = new Random();

    /**
     * Determines whether the address is the result of windows auto configuration.
     * (i.e. One that is in the 169.254.0.0 network)
     * @param add the address to inspect
     * @return true if the address is autoconfigured by windows, false otherwise.
     */
    public static boolean isWindowsAutoConfiguredIPv4Address(InetAddress add)
    {
        return (add.getAddress()[0] & 0xFF) == 169
            && (add.getAddress()[1] & 0xFF) == 254;
    }

    /**
     * Determines whether the address is an IPv4 link local address. IPv4 link
     * local addresses are those in the following networks:
     *
     * 10.0.0.0    to 10.255.255.255
     * 172.16.0.0  to 172.31.255.255
     * 192.168.0.0 to 192.168.255.255
     *
     * @param add the address to inspect
     * @return true if add is a link local ipv4 address and false if not.
     */
    public static boolean isLinkLocalIPv4Address(InetAddress add)
    {
        if (add instanceof Inet4Address)
        {
            byte address[] = add.getAddress();
            if ( (address[0] & 0xFF) == 10)
                return true;
            if ( (address[0] & 0xFF) == 172
                && (address[1] & 0xFF) >= 16 && address[1] <= 31)
                return true;
            if ( (address[0] & 0xFF) == 192
                && (address[1] & 0xFF) == 168)
                return true;
            return false;
        }
        return false;
    }

    /**
     * Returns a random local port number that user applications could bind to.
     * (i.e. above 1024).
     * @return a random int located between 1024 and 65 535.
     */
    public static int getRandomPortNumber()
    {
        return getRandomPortNumber(MIN_PORT_NUMBER, MAX_PORT_NUMBER);
    }

    /**
     * Returns a random local port number, greater than min and lower than max.
     *
     * @param min the minimum allowed value for the returned port number.
     * @param max the maximum allowed value for the returned port number.
     *
     * @return a random int located between greater than min and lower than max.
     */
    public static int getRandomPortNumber(int min, int max)
    {
        return portNumberGenerator.nextInt(max - min) + min;
    }

    /**
     * Returns a random local port number, greater than min and lower than max.
     * If the pair flag is set to true, then the returned port number is
     * guaranteed to be pair. This is useful for protocols that require this
     * such as RTP
     *
     * @param min the minimum allowed value for the returned port number.
     * @param max the maximum allowed value for the returned port number.
     * @param pair specifies whether the caller would like the returned port to
     * be pair.
     *
     * @return a random int located between greater than min and lower than max.
     */
    public static int getRandomPortNumber(int min, int max, boolean pair)
    {
        if(pair)
        {
            int delta = max - min;
            delta /= 2;
            int port = getRandomPortNumber(min, min + delta);
            return port * 2;
        }
        else
        {
            return getRandomPortNumber(min, max);
        }
    }

    /**
     * Verifies whether <tt>address</tt> could be an IPv6 address string.
     *
     * @param address the String that we'd like to determine as an IPv6 address.
     *
     * @return true if the address containaed by <tt>address</tt> is an ipv6
     * address and falase otherwise.
     */
    public static boolean isIPv6Address(String address)
    {
        return (address != null && address.indexOf(':') != -1);
    }

    
}
