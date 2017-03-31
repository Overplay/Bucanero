package io.ourglass.bucanero.messages;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mkahn on 3/7/17.
 */

public class KillAppMessage extends AppControlOttoMessage {

    public KillAppMessage(JSONObject killJson) throws BadOttoMessageException {
        super(""); //shut the compiler up
        try {
            this.appId = killJson.getString("appId");
        } catch (JSONException e) {
            throw new BadOttoMessageException("Missing inbound parameters");
        }
    }
}
