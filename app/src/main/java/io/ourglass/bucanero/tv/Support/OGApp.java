package io.ourglass.bucanero.tv.Support;

import org.json.JSONObject;

/**
 * Created by mkahn on 4/3/17.
 */

public class OGApp {

    public String appId;
    public String appType;
    public String displayName;
    public int width;
    public int height;

    public OGApp(JSONObject inboundAppJson){


        this.appType = inboundAppJson.optString("appType");
        this.appId = inboundAppJson.optString("appId");
        this.displayName = inboundAppJson.optString("displayName", "SayWhat?");
        this.width = inboundAppJson.optInt("appWidth", 0);
        this.height = inboundAppJson.optInt("appHeight", 0);

    }

    public Size getSize(){
        return new Size(this.width, this.height);
    }

    public String toString(){
        return this.displayName + "("+this.appId+")";
    }

}
