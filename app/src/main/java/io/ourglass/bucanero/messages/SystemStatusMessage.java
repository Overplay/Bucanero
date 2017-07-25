package io.ourglass.bucanero.messages;

/**
 * Created by mkahn on 4/17/17.
 */

public class SystemStatusMessage extends OttobusMainThreadMessage {

    // NETWORK_LOS means comms to cloud-dm.ourglass.tv
    public enum SystemStatus { BOOT_START, BOOT_COMPLETE, NETWORK_LOS, NETWORK_CONNECTED, NETWORK_ISSUE, STB_LOS, STB_CONNECTED,
        HDMI_RX_LOS, HDMI_CONFIGURED, HDMI_PLAY, HDMI_STOP, HDMI_SEVERE_ERROR, HDMI_RX_LINK }

    public SystemStatus status;

    public Exception relatedException;

    public SystemStatusMessage(SystemStatus status){
        this.status = status;
    }

    public SystemStatusMessage(SystemStatus status, Exception exception){
        this.status = status;
        this.relatedException = exception;
    }

    public static void sendStatusMessage(SystemStatus status){
        (new SystemStatusMessage(status)).post();
    }

    public static void sendStatusMessageWithException(SystemStatus status, Exception e){
        (new SystemStatusMessage(status, e)).post();
    }


}
