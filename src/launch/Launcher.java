/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package launch;

import conference.ConferenceServer;
import signalization.OperationFailedException;
import signalization.SignalizationServer;
import signalization.SignalizationServerSipImpl;

/**
 *
 * @author Usuario
 */
public class Launcher {
    
    public static void main (String[] args) {
        try {
            ConferenceServer conferenceServer = new ConferenceServer();
            SignalizationServer server = new SignalizationServerSipImpl(conferenceServer);
        } catch (OperationFailedException ex) {
            ex.printStackTrace();
        }
    }
    
}
