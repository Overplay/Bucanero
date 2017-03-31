package io.ourglass.bucanero.services.SocketIO;

import org.json.JSONException;
import org.json.JSONObject;

import io.ourglass.bucanero.core.OGSystem;
import io.ourglass.bucanero.messages.JsonMessage;

/**
 * Created by mkahn on 3/7/17.
 */

/**
 *
 * messages for return to bellini look like { message: { action: "action-verb", payload: { JSON }}}
 */

public class IdentifyMessage implements JsonMessage {

    public JSONObject toJson(){

        JSONObject rval = new JSONObject();
        JSONObject inner = new JSONObject();
        try {
            inner.put("action", "ident-ack");
            inner.put("payload", OGSystem.getSystemInfo());
            rval.put("message", inner);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return rval;
    }

}