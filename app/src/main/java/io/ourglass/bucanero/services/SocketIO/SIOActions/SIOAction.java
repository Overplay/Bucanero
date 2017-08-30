package io.ourglass.bucanero.services.SocketIO.SIOActions;

import org.json.JSONObject;

/**
 * Created by mkahn on 8/16/17.
 */

public abstract class SIOAction {

    public final String command;
    public String TAG = "SIOAction";

    SIOAction(String command){
        this.command = command;
    }

    public abstract void process(JSONObject inboundObject);

}
