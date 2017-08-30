package io.ourglass.bucanero.services.SocketIO.SIOMessages;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mkahn on 3/7/17.
 */

/**
 *
 * messages for return to bellini look like { message: { action: "action-verb", payload: { JSON }}}
 */

public class SettingResponseMessage implements JsonMessage {

    String setting;
    String newValue;

    public SettingResponseMessage(String setting, String newValue){

        this.setting = setting;
        this.newValue = newValue;

    }

    public JSONObject toJson(){

        JSONObject rval = new JSONObject();
        JSONObject inner = new JSONObject();

        JSONObject settings = new JSONObject();


        try {
            inner.put("action", "system-setting-ack");
            settings.put(this.setting, this.newValue);
            inner.put("settings", settings);
            rval.put("message", inner);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return rval;
    }

}