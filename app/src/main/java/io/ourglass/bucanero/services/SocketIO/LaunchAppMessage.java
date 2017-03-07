package io.ourglass.bucanero.services.SocketIO;

/**
 * Created by mkahn on 3/7/17.
 */

public class LaunchAppMessage extends AppActionMessage {

    public String action = "launch";

    public LaunchAppMessage(String appId) {
        super(appId);
    }
}
