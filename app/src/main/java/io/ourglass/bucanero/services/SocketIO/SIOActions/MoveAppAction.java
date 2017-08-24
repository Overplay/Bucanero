package io.ourglass.bucanero.services.SocketIO.SIOActions;

import android.util.Log;

import org.json.JSONObject;

import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.messages.BadOttoMessageException;
import io.ourglass.bucanero.messages.MoveAppMessage;

/**
 * Created by mkahn on 8/16/17.
 */

public class MoveAppAction extends SIOAction {

    public MoveAppAction(){
        super("move");
    }

    @Override
    public void process(JSONObject inboundObject) {
        try {
            ABApplication.ottobus.post(new MoveAppMessage(inboundObject));
        } catch (BadOttoMessageException e) {
            Log.e(TAG, "Malformed move command, ignoring");
        }
    }
}
