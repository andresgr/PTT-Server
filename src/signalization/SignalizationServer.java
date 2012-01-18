/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package signalization;

import conference.ConferenceServer;
import java.net.InetAddress;
import java.util.ArrayList;
import javax.sip.ListeningPoint;
import javax.sip.SipProvider;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.header.ContactHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;

/**
 *
 * @author Usuario
 */
public interface SignalizationServer {

    public SipProvider getDefaultJainSipProvider();

    public AddressFactory getAddressFactory();

    public HeaderFactory getHeaderFactory();

    public MessageFactory getMessageFactory();

    public String generateLocalTag();

    public Address getOurSipAddress();

    public ListeningPoint getDefaultListeningPoint();

    public ArrayList<ViaHeader> getLocalViaHeaders(InetAddress address, 
            ListeningPoint listeningPoint) throws OperationFailedException ;

    public ContactHeader getContactHeader();

    public MaxForwardsHeader getMaxForwardsHeader() throws OperationFailedException;

    public UserAgentHeader getUserAgentHeader();

}
