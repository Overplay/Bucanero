package io.ourglass.bucanero.services.SocketIO.SIOActions;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import io.ourglass.bucanero.core.OGSystem;
import io.ourglass.bucanero.messages.VenuePairCompleteMessage;

/**
 * Created by mkahn on 8/16/17.
 */

public class CloudUpdateAction extends SIOAction {

    public CloudUpdateAction(){
        super("cloud_record_update");
    }

    @Override
    public void process(JSONObject inboundObject) {
        Log.d(TAG, "Socket cloud_record_update command received");
        OGSystem.updateFromOGCloud();
        try {
            JSONObject change = inboundObject.getJSONObject("change");
            if (change.has("atVenueUUID")) {
                (new VenuePairCompleteMessage()).post();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
