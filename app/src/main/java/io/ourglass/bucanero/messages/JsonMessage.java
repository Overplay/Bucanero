package io.ourglass.bucanero.messages;

import org.json.JSONObject;

/**
 * Created by mkahn on 3/7/17.
 */

public interface JsonMessage {

    public JSONObject toJson();

}