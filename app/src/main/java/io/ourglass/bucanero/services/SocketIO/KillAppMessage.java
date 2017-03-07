package io.ourglass.bucanero.services.SocketIO;

/**
 * Created by mkahn on 3/7/17.
 */

public class KillAppMessage extends AppActionMessage {

    public String action = "kill";

    public KillAppMessage(String appId) {
        super(appId);
    }
}
