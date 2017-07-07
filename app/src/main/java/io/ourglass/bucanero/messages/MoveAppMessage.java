package io.ourglass.bucanero.messages;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mkahn on 3/7/17.
 */

public class MoveAppMessage extends AppControlOttoMessage {

    public String moveType;
    public int amount = 0;

    public MoveAppMessage(JSONObject moveJson) throws BadOttoMessageException {

        super(""); //shut the compiler up
        try {
            //JSONObject payload = moveJson.getJSONObject("payload");
            this.appId = moveJson.getString("appId");
            //this.moveType = payload.getString("moveType");
            //this.amount = payload.optInt("amount");

        } catch (JSONException e) {
            throw new BadOttoMessageException("Missing inbound parameters");
        }

    }


}
