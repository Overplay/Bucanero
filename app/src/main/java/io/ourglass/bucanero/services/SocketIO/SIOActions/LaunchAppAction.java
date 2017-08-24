package io.ourglass.bucanero.services.SocketIO.SIOActions;

import android.util.Log;

import org.json.JSONObject;

import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.messages.BadOttoMessageException;
import io.ourglass.bucanero.messages.LaunchAppMessage;

/**
 * Created by mkahn on 8/16/17.
 */

public class LaunchAppAction extends SIOAction {

    public LaunchAppAction(){
        super("launch");
    }

    @Override
    public void process(JSONObject inboundObject) {
        Log.d(TAG, "Socket app launch received");

        try {
            ABApplication.ottobus.post(new LaunchAppMessage(inboundObject));
        } catch (BadOttoMessageException e) {
            Log.e(TAG, "Malformed launch command, ignoring");
        }
    }
}
