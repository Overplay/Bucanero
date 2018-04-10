package io.ourglass.bucanero.services.SocketIO.SIOActions;

import android.util.Log;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import org.json.JSONObject;

import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.core.OGSystem;
import io.ourglass.bucanero.messages.TVControlMessage;

/**
 * Created by mkahn on 8/16/17.
 */

public class ChannelChangeAction extends SIOAction {

    public ChannelChangeAction(){
        super("tune");
    }

    @Override
    public void process(JSONObject inboundObject) {
        Log.d(TAG, "Socket tune command received");

        TVControlMessage tvcm = new TVControlMessage(inboundObject);
        ABApplication.ottobus.post(tvcm); // in case anyone cares
        OGSystem.changeTVChannel(tvcm.toChannel);

        Answers.getInstance().logCustom(new CustomEvent("ChannelChange")
                .putCustomAttribute("newChannel", inboundObject.optInt("channel", 0)));

//        // TODO: Hack Alert!!
//
//        if ( inboundObject.optInt("channel", 0) == 620 ){
//            Log.d(TAG, "BeIn Hack, moving to floor");
//            (new MoveWebViewMessage("crawler", 1)).post();
//        } else {
//            (new MoveWebViewMessage("crawler", 0)).post();
//        }

    }
}
