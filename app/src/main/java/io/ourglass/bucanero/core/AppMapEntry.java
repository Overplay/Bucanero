package io.ourglass.bucanero.core;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mkahn on 9/7/17.
 */

public class AppMapEntry {

    public enum AppType { WIDGET, CRAWLER, NOTYPE }

    public String appId;
    public int slot;
    public AppType type;

    public AppMapEntry(String appId, int slot, AppType type){
        this.appId = appId;
        this.slot = slot;
        this.type = type;
    }

    // TODO this is bad. And there's probably some built in enum way of doing it...
    public static AppMapEntry.AppType mapFromString(String stringType){

        switch (stringType) {
            case "widget":
                return AppType.WIDGET;
            case "crawler":
                return AppType.CRAWLER;
        }

        return AppType.NOTYPE;

    }

    public JSONObject toJson(){
        JSONObject rval = new JSONObject();
        try {
            rval.put("appId", this.appId);
            rval.put("slot", this.slot);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return rval;
    }
}
