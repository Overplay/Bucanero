package io.ourglass.bucanero.services.SocketIO;

/**
 * Created by mkahn on 3/7/17.
 */

public abstract class AppActionMessage {
    public String appId;
    public AppActionMessage(String appId){
        this.appId = appId;
    }
}
