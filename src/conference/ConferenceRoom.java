/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package conference;

import java.util.Iterator;

/**
 *
 * @author Usuario
 */
public interface ConferenceRoom {

    public static final String CREATING = "CREATING";
    public static final String INITIATING = "INITIATING";
    public static final String ACTIVE = "ACTIVE";
    public static final String TOKEN_FREE = "TOKEN_FREE";
    public static final String TOKEN_RESERVED = "TOKEN_RESERVED";
    public static final String TERMINATING = "TERMINATING";

    public int getSize();

    /**
     * Each conference room is identified by the call-id of the creating transaction
     * @return The conference room id
     */
    public String getId();

    public Iterator<String> getMembers();

    public boolean addMember(String user);

    public boolean hasMember(String user);

    public boolean removeMember(String user);

    public String getTalkFlag();

    public boolean removeAllMembers();

    @Override
    public String toString();

    public boolean inviteMembers();

    public void readyToTalk();
    
}
