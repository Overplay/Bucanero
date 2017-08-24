package io.ourglass.bucanero.services.SocketIO.SIOActions;

import android.util.Log;

import org.json.JSONObject;

import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.messages.BadOttoMessageException;
import io.ourglass.bucanero.messages.KillAppMessage;

/**
 * Created by mkahn on 8/16/17.
 */

public class KillAppAction extends SIOAction {

    public KillAppAction(){
        super("kill");
    }

    @Override
    public void process(JSONObject inboundObject) {
        Log.d(TAG, "Socket app kill received");

        try {
            ABApplication.ottobus.post(new KillAppMessage(inboundObject));
        } catch (BadOttoMessageException e) {
            Log.e(TAG, "Malformed kill command, ignoring");
        }
    }
}
