package io.ourglass.bucanero.services.SocketIO.SIOActions;

import android.util.Log;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import org.json.JSONObject;

import io.ourglass.bucanero.messages.SystemCommandMessage;

/**
 * Created by mkahn on 8/16/17.
 */

// This does not appear to be used!!! 2/28/2018

@Deprecated
public class VenuePairDoneAction extends SIOAction {

    public VenuePairDoneAction(){
        super("venue_pair_done");
    }

    @Override
    public void process(JSONObject inboundObject) {
        Log.d(TAG, "Socket venue_pair_done command received");
        (new SystemCommandMessage(SystemCommandMessage.SystemCommand.VENUE_PAIR_DONE)).post();
        Answers.getInstance().logCustom(new CustomEvent("VenuePairDone"));
    }
}
