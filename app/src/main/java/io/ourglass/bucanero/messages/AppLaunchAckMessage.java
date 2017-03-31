package io.ourglass.bucanero.messages;

/**
 * Created by mkahn on 3/7/17.
 */

/**
 * Inboound JSON should looks like { action: "launch", payload: { appId: "io.ourglass.blah", fullUrl: "path-to-app", appType: "widget|crawler" }}
 */
public class AppLaunchAckMessage extends AppControlOttoMessage {

    public int layoutSlot;

    public AppLaunchAckMessage(String appId, int layoutSlot) throws BadOttoMessageException {
        super(appId); //shut the compiler up
        this.layoutSlot = layoutSlot;
    }
}
