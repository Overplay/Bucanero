package io.ourglass.bucanero.services.SocketIO.SIOActions;

import android.util.Log;

import org.json.JSONObject;

import io.ourglass.bucanero.services.SocketIO.SIOMessages.IdentifyMessage;

import static io.ourglass.bucanero.services.SocketIO.SocketIOManager.sailsSIODeviceMessage;

/**
 * Created by mkahn on 8/16/17.
 */

public class IdentifyAction extends SIOAction {

    public IdentifyAction(){
        super("identify");
    }


    @Override
    public void process(JSONObject inboundObject) {
        Log.d("PingAction", "Socket identify received");
        sailsSIODeviceMessage(new IdentifyMessage().toJson(), null);
    }
}
