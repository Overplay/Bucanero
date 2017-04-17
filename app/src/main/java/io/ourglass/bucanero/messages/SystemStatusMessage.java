package io.ourglass.bucanero.messages;

/**
 * Created by mkahn on 4/17/17.
 */

public class SystemStatusMessage extends OttobusMainThreadMessage {

    public enum SystemStatus { BOOT_START, BOOT_COMPLETE }

    public SystemStatus status;

    public SystemStatusMessage(SystemStatus status){
        this.status = status;
    }

}
