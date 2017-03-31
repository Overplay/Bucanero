package io.ourglass.bucanero.services.SocketIO;

import org.json.JSONException;
import org.json.JSONObject;

import io.ourglass.bucanero.messages.JsonMessage;

/**
 * Created by mkahn on 3/7/17.
 */

// TODO this is feeling sloppy already
public class PingAckMessage implements JsonMessage {

    public JSONObject toJson(){

        JSONObject rval = new JSONObject();
        JSONObject inner = new JSONObject();
        try {
            inner.put("action", "ping-ack");
            rval.put("message", inner);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return rval;
    }
}