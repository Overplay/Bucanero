package io.ourglass.bucanero.services.SocketIO.SIOActions;

import android.util.Log;

import org.json.JSONObject;

import io.ourglass.bucanero.services.SocketIO.SIOMessages.PingAckMessage;

import static io.ourglass.bucanero.services.SocketIO.SocketIOManager.sailsSIODeviceMessage;

/**
 * Created by mkahn on 8/16/17.
 */

public class PingAction extends SIOAction {

    public PingAction(){
        super("ping");
    }

    @Override
    public void process(JSONObject inboundObject) {
        Log.d("PingAction", "Socket ping received");
        sailsSIODeviceMessage(new PingAckMessage().toJson(), null);
    }
}
