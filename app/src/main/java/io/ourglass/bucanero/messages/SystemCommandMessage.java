package io.ourglass.bucanero.messages;

/**
 * Created by mkahn on 4/17/17.
 */

public class SystemCommandMessage extends OttobusMainThreadMessage {

    public enum SystemCommand { DISMISS_OVERLAY, VENUE_PAIR_DONE, REBOOT, SHOW_HDMI_DEBUG_LAYER, HIDE_HDMI_DEBUG_LAYER }

    public SystemCommand status;

    public SystemCommandMessage(SystemCommand status){
        this.status = status;
    }

}
