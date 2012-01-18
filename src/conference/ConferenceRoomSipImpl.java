/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package conference;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;
import signalization.OperationFailedException;
import signalization.SignalizationServer;
import util.Logger;

/**
 *
 * @author Usuario
 */
public class ConferenceRoomSipImpl implements ConferenceRoom {

    /**
     * The logger for this class
     */
    private static final Logger logger = Logger.getLogger(ConferenceRoomSipImpl.class);

    private Vector<String> memberList = new Vector<String>();
    private String talkFlag;
    private String conferenceRoomId;

    private String state = ConferenceRoom.CREATING;

    private Dialog dialog;
    private SignalizationServer signalizationServer;
    private String sdpOffer;
            /*= "v=0" + "\n" +
        "o=bell 7827084 7827084 IN IP4 85.132.95.15" + "\n" +
        "s=-" + "\n" +
        "c=IN IP4 85.132.95.15" + "\n" +
        "t=0 0" + "\n" +
        "m=audio 53968 RTP/AVP 8 18 0 101" + "\n" +
        "a=ptime:20" + "\n" +
        "a=sqn:0" + "\n" +
        "a=cdsc:1 image udptl t38" + "\n" +
        "a=cpar:a=T38FaxVersion:0" + "\n" +
        "a=cpar:a=T38MaxBitRate:14400" + "\n" +
        "a=cpar:a=T38FaxRateManagement:transferredTCF" + "\n" +
        "a=cpar:a=T38FaxMaxBuffer:336" + "\n" +
        "a=cpar:a=T38FaxMaxDatagram:176" + "\n" +
        "a=cpar:a=T38FaxUdpEC:t38UDPRedundancy" + "\n" +
        "a=rtpmap:101 telephone-event/8000";
             */

    public void parseSDPContent(String sdpDescription){
        return;
    }

    public ConferenceRoomSipImpl(SignalizationServer ss, Dialog dialog) {
        this.signalizationServer = ss;
        this.dialog = dialog;
        this.conferenceRoomId = dialog.getCallId().getCallId();        
    }

    public ConferenceRoomSipImpl(SignalizationServer ss, Dialog dialog, String sdpContent) {
         this(ss, dialog);
         this.sdpOffer = sdpContent;
         parseSDPContent(sdpContent);
    }
    
    @Override
    public int getSize() {
        return memberList.size();
    }

    @Override
    public String getId() {
        return conferenceRoomId;
    }

    @Override
    public Iterator<String> getMembers() {
        return memberList.iterator();
    }

    @Override
    public boolean addMember(String user) {
        if (hasMember(user))
                return false;

        memberList.add(user);
        return true;
    }

    @Override
    public boolean hasMember(String user) {
        return memberList.contains(user);
    }

    @Override
    public boolean removeMember(String user) {
        if (!hasMember(user))
                return false;

        if (talkFlag.equals(user))
                talkFlag = null;

        memberList.remove(user);
        return true;
    }

    @Override
    public String getTalkFlag() {
        return talkFlag;
    }

    @Override
    public boolean removeAllMembers() {
        memberList = new Vector<String>();
        talkFlag = null;
        return true;
    }

    @Override
    public String toString() {
        return this.conferenceRoomId + "\tMembers: " + memberList.toString();
    }

    @Override
    public boolean inviteMembers() {

        if (state != ConferenceRoom.CREATING)
            return false;
        
        state = ConferenceRoom.INITIATING;
        for (String member : memberList) {
            try {
                if (!sendInvite(signalizationServer.getAddressFactory().createAddress(member))) {
                    return false;
                }

            } catch (OperationFailedException ex) {
                ex.printStackTrace();
            } catch (ParseException ex) {
                ex.printStackTrace();
            }
        }
        return true;
    }

    public void readyToTalk() {
        state = ConferenceRoom.ACTIVE;
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
    private Request createInviteRequest(Address toAddress) throws OperationFailedException {

        InetAddress destinationInetAddress = null;
        if (toAddress.getURI().isSipURI()) {
            try {
                destinationInetAddress = InetAddress.getByName(( (SipURI) toAddress.getURI()).getHost());
            } catch (UnknownHostException ex) {
                throw new IllegalArgumentException(( (SipURI) toAddress.getURI()).getHost()
                    + " is not a valid internet address " + ex.getMessage());
            }
        }

        //Call ID
        CallIdHeader callIdHeader;
        try {
            callIdHeader = signalizationServer.getHeaderFactory().createCallIdHeader(this.conferenceRoomId);
        } catch (ParseException ex) {
            //shouldn't happen
            logger.error("An unexpected erro occurred while constructing the CallIdHeader", ex);
            throw new OperationFailedException("An unexpected erro occurred while "
                + "constructing the CallIdHeader"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        //CSeq
        CSeqHeader cSeqHeader = null;
        try {
            cSeqHeader = signalizationServer.getHeaderFactory().createCSeqHeader(1l, Request.INVITE);
        } catch (InvalidArgumentException ex) {
            //Shouldn't happen
            logger.error("An unexpected erro occurred while constructing the CSeqHeadder", ex);
            throw new OperationFailedException(
                "An unexpected erro occurred while"
                + "constructing the CSeqHeadder"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        } catch(ParseException exc) {
            //shouldn't happen
            logger.error("An unexpected erro occurred while constructing the CSeqHeadder", exc);
            throw new OperationFailedException(
                "An unexpected erro occurred while"
                + "constructing the CSeqHeadder"
                , OperationFailedException.INTERNAL_ERROR
                , exc);
        }

        //FromHeader
        String localTag = signalizationServer.generateLocalTag();
        FromHeader fromHeader = null;
        ToHeader   toHeader = null;
        try{
            //FromHeader
            fromHeader = signalizationServer.getHeaderFactory()
                .createFromHeader(signalizationServer.getOurSipAddress(), localTag);

            //ToHeader
            toHeader = signalizationServer.getHeaderFactory().createToHeader(toAddress, null);
        }catch (ParseException ex){
            //these two should never happen.
            logger.error("An unexpected erro occurred while constructing the ToHeader", ex);
            throw new OperationFailedException(
                "An unexpected erro occurred while constructing the ToHeader"
                , OperationFailedException.INTERNAL_ERROR, ex);
        }

        //ViaHeaders
        ArrayList viaHeaders = null;
        if (destinationInetAddress != null) {
            viaHeaders = signalizationServer.getLocalViaHeaders(destinationInetAddress
                , signalizationServer.getDefaultListeningPoint());
        }else {
            try {
                viaHeaders = signalizationServer.getLocalViaHeaders(
                        InetAddress.getByName(((SipURI)signalizationServer.
                            getContactHeader().getAddress().getURI()).getHost()),
                        signalizationServer.getDefaultListeningPoint());
            } catch (UnknownHostException ex) {
                ex.printStackTrace();
            }
        }

        //MaxForwards
        MaxForwardsHeader maxForwards = signalizationServer.getMaxForwardsHeader();

        //Contact
        ContactHeader contactHeader = signalizationServer.getContactHeader();

        Request invite = null;
        try {
            invite = signalizationServer.getMessageFactory().createRequest(
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
            throw new OperationFailedException(
                "Failed to create invite Request!"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        //User Agent
        UserAgentHeader userAgentHeader = signalizationServer.getUserAgentHeader();
        if(userAgentHeader != null)
            invite.addHeader(userAgentHeader);

        //add the contact header.
        invite.addHeader(contactHeader);
        return invite;
    }

    /**
     * Init and establish the specified call.
     *
     * @param calleeAddress the address of the callee that we'd like to connect
     * with.
     *
     * @return CallParticipant the CallParticipant that will represented by
     *   the specified uri. All following state change events will be
     *   delivered through that call participant. The Call that this
     *   participant is a member of could be retrieved from the
     *   CallParticipatn instance with the use of the corresponding method.
     *
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call.
     */
    private boolean sendInvite(Address calleeAddress) throws OperationFailedException {
        //create the invite request
        Request invite = createInviteRequest(calleeAddress);

        //Content
        ContentTypeHeader contentTypeHeader = null;
        try {
            //content type should be application/sdp (not applications)
            //reported by Oleg Shevchenko (Miratech)
            contentTypeHeader = signalizationServer.getHeaderFactory().createContentTypeHeader(
                    "application", "sdp");
        } catch (ParseException ex) {
            //Shouldn't happen
            logger.error("Failed to create a content type header for the INVITE request", ex);
            throw new OperationFailedException(
                "Failed to create a content type header for the INVITE "
                + "request"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        //Transaction
        ClientTransaction inviteTransaction;
        SipProvider jainSipProvider = signalizationServer.getDefaultJainSipProvider();
        try {
            inviteTransaction = jainSipProvider.getNewClientTransaction(invite);
        } catch (TransactionUnavailableException ex) {
            logger.error("Failed to create inviteTransaction.\n"
                + "This is most probably a network connection error."
                , ex);
            throw new OperationFailedException(
                "Failed to create inviteTransaction.\n"
                + "This is most probably a network connection error."
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        //invite content
        try {
            /*CallSession callSession = NetworkAddressManagerServiceImpl.getMediaService()
                .createCallSession(callParticipant.getCall());
            ((CallSipImpl)callParticipant.getCall())
                .setMediaCallSession(callSession);

            //if possible try to indicate the address of the callee so
            //that the media service can choose the most proper local
            //address to advertise.
            javax.sip.address.URI calleeURI = calleeAddress.getURI();
            InetAddress intendedDestination = null;
            if(calleeURI.isSipURI()) {
                String host = ((SipURI)calleeURI).getHost();
                try {
                    intendedDestination = InetAddress.getByName(host);
                    invite.setContent(callSession.createSdpOffer(intendedDestination), contentTypeHeader);
                } catch (UnknownHostException ex) {
                    logger.warn("Failed to obtain an InetAddress for " + host, ex);
                }
            }*/
            if (sdpOffer != null)
                invite.setContent(sdpOffer, contentTypeHeader);

        }catch (ParseException ex) {
            logger.error("Failed to parse sdp data while creating invite request!", ex);
            throw new OperationFailedException(
                "Failed to parse sdp data while creating invite request!"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }
        /*catch (MediaException ex) {
            logger.error(
                "Failed to parse sdp data while creating invite request!"
                , ex);
            throw new OperationFailedException(
                "Failed to parse sdp data while creating invite request!"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }*/

        try {
            inviteTransaction.sendRequest();
            if (logger.isDebugEnabled())
                logger.debug("sent request: " + invite);
        } catch (SipException ex) {
            logger.error("An error occurred while sending invite request", ex);
            throw new OperationFailedException(
                "An error occurred while sending invite request"
                , OperationFailedException.NETWORK_FAILURE
                , ex);
        }

        //return (CallSipImpl)callParticipant.getCall();
        return true;
    }


}
