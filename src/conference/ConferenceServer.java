/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package conference;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Usuario
 */
public class ConferenceServer {

    private Map<String,ConferenceRoom> activeConferenceRooms;

    public ConferenceServer() {
        activeConferenceRooms = new HashMap<String, ConferenceRoom>();
    }

    public void addConferenceRoom(ConferenceRoom conferenceRoom) {
        activeConferenceRooms.put(conferenceRoom.getId(), conferenceRoom);
    }

    public void removeConferenceRoom(String conferenceRoomId) {
        ConferenceRoom cr = activeConferenceRooms.get(conferenceRoomId);
        if (cr == null)
            return;

        cr.removeAllMembers();
        activeConferenceRooms.remove(conferenceRoomId);
    }

    public void removeConferenceRoom(ConferenceRoom conferenceRoom) {
        activeConferenceRooms.remove(conferenceRoom.getId());
    }

    public ConferenceRoom getConferenceRoom(String id) {
        return activeConferenceRooms.get(id);
    }
    
}
