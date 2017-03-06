package io.ourglass.bucanero.tv.Support;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mkahn on 2/12/17.
 */

public class Size {

    public int width;
    public int height;

    public Size(int width, int height){
        this.width = width;
        this.height = height;
    }

    public String toString(){
        return this.width+"x"+this.height;
    }

    public JSONObject toJson() {

        JSONObject rval = new JSONObject();
        try {
            rval.put("width", this.width);
            rval.put("height", this.height);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return rval;
    }

}
