package io.ourglass.bucanero.messages;

import org.json.JSONObject;

/**
 * Created by mkahn on 4/13/17.
 */

public class TVControlMessage {

    public String action = "tune";  // only kind for now
    public int toChannel;

    public TVControlMessage(JSONObject inboundJson){

        action = inboundJson.optString("action", "tune");
        toChannel = inboundJson.optInt("channel", 0);

    }
}
