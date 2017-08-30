package io.ourglass.bucanero.services.SocketIO.SIOActions;

import android.util.Log;

import org.json.JSONObject;

import io.ourglass.bucanero.messages.SystemCommandMessage;

/**
 * Created by mkahn on 8/16/17.
 */

public class VenuePairDoneAction extends SIOAction {

    public VenuePairDoneAction(){
        super("venue_pair_done");
    }

    @Override
    public void process(JSONObject inboundObject) {
        Log.d(TAG, "Socket venue_pair_done command received");

        Log.d(TAG, "Socket venue pair done received");
        (new SystemCommandMessage(SystemCommandMessage.SystemCommand.VENUE_PAIR_DONE)).post();

    }
}
