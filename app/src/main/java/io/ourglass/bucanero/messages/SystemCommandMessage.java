package io.ourglass.bucanero.messages;

/**
 * Created by mkahn on 4/17/17.
 */

public class SystemCommandMessage extends OttobusMainThreadMessage {

    public enum SystemCommand { DISMISS_OVERLAY, VENUE_PAIR_DONE }

    public SystemCommand status;

    public SystemCommandMessage(SystemCommand status){
        this.status = status;
    }

}
