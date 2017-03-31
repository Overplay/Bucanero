package io.ourglass.bucanero.messages;

import org.json.JSONException;
import org.json.JSONObject;

import io.ourglass.bucanero.tv.Support.Size;

/**
 * Created by mkahn on 3/7/17.
 */

/**
 * Inboound JSON should looks like { action: "launch", payload: { appId: "io.ourglass.blah", fullUrl: "path-to-app", appType: "widget|crawler" }}
 */
public class LaunchAppMessage extends AppControlOttoMessage {

    public String fullUrl;
    public String appType;
    public Size appSize;


    public LaunchAppMessage(JSONObject launchJson) throws BadOttoMessageException {
        super(""); //shut the compiler up
        try {
            this.appId = launchJson.getString("appId");
            this.fullUrl = launchJson.getString("fullUrl");
            this.appType = launchJson.getString("appType");
            int width = launchJson.getInt("width");
            int height = launchJson.getInt("height");
            this.appSize = new Size(width, height);

        } catch (JSONException e) {
            throw new BadOttoMessageException("Missing inbound parameters");
        }
    }
}
