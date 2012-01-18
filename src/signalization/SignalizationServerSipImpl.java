/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package signalization;

import conference.ConferenceRoom;
import conference.ConferenceRoomSipImpl;
import conference.ConferenceServer;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.address.AddressFactoryImpl;
import gov.nist.javax.sip.header.HeaderFactoryImpl;
import gov.nist.javax.sip.header.Supported;
import gov.nist.javax.sip.message.MessageFactoryImpl;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.*;
import javax.sip.*;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import util.Logger;
import util.NetworkUtils;
import util.Parameters;

/**
 *
 * @author Usuario
 */
public class SignalizationServerSipImpl implements SignalizationServer, SipListener {

    /**
     * The logger for this class
     */
    private static final Logger logger = Logger.getLogger(SignalizationServerSipImpl.class);
    
    /**
     * The value of the Supported header
     */
    private static final String POC = "PoC";
 
    /**
     * The SipFactory instance used to create the SipStack and the Address
     * Message and Header Factories.
     */
    private SipFactory sipFactory;

    /**
     * The AddressFactory used to create URLs ans Address objects.
     */
    private AddressFactory addressFactory;

    /**
     * The HeaderFactory used to create SIP message headers.
     */
    private HeaderFactory headerFactory;

    /**
     * The Message Factory used to create SIP messages.
     */
    private MessageFactory messageFactory;

    /**
     * The sipStack instance that handles SIP communications.
     */
    private SipStack jainSipStack;

    /**
     * The default listening point that we use for UDP communication..
     */
    private ListeningPoint udpListeningPoint;

    /**
     * The default listening point that we use for TCP communication..
     */
    private ListeningPoint tcpListeningPoint;

    /**
     * The default JAIN SIP provider that we use for UDP communication...
     */
    private SipProvider udpJainSipProvider;

    /**
     * The default JAIN SIP provider that we use for TCP communication...
     */
    private SipProvider tcpJainSipProvider;
    
    /**
     * 
     */
    private static final int PREFERRED_SIP_PORT = 12060;
    
    /**
     * 
     */
    private static final int MAX_PORT_NUMBER = 50000;
    
    /**
     * 
     */
    private static final int MIN_PORT_NUMBER = 1024;

    /**
     * The sip address that we're currently behind (the one that corresponds to
     * our account id).
     */
    private Address ourSipAddress;
    
    /**
     * The default number of binds that a Protocol Provider Service
     * Implementation should execute in case a port is already bound to
     * (each retry would be on a new random port).
     */
    public static final int BIND_RETRIES_DEFAULT_VALUE = 5;
    
    /**
     * indicates whether or not the provider is initialized and ready for use.
     */
    private boolean isInitialized;
    
    /**
     * A table mapping SIP methods to method processors (every processor must
     * implement the SipListener interface). Whenever a new message arrives we
     * extract its method and hand it to the processor instance registered
     */
    private Hashtable methodProcessors = new Hashtable();

    /**
     * The name of the property under which the jain-sip-ri would expect to find
     * the address, port and transport.
     */
    private static final String JSPNAME_OUTBOUND_PROXY =
        "javax.sip.OUTBOUND_PROXY";

    /**
     * The name of the property under which the jain-sip-ri would expect to find
     * the the name of the stack..
     */
    private static final String JSPNAME_STACK_NAME =
        "javax.sip.STACK_NAME";
    /**
     * The name of the property under which the jain-sip-ri would expect to find
     * the name of a debug log file.
     */
    private static final String NSPNAME_DEBUG_LOG =
        "gov.nist.javax.sip.DEBUG_LOG";

    /**
     * The default name of a debug log file for the jain-sip RI.
     */
    private static String NSPVALUE_DEBUG_LOG = "log/sc-jainsipdebug.log";

    /**
     * The name of the property under which the jain-sip-ri would expect to find
     * the name of a server log file (I don't really know what is the
     * difference between this and the DEBUG_LOG).
     */
    private static final String NSPNAME_SERVER_LOG =
        "gov.nist.javax.sip.SERVER_LOG";

    /**
     * The default name of a server log file for the jain-sip RI.
     */
    private static String NSPVALUE_SERVER_LOG  = "log/sc-jainsipserver.log";

    /**
     * The name of the property under which jain-sip will know if it must
     * deliver some unsolicited notify.
     */
    private static final String NSPNAME_DELIVER_UNSOLICITED_NOTIFY =
        "gov.nist.javax.sip.DELIVER_UNSOLICITED_NOTIFY";

    /**
     * The value of the property under which jain-sip will know if it must
     * deliver some unsolicited notify.
     */
    private static final String NSPVALUE_DELIVER_UNSOLICITED_NOTIFY = "true";
    
        /**
     * The name of the property under which the jain-sip-ri would expect to find
     * the log level (detail) for all stack logging.
     */
    private static final String NSPNAME_TRACE_LEVEL =
        "gov.nist.javax.sip.TRACE_LEVEL";

    /**
     * A String indicating the default debug level for the jain-sip-ri (must be
     * log4j compatible).
     */
    private static final String NSPVALUE_TRACE_LEVEL = "TRACE";

    /**
     * The name of the property under which the jain-sip-ri would expect to find
     * a property specifying whether or not it is to cache client connections.
     */
    private static final String NSPNAME_CACHE_CLIENT_CONNECTIONS =
        "gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS";

    /**
     * A default specifyier telling the stack whether or not to cache client
     * connections.
     */
    private static final String NSPVALUE_CACHE_CLIENT_CONNECTIONS = "true";
    
    /**
     * A random generator we use to generate tags.
     */
    private static Random localTagGenerator = new Random();    
    
    public static final String BRANCH_BEGIN = "z9hG4bK-d8754z-";
    public static final String BRANCH_END = "-1---d8754z-";
    
    /**
     * The default maxForwards header that we use in our requests.
     */
    private MaxForwardsHeader maxForwardsHeader = null;

    /**
     * Default number of times that our requests can be forwarded.
     */
    private static final int  MAX_FORWARDS = 70;

    /**
     * The contact header we use in non REGISTER requests.
     */
    private ContactHeader genericContactHeader = null;

    /**
     * The header that we use to identify ourselves.
     */
    private UserAgentHeader userAgentHeader = null;

    /**
     * The address and port of an outbound proxy if we have one (remains null
     * if we are not using a proxy).
     */
    private InetSocketAddress outboundProxySocketAddress = null;

    /**
     * The transport used by our outbound proxy (remains null
     * if we are not using a proxy).
     */
    private String outboundProxyTransport = null;

    private ConferenceServer conferenceServer;
    
    public SignalizationServerSipImpl(ConferenceServer conferenceServer) throws OperationFailedException {

        this.conferenceServer = conferenceServer;

        sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");
        Properties properties = loadSipStackProperties();

        try {
            // Create SipStack object
            jainSipStack = new SipStackImpl(properties);
        } catch (PeerUnavailableException exc) {
            // could not find gov.nist.jain.protocol.ip.sip.SipStackImpl in the classpath
            logger.fatal("Failed to initialize SIP Stack.", exc);
            throw new OperationFailedException("Failed to create sip stack", OperationFailedException.INTERNAL_ERROR, exc);
        }

        //init proxy port
        int preferredSipPort = Parameters.getInstance().getPort();

        if (preferredSipPort > MAX_PORT_NUMBER || preferredSipPort < MIN_PORT_NUMBER) {
                logger.error(preferredSipPort + " is larger than "
                        + MAX_PORT_NUMBER + " and does not "
                        + "therefore represent a valid port nubmer.");
                preferredSipPort = PREFERRED_SIP_PORT;
        }

        initListeningPoints(preferredSipPort);

        /*********** MAS PARAMETROS DE CONFIGURACION DE LA CUENTA********/
        // get the presence options
        /*String enablePresenceObj = (String) accountID.getAccountProperties().get(
                ProtocolProviderFactory.IS_PRESENCE_ENABLED);

        boolean enablePresence = true;
        if (enablePresenceObj != null) {
            enablePresence = Boolean.valueOf(enablePresenceObj).booleanValue();
        }

        String forceP2PObj = (String) accountID.getAccountProperties().get(ProtocolProviderFactory.FORCE_P2P_MODE);

        boolean forceP2P = true;
        if (forceP2PObj != null) {
            forceP2P = Boolean.valueOf(forceP2PObj).booleanValue();
        }

        int pollingValue = 30;
        try {
            String pollingString = (String) accountID.getAccountProperties().get(ProtocolProviderFactory.POLLING_PERIOD);
            if (pollingString != null) {
                pollingValue = Integer.parseInt(pollingString);
            } else {
                logger.warn("no polling value found, using default value"
                        + " (" + pollingValue + ")");
            }
        } catch (NumberFormatException e) {
            logger.error("wrong polling value stored", e);
        }

        int subscriptionExpiration = 3600;
        try {
            String subscriptionString = (String) accountID.getAccountProperties().get(ProtocolProviderFactory.SUBSCRIPTION_EXPIRATION);
            if (subscriptionString != null) {
                subscriptionExpiration = Integer.parseInt(
                        subscriptionString);
            } else {
                logger.warn("no expiration value found, using default value"
                        + " (" + subscriptionExpiration + ")");
            }
        } catch (NumberFormatException e) {
            logger.error("wrong expiration value stored", e);
        }*/

        //create SIP factories.
        headerFactory = new HeaderFactoryImpl();
        addressFactory = new AddressFactoryImpl();
        messageFactory = new MessageFactoryImpl();

        //create our own address.
        String ourSipUri = Parameters.getInstance().getSip_uri();
        try {
            this.ourSipAddress = addressFactory.createAddress(ourSipUri);
        } catch (ParseException ex) {
            logger.error("Could not create a SIP URI for user " + ourSipUri, ex);
            throw new IllegalArgumentException("Could not create a SIP URI for user " + ourSipUri);
        }
        logger.info("Created sip stack for service: " + ourSipAddress);
        isInitialized = true;
    }

        /**
     * Extracts all properties concerning the usage of an outbound proxy for
     * this account.
     * @param jainSipProperties the properties that we will be passing to the
     * jain sip stack when initialize it (that's where we'll put all proxy
     * properties).
     */
    private void initOutboundProxy(Hashtable jainSipProperties)
    {
        //First init the proxy address
        String proxyAddressStr = Parameters.getInstance().getProxyAddress();

        InetAddress proxyAddress = null;
        try {
            proxyAddress = InetAddress.getByName(proxyAddressStr);
        } catch (UnknownHostException ex) {
            logger.error(proxyAddressStr + " appears to be an either invalid or inaccessible address", ex);
            throw new IllegalArgumentException(proxyAddressStr
                + " appears to be an either invalid or inaccessible address " + ex.getMessage());
        }

        //return if no proxy is specified.
        if(proxyAddressStr == null || proxyAddressStr.length() == 0) {
            return;
        }

        //init proxy port
        int proxyPort = Parameters.getInstance().getProxyPort();

        /*String proxyPortStr = (String) accountID.getAccountProperties().get(ProtocolProviderFactory.PROXY_PORT);

        if (proxyPortStr != null && proxyPortStr.length() > 0) {
            try{
                proxyPort = Integer.parseInt(proxyPortStr);
            }catch (NumberFormatException ex){
                logger.error(proxyPortStr + " is not a valid port value. Expected an integer", ex);
                proxyPort = this.DEFAULT_PROXY_PORT;
            }

            if (proxyPort > NetworkUtils.MAX_PORT_NUMBER) {
                throw new IllegalArgumentException(proxyPort + " is larger than " + NetworkUtils.MAX_PORT_NUMBER
                    + " and does not therefore represent a valid port nubmer.");
            }
        }*/

        //proxy transport
        String proxyTransport = Parameters.getInstance().getProxyTransport();

        if (proxyTransport != null && proxyTransport.length() > 0) {
            if (!proxyTransport.equals(ListeningPoint.UDP)
                && !proxyTransport.equals(ListeningPoint.TCP)
                && !proxyTransport.equals(ListeningPoint.TLS))
            {
                throw new IllegalArgumentException(proxyTransport
                    + " is not a valid transport protocol. Transport must be "
                    + "left blanc or set to TCP, UDP or TLS.");
            }
        }else {
            proxyTransport = ListeningPoint.UDP;
        }

        StringBuffer proxyStringBuffer = new StringBuffer(proxyAddress.getHostAddress());

        if(proxyAddress instanceof Inet6Address) {
            proxyStringBuffer.insert(0, '[');
            proxyStringBuffer.append(']');
        }

        proxyStringBuffer.append(':');
        proxyStringBuffer.append(Integer.toString(proxyPort));
        proxyStringBuffer.append('/');
        proxyStringBuffer.append(proxyTransport);

        //done parsing. init properties.
        jainSipProperties.put(JSPNAME_OUTBOUND_PROXY, proxyStringBuffer.toString());

        //store a reference to our sip proxy so that we can use it when
        //constructing via and contact headers.
        this.outboundProxySocketAddress = new InetSocketAddress(proxyAddress, proxyPort);
        this.outboundProxyTransport = proxyTransport;
    }

    /**
     * Loads Sip properties
     * @return Sip Stack Properties
     */
    private Properties loadSipStackProperties() {
         
        Properties properties = new Properties();
        
        //init the proxy
        initOutboundProxy(properties);

        // Sip Stack name.
        properties.setProperty(JSPNAME_STACK_NAME, "PoCServer");

        // NIST SIP specific properties
        properties.setProperty(NSPNAME_DEBUG_LOG, NSPVALUE_DEBUG_LOG);
        properties.setProperty(NSPNAME_SERVER_LOG, NSPVALUE_SERVER_LOG);

        // Drop the client connection after we are done with the transaction.
        properties.setProperty(NSPNAME_CACHE_CLIENT_CONNECTIONS, NSPVALUE_CACHE_CLIENT_CONNECTIONS);

        // Log level
        /*if (accountID.getAccountProperties().containsKey(NSPNAME_TRACE_LEVEL)) {
            properties.setProperty(NSPNAME_TRACE_LEVEL, (String) accountID.getAccountProperties().get(NSPNAME_TRACE_LEVEL));
        } else {
            properties.setProperty(NSPNAME_TRACE_LEVEL, NSPVALUE_TRACE_LEVEL);
        }
        
        // deliver unsolicited NOTIFY
        properties.setProperty(NSPNAME_DELIVER_UNSOLICITED_NOTIFY, NSPVALUE_DELIVER_UNSOLICITED_NOTIFY);*/

        return properties;
    }
    
    /**
     * Traverses all addresses available at this machine and creates a listening
     * points on every one of them. The listening points would be initially
     * bound to the <tt>preferredPortNumber</tt> indicated by the user. If that
     * fails a new random port will be tried. The whole procedure is repeated
     * as many times as specified in the BIND_RETRIES property.
     *
     * @param preferredPortNumber the port number that we'd like listening
     * points to be bound to.
     *
     * @throws OperationFailedException with code NETWORK_ERROR if we faile
     * to bind on a local port while and code INTERNAL_ERROR if a jain-sip
     * operation fails for some reason.
     */
    private void initListeningPoints(int preferredPortNumber) throws OperationFailedException {
        try {
            udpListeningPoint = createListeningPoint(preferredPortNumber
                                                     , ListeningPoint.UDP
                                                     , BIND_RETRIES_DEFAULT_VALUE);
            
            tcpListeningPoint = createListeningPoint(preferredPortNumber
                                                     , ListeningPoint.TCP
                                                     , BIND_RETRIES_DEFAULT_VALUE);

            try {
                udpJainSipProvider = jainSipStack.createSipProvider(udpListeningPoint);
                udpJainSipProvider.addSipListener(this);
                
                tcpJainSipProvider = jainSipStack.createSipProvider(tcpListeningPoint);
                tcpJainSipProvider.addSipListener(this);
            }catch (ObjectInUseException ex) {
                logger.fatal("Failed to create a SIP Provider", ex);
                throw new OperationFailedException(
                    "An error occurred while creating SIP Provider for"
                    , OperationFailedException.INTERNAL_ERROR
                    , ex);
            }
            logger.debug("Created listening points and SIP provider on port " + preferredPortNumber);
        } catch (TransportNotSupportedException ex){
            logger.fatal("Failed to create a listening point", ex);
            throw new OperationFailedException(
                    "A unexpected error occurred while creating listening point"
                    , OperationFailedException.INTERNAL_ERROR
                    , ex);

        }catch (TooManyListenersException ex){
            logger.fatal("Failed to add a provider listener", ex);
            throw new OperationFailedException(
                    "A unexpected error occurred while creating listening point"
                    , OperationFailedException.INTERNAL_ERROR
                    , ex);
        }
        logger.trace("Done creating listening points.");
    }

    /**
     * Creates a listening point for the specified <tt>transport</tt> first
     * trying to bind on the <tt>preferredPortNumber</tt>. If the
     * preferredPortNumber is already used by another program or another
     * listening point, we will be retrying the operation (<tt>bindRetries</tt>
     * times in all.)
     *
     * @param preferredPortNumber the port number that we should first try to
     * bind to.
     * @param transport the transport (UDP/TCP/TLS) of the listening point that
     * we will be creating
     * @param bindRetries the number of times that we should try to bind on
     * different random ports before giving up.
     * @return the newly creaed <tt>ListeningPoint</tt> instance or
     * <tt>null</tt> if we didn't manage to create a listening point in bind
     * retries.
     *
     * @throws OperationFailedException with code NETWORK_ERROR if we faile
     * to bind on a local port while and code INTERNAL_ERROR if a jain-sip
     * operation fails for some reason.
     * @throws TransportNotSupportedException if <tt>transport</tt> is not
     * a valid SIP transport (i.e. not one of UDP/TCP/TLS).
     */
    private ListeningPoint createListeningPoint(int preferredPortNumber,
                                                String transport,
                                                int bindRetries)
        throws OperationFailedException, TransportNotSupportedException {
        
        int currentlyTriedPort = preferredPortNumber;

        ListeningPoint listeningPoint = null;

        //we'll first try to bind to the port specified by the user. if
        //this fails we'll try again times (bindRetries times in all) until
        //we find a free local port.
        for (int i = 0; i < bindRetries; i++) {
            try {
                //make sure that the we don't already have some other
                //listening point on this port (possibly initialized from a
                //different account)
                if (listeningPointAlreadyBound(currentlyTriedPort, transport)) {
                    logger.debug("The following listeing point alredy existed "
                             + "port: " + currentlyTriedPort + " trans: " + transport);
                    throw new InvalidArgumentException("Address already in use");
                }

                listeningPoint = jainSipStack.createListeningPoint(NetworkUtils.IN_ADDR_ANY,
                    currentlyTriedPort, transport);
                
                //we succeeded - break so that we don't try to bind again
                logger.debug("Created LP " + listeningPoint.getIPAddress()
                    + ":" + listeningPoint.getPort() + "/"
                    + listeningPoint.getTransport());

                try {
                    listeningPoint.setSentBy("0.0.0.0");
                }catch (ParseException ex){}
                return listeningPoint;
            }catch (InvalidArgumentException exc){
                if (exc.getMessage().indexOf("Address already in use")== -1){
                    logger.fatal("An exception occurred while trying to create a listening point.", exc);
                    throw new OperationFailedException(
                        "An error occurred while creating listening points. "
                        , OperationFailedException.NETWORK_FAILURE, exc);
                }
            }
            //port seems to be taken. try another one.
            logger.debug("Port " + currentlyTriedPort + " seems in use for transport ." + transport);
            currentlyTriedPort = NetworkUtils.getRandomPortNumber();
            logger.debug("Retrying bind on port " + currentlyTriedPort);
        }

        logger.error("Failed to create a listening point for tranport "+ transport);
        return null;
    }

    
    /**
     * Verifies whether a listening point for the specified port and transport
     * already exists. We need this method because we can only have one single
     * stack per JVM and it is possible for some other account to have already
     * created a listening point for a port that we're trying to bind on. Yet
     * that would be undesirable as we would not like to receive any of the SIP
     * messages meannt for the other account.
     *
     * @param port the port number that we're interested in.
     * @param transport the transport of the listening point we're looking for.
     *
     * @return true if a ListeningPoint already exists for the specified port
     * and tranport and false otherwise.
     */
    private boolean listeningPointAlreadyBound(int port, String transport) {
        if(jainSipStack == null){
            return false;
        }

        //What really matters is not the transport of the listening point but
        //whether it is UDP or TCP (i.e. TLS listening points have to be
        //considered as TCP).
        boolean searchTransportIsUDP = transport.equals(ListeningPoint.UDP);
        Iterator existingListeningPoints = jainSipStack.getListeningPoints();
        while(existingListeningPoints.hasNext()) {
            ListeningPoint lp = (ListeningPoint)existingListeningPoints.next();
            if(lp.getPort() == port) {
                boolean lpIsUDP = lp.getTransport().equalsIgnoreCase(ListeningPoint.UDP);
                if(lpIsUDP == searchTransportIsUDP) {
                    return true;
                }
            }
        }
        return false;
    }
    
    
    
    /**
     * Registers <tt>methodProcessor</tt> in the <tt>methorProcessors</tt>
     * table so that it would receives all messages in a transaction initiated
     * by a <tt>method</tt> request. If any previous processors exist for the
     * same method, they will be replaced by this one.
     *
     * @param method a String representing the SIP method that we're registering
     * the processor for (e.g. INVITE, REGISTER, or SUBSCRIBE).
     * @param methodProcessor a <tt>SipListener</tt> implementation that would
     * handle all messages received within a <tt>method</tt> transaction.
     */
    public void registerMethodProcessor(String      method,
                                        SipListener methodProcessor)
    {
        this.methodProcessors.put(method, methodProcessor);
    }

    /**
     * Unregisters <tt>methodProcessor</tt> from the <tt>methorProcessors</tt>
     * table so that it won't receive further messages in a transaction
     * initiated by a <tt>method</tt> request.
     *
     * @param method the name of the method whose processor we'd like to
     * unregister.
     */
    public void unregisterMethodProcessor(String      method)
    {
        this.methodProcessors.remove(method);
    }

    
    /**
     * Returns a List of Strings corresponding to all methods that we have a
     * processor for.
     * @return a List of methods that we support.
     */
    public List getSupportedMethods()
    {
        return new ArrayList(methodProcessors.keySet());
    }
    

    /**
     * Generar un String aleatorio de length bytes
     */
    public String getRandomHex(int length) {
    	Random rand = new Random();
    	String s = new String();
    	while (s.length() < length) {
    		int n = rand.nextInt();
    		s = s.concat(Integer.toHexString(n));
    	}
        int diff = s.length() - length;
        if (diff > 0)
            s = s.substring(0, s.length() - diff);
    	return s.toLowerCase();
    }




    /****************************************************************************
     ************************ SipListener Implementation*************************
     ****************************************************************************/
    
    @Override
    public void processRequest(RequestEvent requestEvent) {
        logger.debug("received request=" + requestEvent.getRequest().getMethod());

        Request request = requestEvent.getRequest();
           
        ServerTransaction serverTransaction = requestEvent.getServerTransaction();
        SipProvider jainSipProvider = (SipProvider)requestEvent.getSource();

        if (serverTransaction == null) {
            try {
                serverTransaction = jainSipProvider.getNewServerTransaction(
                    request);
            } catch (TransactionAlreadyExistsException ex){
                //let's not scare the user and only log a message
                logger.error("Failed to create a new server transaction for an incoming request\n"
                    + "(Next message contains the request)", ex);
                return;
            } catch (TransactionUnavailableException ex) {
                //let's not scare the user and only log a message
                logger.error("Failed to create a new server transaction for an incoming request\n"
                    + "(Next message contains the request)", ex);
                    return;
            }
        }
        
        // test if a "Supported: PoC" header is present and known
        SupportedHeader supportedHeader = (SupportedHeader)request.getHeader(SupportedHeader.NAME);
        if (supportedHeader == null || 
                ((Supported)supportedHeader).getHeaderValue().compareTo(POC) != 0) {
            
            // we aren't supposed to receive any SIP request without the Supported: Poc
            // header, so just say "not implemented". 
            sendNotImplementedResponse(requestEvent, serverTransaction);
        }

        //INVITE
        if (request.getMethod().equals(Request.INVITE))
        {
            logger.debug("received INVITE");
            if (serverTransaction.getDialog().getState() == null)
            {
                if (logger.isDebugEnabled())
                    logger.debug("request is an INVITE. Dialog state="
                                 + serverTransaction.getDialog().getState());
                processInvite(jainSipProvider, serverTransaction, request);
            }
            else
            {
                logger.error("reINVITE-s are not currently supported.");
            }
        }
        //ACK
        else if (request.getMethod().equals(Request.ACK))
        {
            //processAck(serverTransaction, request);
        }
        //BYE
        else if (request.getMethod().equals(Request.BYE))
        {
            //processBye(serverTransaction, request);
        }
        //CANCEL
        else if (request.getMethod().equals(Request.CANCEL))
        {
            //processCancel(serverTransaction, request);
        } else {
            // This service doesn't implement any other SIP request so just say "not implemented". 
            sendNotImplementedResponse(requestEvent, serverTransaction);
        }
    }

    private void sendNotImplementedResponse(RequestEvent requestEvent, ServerTransaction serverTransaction) {
        try {
            Response response = messageFactory.createResponse(Response.NOT_IMPLEMENTED, requestEvent.getRequest());
            serverTransaction.sendResponse(response);
        } catch (Exception e) {
            logger.error("Error while sending the response 501", e);
            return;
        }
    }
    
    
    /**
     * Returns the transport that we should use if we have no clear idea of our
     * destination's preferred transport. The method would first check if
     * we are running behind an outbound proxy and if so return its transport.
     * If no outbound proxy is set, the method would check the contents of the
     * DEFAULT_TRANSPORT property and return it if not null. Otherwise the
     * method would return UDP;
     *
     * @return The first non null password of the following: a) the transport
     * of our outbound proxy, b) the transport specified by the
     * DEFAULT_TRANSPORT property, c) UDP.
     */
    public String getDefaultTransport() {
        return Parameters.getInstance().getTransport();
    }
    
    /**
     * Returns the default listening point that we use for communication over
     * <tt>transport</tt>.
     *
     * @param transport the transport that the returned listening point needs
     * to support.
     *
     * @return the default listening point that we use for communication over
     * <tt>transport</tt> or null if no such transport is supported.
     */
    public ListeningPoint getListeningPoint(String transport)
    {
        if(transport.equalsIgnoreCase(ListeningPoint.UDP)) {
            return udpListeningPoint;
        }else if(transport.equalsIgnoreCase(ListeningPoint.TCP)) {
            return tcpListeningPoint;
        }
        return null;
    }

    /**
     * Returns the default jain sip provider that we use for communication over
     * <tt>transport</tt>.
     *
     * @param transport the transport that the returned provider needs
     * to support.
     *
     * @return the default jain sip provider that we use for communication over
     * <tt>transport</tt> or null if no such transport is supported.
     */
    public SipProvider getJainSipProvider(String transport)
    {
        if(transport.equalsIgnoreCase(ListeningPoint.UDP)) {
            return udpJainSipProvider;
        }else if(transport.equalsIgnoreCase(ListeningPoint.TCP)) {
            return tcpJainSipProvider;
        }
        return null;
    }

    
    /**
     * Generates a ToTag and attaches it to the to header of <tt>response</tt>.
     *
     * @param response the response that is to get the ToTag.
     * @param containingDialog the Dialog instance that is to extract a unique
     * Tag value (containingDialog.hashCode())
     */
    public void attachToTag(Response response, Dialog containingDialog)
    {
        ToHeader to = (ToHeader) response.getHeader(ToHeader.NAME);
        if (to == null) {
            logger.debug("Strange ... no to tag in response:" + response);
            return;
        }

        if(containingDialog.getLocalTag() != null)
        {
            logger.debug("We seem to already have a tag in this dialog. "
                         +"Returning");
            return;
        }

        try
        {
            if (to.getTag() == null || to.getTag().trim().length() == 0)
            {

                String toTag = generateLocalTag();

                logger.debug("generated to tag: " + toTag);
                to.setTag(toTag);
            }
        }
        catch (ParseException ex)
        {
            //a parse exception here mean an internal error so we can only log
            logger.error("Failed to attach a to tag to an outgoing response."
                         , ex);
        }
    }

    

    /**
     * Creates an invite request destined for <tt>callee</tt>.
     *
     * @param toAddress the sip address of the callee that the request is meant
     * for.
     * @return a newly created sip <tt>Request</tt> destined for
     * <tt>callee</tt>.
     * @throws OperationFailedException with the correspoding code if creating
     * the request fails.
     */
    private Request createInviteRequest(Address toAddress)
        throws OperationFailedException
    {

        InetAddress destinationInetAddress = null;
        if (toAddress.getURI().isSipURI()) {
            try
            {
                System.out.println("URI del to: " + toAddress.getURI());
                System.out.println("Host del to: " + ((SipURI)toAddress.getURI()).getHost());
                destinationInetAddress = InetAddress.getByName(
                    ( (SipURI) toAddress.getURI()).getHost());
            }
            catch (UnknownHostException ex)
            {
                throw new IllegalArgumentException(
                    ( (SipURI) toAddress.getURI()).getHost()
                    + " is not a valid internet address " + ex.getMessage());
            }
        }

        //Call ID
        CallIdHeader callIdHeader = getDefaultJainSipProvider().getNewCallId();

        //CSeq
        CSeqHeader cSeqHeader = null;
        try {
            cSeqHeader = headerFactory.createCSeqHeader(1l, Request.INVITE);
        } catch (InvalidArgumentException ex) {
            //Shouldn't happen
            logger.error("An unexpected erro occurred while constructing the CSeqHeadder", ex);
            throw new OperationFailedException("An unexpected erro occurred while constructing the CSeqHeadder"
                , OperationFailedException.INTERNAL_ERROR, ex);
        } catch(ParseException exc) {
            //shouldn't happen
            logger.error("An unexpected erro occurred while constructing the CSeqHeadder", exc);
            throw new OperationFailedException("An unexpected erro occurred while constructing the CSeqHeadder"
                , OperationFailedException.INTERNAL_ERROR, exc);
        }

        //FromHeader
        String localTag = generateLocalTag();
        FromHeader fromHeader = null;
        ToHeader   toHeader = null;
        try {
            //FromHeader
            fromHeader = headerFactory.createFromHeader(ourSipAddress, localTag);

            //ToHeader
            toHeader = headerFactory.createToHeader(toAddress, null);
        } catch (ParseException ex) {
            //these two should never happen.
            logger.error("An unexpected erro occurred while constructing the ToHeader", ex);
            throw new OperationFailedException("An unexpected erro occurred while constructing the ToHeader"
                , OperationFailedException.INTERNAL_ERROR, ex);
        }

        //ViaHeaders
        ArrayList viaHeaders = null;
        if (destinationInetAddress != null) {
            viaHeaders = getLocalViaHeaders(destinationInetAddress, getDefaultListeningPoint());
        }else {
            try {
                viaHeaders = getLocalViaHeaders(InetAddress.getByName(((SipURI)getContactHeader().getAddress().getURI()).getHost()),
                        getDefaultListeningPoint());
            } catch (UnknownHostException ex) {
                ex.printStackTrace();
            }
        }

        //MaxForwards
        MaxForwardsHeader maxForwards = getMaxForwardsHeader();

        //Contact
        ContactHeader contactHeader = getContactHeader();

        Request invite = null;
        try {
            invite = messageFactory.createRequest(
                toHeader.getAddress().getURI()
                , Request.INVITE
                , callIdHeader
                , cSeqHeader
                , fromHeader
                , toHeader
                , viaHeaders
                , maxForwards);

        } catch (ParseException ex) {
            //shouldn't happen
            logger.error("Failed to create invite Request!", ex);
            throw new OperationFailedException("Failed to create invite Request!"
                , OperationFailedException.INTERNAL_ERROR, ex);
        }

        //User Agent
        UserAgentHeader userAgentHeadr = getUserAgentHeader();
        if(userAgentHeadr != null)
            invite.addHeader(userAgentHeadr);

        //add the contact header.
        invite.addHeader(contactHeader);

        return invite;
    }

    /**
     * Creates a new call and sends a RINGING response.
     *
     * @param sourceProvider the provider containin <tt>sourceTransaction</tt>.
     * @param serverTransaction the transaction containing the received request.
     * @param invite the Request that we've just received.
     */
    private void processInvite( SipProvider       sourceProvider,
                                ServerTransaction serverTransaction,
                                Request           invite)
    {
        logger.trace("Creating call participant.");
        Dialog dialog = serverTransaction.getDialog();
//      CallParticipantSipImpl callParticipant = createCallParticipantFor(serverTransaction, sourceProvider);

        ConferenceRoom conferenceRoom;

        //sdp description may be in acks - bug report Laurent Michel
        ContentLengthHeader cl = invite.getContentLength();
        if (cl != null && cl.getContentLength() > 0) {
            conferenceRoom = new ConferenceRoomSipImpl(this, dialog, (String)invite.getContent());
            //callParticipant.setSdpDescription(new String(invite.getRawContent()));
        } else {
            conferenceRoom = new ConferenceRoomSipImpl(this, dialog);
        }
        logger.trace("Conference room created = " + conferenceRoom.toString());
        conferenceServer.addConferenceRoom(conferenceRoom);

        /*logger.trace("Will verify whether INVITE is properly addressed.");

        //Are we the one they are looking for?
        javax.sip.address.URI calleeURI = dialog.getLocalParty().getURI();

        if (calleeURI.isSipURI()) {
            //user info is case sensitive according to rfc3261
            String calleeUser = ( (SipURI) calleeURI).getUser();
            String localUser = ((SipURI)ourSipAddress.getURI()).getUser();
            if (calleeUser != null && !calleeUser.equals(localUser)) {
                conferenceRoom = null;
                Response notFound = null;
                try {
                    notFound = messageFactory.createResponse( Response.NOT_FOUND, invite);

                    //attach a to tag
                    attachToTag(notFound, dialog);
                    notFound.setHeader(getUserAgentHeader());
                }catch (ParseException ex) {
                    logger.error("Error while trying to create a response", ex);
                    return;
                }

                try {
                    serverTransaction.sendResponse(notFound);
                    logger.debug("sent a not found response: " + notFound);
                }catch (Exception ex) {
                    logger.error("Error while trying to send a response", ex);
                    return;
                }
                return;
            }
        }*/

        ToHeader to = (ToHeader)invite.getHeader(ToHeader.NAME);
        conferenceRoom.addMember(to.getAddress().getURI().toString());

        if (conferenceRoom.inviteMembers()) {
            logger.debug("Invitation sent to members of conference room " + conferenceRoom.getId());
        }else {
            logger.debug("Unable to invite members to conference room " + conferenceRoom.getId());
        }


        //Send RINGING
/*        logger.debug("Invite seems ok, we'll say RINGING.");
        Response ringing = null;
        try {
            ringing = messageFactory.createResponse(Response.RINGING, invite);
            attachToTag(ringing, dialog);
            ringing.setHeader(getUserAgentHeader());

            //set our display name
            //((ToHeader)ringing.getHeader(ToHeader.NAME)).getAddress().setDisplayName(ourDisplayName);

            ringing.addHeader(getContactHeader());
        } catch (ParseException ex) {
            logger.error("Error while trying to send a request", ex);
            return;
        }

        try {
            logger.trace("will send ringing response: ");
            serverTransaction.sendResponse(ringing);
            logger.debug("sent a ringing response: " + ringing);
        }
        catch (Exception ex) {
            logger.error("Error while trying to send a request", ex);
            return;
        }*/
    }

    
    /**
     * Sets the state of the corresponding call participant to DISCONNECTED
     * and sends an OK response.
     *
     * @param serverTransaction the ServerTransaction the the BYE request
     * arrived in.
     * @param byeRequest the BYE request to process
     */
    private void processBye(ServerTransaction serverTransaction, Request byeRequest) {
        //find the call
        /*CallParticipantSipImpl callParticipant = activeCallsRepository
            .findCallParticipant( serverTransaction.getDialog());*/
        ConferenceRoom conferenceRoom = conferenceServer.getConferenceRoom(
                serverTransaction.getDialog().getCallId().getCallId());

        FromHeader fh = (FromHeader)byeRequest.getHeader(FromHeader.NAME);
        String member = fh.getAddress().getURI().toString();

        if (conferenceRoom == null || ! conferenceRoom.hasMember(member)) {
            logger.debug("Received a stray bye request.");
            return;
        }

        //Send OK
        Response ok = null;
        try {
            ok = messageFactory.createResponse(Response.OK, byeRequest);
            attachToTag(ok, serverTransaction.getDialog());
            ok.setHeader(getUserAgentHeader());
        } catch (ParseException ex) {
            logger.error("Error while trying to send a response to a bye", ex);
            //no need to let the user know about the error since it doesn't affect them
            return;
        }

        try {
            serverTransaction.sendResponse(ok);
            logger.debug("sent response " + ok);
        } catch (Exception ex) {
            //This is not really a problem according to the RFC
            //so just dump to stdout should someone be interested
            logger.error("Failed to send an OK response to BYE request, exception was:\n", ex);
        }

        //change status
        //callParticipant.setState(CallParticipantState.DISCONNECTED);
        conferenceRoom.removeMember(POC);
        if (conferenceRoom.getSize() == 0)
            conferenceServer.removeConferenceRoom(conferenceRoom);

    }

    /**
     * Updates the sesion description and sends the state of the corresponding
     * call participant to CONNECTED.
     *
     * @param serverTransaction the transaction that the Ack was received in.
     * @param ackRequest Request
     */
    void processAck(ServerTransaction serverTransaction, Request ackRequest) {
        //find the call
        /*CallParticipantSipImpl callParticipant = activeCallsRepository
            .findCallParticipant(serverTransaction.getDialog());*/
        ConferenceRoom conferenceRoom = conferenceServer.getConferenceRoom(
                serverTransaction.getDialog().getCallId().getCallId());

        FromHeader fh = (FromHeader)ackRequest.getHeader(FromHeader.NAME);
        String member = fh.getAddress().getURI().toString();

        if (conferenceRoom == null || ! conferenceRoom.hasMember(member)) {
            //this is most probably the ack for a killed call - don't signal it
            logger.debug("didn't find an ack's call, returning");
            return;
        }
        conferenceRoom.readyToTalk();
        /**
         * A PARTIR DE AQUI SE PUEDE HABLAR
         */

        /**
         * WE DON'T SUPPORT SDP DESCRIPTIONS IN ACK MESSAGES
         */
        /*
        ContentLengthHeader cl = ackRequest.getContentLength();
        if (cl != null && cl.getContentLength() > 0)
        {
            callParticipant.setSdpDescription(
                                    new String(ackRequest.getRawContent()));
        }
        //change status
        callParticipant.setState(CallParticipantState.CONNECTED);*/
    }

    /**
     * Sets the state of the specifies call participant as DISCONNECTED.
     *
     * @param serverTransaction the transaction that the cancel was received in.
     * @param cancelRequest the Request that we've just received.
     */
    void processCancel(ServerTransaction serverTransaction, Request cancelRequest) {
        //find the call
        /*CallParticipantSipImpl callParticipant = activeCallsRepository
            .findCallParticipant( serverTransaction.getDialog() );

        if (callParticipant == null) {
            logger.debug("received a stray CANCEL req. ignoring");
            return;
        }*/
        ConferenceRoom conferenceRoom = conferenceServer.getConferenceRoom(
                serverTransaction.getDialog().getCallId().getCallId());

        FromHeader fh = (FromHeader)cancelRequest.getHeader(FromHeader.NAME);
        String member = fh.getAddress().getURI().toString();

        if (conferenceRoom == null || ! conferenceRoom.hasMember(member)) {
            //this is most probably the ack for a killed call - don't signal it
            logger.debug("received a stray CANCEL req. ignoring");
            return;
        }

        // Cancels should be OK-ed and the initial transaction - terminated
        // (report and fix by Ranga)
        try {
            Response ok = messageFactory.createResponse(Response.OK, cancelRequest);

            attachToTag(ok, serverTransaction.getDialog());
            ok.setHeader(getUserAgentHeader());
            serverTransaction.sendResponse(ok);
            logger.debug("sent an ok response to a CANCEL request:\n" + ok);
        } catch (ParseException ex) {
            logger.error("Failed to create an OK Response to an CANCEL request.", ex);
            conferenceRoom.removeMember(member);
        } catch (Exception ex) {
            logger.error("Failed to send an OK Response to an CANCEL request.", ex);
            conferenceRoom.removeMember(member);
        }
        conferenceRoom.removeMember(member);
/*        try {
            //stop the invite transaction as well
            Transaction tran = callParticipant.getFirstTransaction();
            //should be server transaction and misplaced cancels should be
            //filtered by the stack but it doesn't hurt checking anyway
            if (! (tran instanceof ServerTransaction)) {
                logger.error("Received a misplaced CANCEL request!");
                return;
            }

            ServerTransaction inviteTran = (ServerTransaction) tran;
            Request invite = callParticipant.getFirstTransaction().getRequest();
            Response requestTerminated =
                protocolProvider.getMessageFactory()
                    .createResponse(Response.REQUEST_TERMINATED, invite);
            requestTerminated.setHeader(
                            protocolProvider.getIMS_OSGi_ClientAgentHeader());
            protocolProvider.attachToTag(requestTerminated
                                         , callParticipant.getDialog());
            inviteTran.sendResponse(requestTerminated);
            if( logger.isDebugEnabled() )
                logger.debug("sent request terminated response:\n"
                              + requestTerminated);
        }catch (ParseException ex) {
            logger.error("Failed to create a REQUEST_TERMINATED Response to "
                         + "an INVITE request."
                         , ex);
        }catch (Exception ex) {
            logger.error("Failed to send an REQUEST_TERMINATED Response to "
                         + "an INVITE request."
                         , ex);
        }

        //change status
        callParticipant.setState(CallParticipantState.DISCONNECTED);
 * 
 */
    }
    
    /**
     * Analyzes the incoming <tt>responseEvent</tt> and then forwards it to the
     * proper event handler.
     *
     * @param responseEvent the responseEvent that we received
     * ProtocolProviderService.
     */
    @Override
    public void processResponse(ResponseEvent responseEvent) {
        ClientTransaction clientTransaction = responseEvent.getClientTransaction();
        Response response = responseEvent.getResponse();
        CSeqHeader cseq = ((CSeqHeader)response.getHeader(CSeqHeader.NAME));

        if (cseq == null){
            logger.error("An incoming response did not contain a CSeq header");
        }
        String method = cseq.getMethod();
        SipProvider sourceProvider = (SipProvider)responseEvent.getSource();

        //OK
        if (response.getStatusCode() == Response.OK) {
            if(method.equals(Request.INVITE)){
                processInviteOK(clientTransaction, response);
            }else if (method.equals(Request.BYE)) {
                //ignore
            }
        }
        //Ringing
        else if (response.getStatusCode() == Response.RINGING) {
            //processRinging(clientTransaction, response);
        }
        //Trying
        else if (response.getStatusCode() == Response.TRYING) {
            //processTrying(clientTransaction, response);
        }
        //Busy here.
        else if (response.getStatusCode() == Response.BUSY_HERE) {
            processBusyHere(clientTransaction, response);
        }

        //401 UNAUTHORIZED
        /*else if (response.getStatusCode() == Response.UNAUTHORIZED
                 || response.getStatusCode() == Response.PROXY_AUTHENTICATION_REQUIRED) {
            processAuthenticationChallenge(clientTransaction, response, sourceProvider);
        }*/
        //errors
        else if ( response.getStatusCode() / 100 == 4 ||
            response.getStatusCode() / 100 == 5)
        {
            /*CallParticipantSipImpl callParticipant = activeCallsRepository
                .findCallParticipant(clientTransaction.getDialog());

            logger.error("Received error: " +response.getStatusCode()
                         +" "+ response.getReasonPhrase());

            if(callParticipant != null)
                callParticipant.setState(CallParticipantState.FAILED);*/
        }
        //ignore everything else.
    }


        /**
     * Sets to CONNECTED that state of the corresponding call participant and
     * sends an ACK.
     * @param clientTransaction the <tt>ClientTransaction</tt> that the response
     * arrived in.
     * @param ok the OK <tt>Response</tt> to process
     */
    private void processInviteOK(ClientTransaction clientTransaction, Response ok){
        //Dialog dialog = clientTransaction.getDialog();
        //find the call
        ConferenceRoom conferenceRoom = conferenceServer.getConferenceRoom(
                clientTransaction.getDialog().getCallId().getCallId());

        FromHeader fh = (FromHeader)ok.getHeader(FromHeader.NAME);
        String member = fh.getAddress().getURI().toString();

        if (conferenceRoom == null || ! conferenceRoom.hasMember(member)) {
            //this is most probably the ack for a killed call - don't signal it
            logger.debug("received a stray ok response");
            return;
        }


        /*if (callParticipant.getState() == CallParticipantState.CONNECTED) {
            // This can happen if the OK UDP packet has been resent due to a
            //timeout. (fix by Michael Koch)
            logger.debug("Ignoring invite OK since call participant is "
                         +"already connected.");
            return;
        }
/*
        Request ack = null;
        ContentTypeHeader contentTypeHeader = null;
        //Create ACK
        try
        {
            //Need to use dialog generated ACKs so that the remote UA core
            //sees them - Fixed by M.Ranganathan
            ack = clientTransaction.getDialog().createRequest(Request.ACK);

            //Content should it be necessary.

            //content type should be application/sdp (not applications)
            //reported by Oleg Shevchenko (Miratech)
            contentTypeHeader =
                protocolProvider.getHeaderFactory().createContentTypeHeader(
                "application", "sdp");
        }
        catch (ParseException ex)
        {
            //Shouldn't happen
            callParticipant.setState(CallParticipantState.FAILED
                , "Failed to create a content type header for the ACK request");
            logger.error(
                "Failed to create a content type header for the ACK request"
                , ex);
        }
        catch (SipException ex)
        {
            logger.error("Failed to create ACK request!", ex);
            callParticipant.setState(CallParticipantState.FAILED);
            return;
        }

        //!!! set sdp content before setting call state as that is where
       //listeners get alerted and they need the sdp
       callParticipant.setSdpDescription(new String(ok.getRawContent()));

        //notify the media manager of the sdp content
        CallSession callSession
            = ((CallSipImpl)callParticipant.getCall()).getMediaCallSession();

        try
        {
            try
            {
                if(callSession == null)
                {
                    //non existent call session - that means we didn't send sdp
                    //in the invide and this is the offer so we need to create
                    //the answer.
                    callSession = NetworkAddressManagerServiceImpl.getMediaService()
                        .createCallSession(callParticipant.getCall());
                    String sdp = callSession.processSdpOffer(
                        callParticipant
                        , callParticipant.getSdpDescription());
                    ack.setContent(sdp, contentTypeHeader);
                }

            }
            finally
            {
                // Send the ACK now since we got all the info we need,
                // and callSession.processSdpAnswer can take a few seconds.
                // (patch by Michael Koch)
                try{
                    clientTransaction.getDialog().sendAck(ack);
                }
                catch (SipException ex)
                {
                    logger.error("Failed to acknowledge call!", ex);
                    callParticipant.setState(CallParticipantState.FAILED);
                    return;
                }
            }

            callSession.processSdpAnswer(callParticipant
                                         , callParticipant.getSdpDescription());
        }
        catch (ParseException exc)
        {
            logger.error("There was an error parsing the SDP description of "
                         + callParticipant.getDisplayName()
                         + "(" + callParticipant.getAddress() + ")"
                        , exc);
            callParticipant.setState(CallParticipantState.FAILED
                , "There was an error parsing the SDP description of "
                + callParticipant.getDisplayName()
                + "(" + callParticipant.getAddress() + ")");
        }
        catch (MediaException exc)
        {
            logger.error("We failed to process the SDP description of "
                         + callParticipant.getDisplayName()
                         + "(" + callParticipant.getAddress() + ")"
                         + ". Error was: "
                         + exc.getMessage()
                        , exc);
            callParticipant.setState(CallParticipantState.FAILED
                , "We failed to process the SDP description of "
                + callParticipant.getDisplayName()
                + "(" + callParticipant.getAddress() + ")"
                + ". Error was: "
                + exc.getMessage());
        }

        //change status
        callParticipant.setState(CallParticipantState.CONNECTED);*/
    }

    /**
     * Sets corresponding state to the call participant associated with this
     * transaction.
     * @param clientTransaction the transaction in which
     * @param busyHere the busy here Response
     */
    private void processBusyHere(ClientTransaction clientTransaction, Response busyHere) {
        /*Dialog dialog = clientTransaction.getDialog();
        //find the call
        //CallParticipantSipImpl callParticipant = activeCallsRepository.findCallParticipant(dialog);

        if (callParticipant == null)
        {
            logger.debug("Received a stray busyHere response.");
            return;
        }

        //change status
        callParticipant.setState(CallParticipantState.BUSY);*/
    }


    @Override
    public void processTimeout(TimeoutEvent te) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void processIOException(IOExceptionEvent ioee) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent tte) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent dte) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Sinalization Server interface methods
     */
    @Override
    public AddressFactory getAddressFactory() {
        return addressFactory;
    }

    @Override
    public HeaderFactory getHeaderFactory() {
        return headerFactory;
    }

    @Override
    public MessageFactory getMessageFactory() {
        return messageFactory;
    }

    @Override
    public Address getOurSipAddress() {
        return ourSipAddress;
    }

    /**
     * Generate a tag for a FROM header or TO header. Just return a random 4
     * digit integer (should be enough to avoid any clashes!) Tags only need to
     * be unique within a call.
     *
     * @return a string that can be used as a tag parameter.
     *
     * synchronized: needed for access to 'rand', else risk to generate same tag
     * twice
     */
    @Override
    public synchronized String generateLocalTag()
    {
            return Integer.toHexString(localTagGenerator.nextInt());
    }

        /**
     * Initializes and returns an ArrayList with a single ViaHeader
     * containing a localhost address usable with the specified
     * s<tt>destination</tt>. This ArrayList may be used when sending
     * requests to that destination.
     * <p>
     * @param destination The address of the destination that the request using
     * the via headers will be sent to.
     * @param srcListeningPoint the listening point that we will be using when
     * accessing destination.
     *
     * @return ViaHeader-s list to be used when sending requests.
     * @throws OperationFailedException code INTERNAL_ERROR if a ParseException
     * occurs while initializing the array list.
     *
     */
    @Override
    public ArrayList getLocalViaHeaders(InetAddress destination, ListeningPoint srcListeningPoint)
        throws OperationFailedException {
        ArrayList viaHeaders = new ArrayList();
        try
        {
            //InetAddress localAddress = networkAddressManagerService.getLocalHost(destination);
            InetAddress localAddress = InetAddress.getLocalHost();
            ViaHeader viaHeader = headerFactory.createViaHeader(
                localAddress.getHostAddress()
                , srcListeningPoint.getPort()
                , srcListeningPoint.getTransport()
                , null
                );
            viaHeader.setBranch(BRANCH_BEGIN + getRandomHex(16) + BRANCH_END);
            viaHeader.setRPort();
            viaHeaders.add(viaHeader);

            //logger.debug("generated via headers:" + viaHeader);
            return viaHeaders;
        } catch (UnknownHostException ex) {
            logger.error( "A UnknownHostException occurred while creating Via Headers!", ex);
            throw new OperationFailedException("A UnknownHostException occurred while creating Via Headers!"
                ,OperationFailedException.INTERNAL_ERROR
                ,ex);
        } catch (ParseException ex) {
            logger.error(
                "A ParseException occurred while creating Via Headers!", ex);
            throw new OperationFailedException(
                "A ParseException occurred while creating Via Headers!"
                ,OperationFailedException.INTERNAL_ERROR
                ,ex);
        } catch (InvalidArgumentException ex) {
            logger.error(
                "Unable to create a via header for port "
                + udpListeningPoint.getPort(),
                ex);
            throw new OperationFailedException(
                "Unable to create a via header for port "
                + udpListeningPoint.getPort()
                ,OperationFailedException.INTERNAL_ERROR
                ,ex);
        }
    }

    /**
     * Initializes and returns this provider's default maxForwardsHeader field
     * using the value specified by MAX_FORWARDS.
     *
     * @return an instance of a MaxForwardsHeader that can be used when
     * sending requests
     *
     * @throws OperationFailedException with code INTERNAL_ERROR if MAX_FORWARDS
     * has an invalid value.
     */
    @Override
    public MaxForwardsHeader getMaxForwardsHeader() throws OperationFailedException {
        if (maxForwardsHeader == null)
        {
            try
            {
                maxForwardsHeader = headerFactory.createMaxForwardsHeader(MAX_FORWARDS);
                logger.debug("generated max forwards: " + maxForwardsHeader.toString());
            }
            catch (InvalidArgumentException ex)
            {
                throw new OperationFailedException(
                    "A problem occurred while creating MaxForwardsHeader"
                    , OperationFailedException.INTERNAL_ERROR
                    , ex);
            }
        }
        return maxForwardsHeader;
    }


    /**
     * Retrns a Contact header containing our sip uri and therefore usable in all
     * but REGISTER requests. Same as calling getContactHeader(false)
     *
     * @return a Contact header containing our sip uri
     */
    @Override
    public ContactHeader getContactHeader()
    {
        if(this.genericContactHeader == null) {
            genericContactHeader = headerFactory.createContactHeader(ourSipAddress);
            logger.debug("generated contactHeader:" + genericContactHeader);
        }
        return genericContactHeader;
    }

    /**
     * Returns the provider that corresponds to the transport returned by
     * getDefaultTransport(). Equivalent to calling
     * getJainSipProvider(getDefaultTransport())
     *
     * @return the Jain SipProvider that corresponds to the transport returned
     * by getDefaultTransport().
     */
    @Override
    public SipProvider getDefaultJainSipProvider()
    {
        return getJainSipProvider(getDefaultTransport());
    }

    /**
     * Returns the listening point that corresponds to the transport returned by
     * getDefaultTransport(). Equivalent to calling
     * getListeningPoint(getDefaultTransport())
     *
     * @return the Jain SipProvider that corresponds to the transport returned
     * by getDefaultTransport().
     */
    @Override
    public ListeningPoint getDefaultListeningPoint()
    {
        return getListeningPoint(getDefaultTransport());
    }

    /**
     * Returns a User Agent header that could be used for signing our requests.
     *
     * @return a <tt>UserAgentHeader</tt> that could be used for signing our
     * requests.
     */
    @Override
    public UserAgentHeader getUserAgentHeader()
    {
        if(userAgentHeader == null)
        {
            try
            {
                List userAgentTokens = new LinkedList();
                userAgentTokens.add("PoC Server");
                userAgentTokens.add("0.1");
                String dateString = new Date().toString().replace(' ', '_').replace(':', '-');
                userAgentTokens.add("CVS-" + dateString);
               userAgentHeader = this.headerFactory.createUserAgentHeader(userAgentTokens);
            }
            catch (ParseException ex) {
                //shouldn't happen
                return null;
            }
        }
        return userAgentHeader;
    }





    /**
     * Class for terminating the SignalizationServerSipImpl Object
     */
    protected class ShutdownThread implements Runnable
    {
        @Override
        public void run() {
            logger.trace("Killing the SIP Protocol Provider.");
            
            try {
                udpJainSipProvider.removeListeningPoint(udpListeningPoint);
                tcpJainSipProvider.removeListeningPoint(tcpListeningPoint);
            }catch (ObjectInUseException ex) {
                logger.info("An exception occurred while ", ex);
            }

            try {
                jainSipStack.stop();
            } catch (Exception ex) {
                //catch anything the stack can throw at us here so that we could
                //peacefully finish our shutdown.
                logger.error("Failed to properly stop the stack!", ex);
            }

            udpListeningPoint = null;
            tcpListeningPoint = null;
            udpJainSipProvider = null;
            tcpJainSipProvider = null;
            headerFactory = null;
            messageFactory = null;
            addressFactory = null;
            sipFactory = null;
            methodProcessors.clear();
            isInitialized = false;
        }
    }
    
}
