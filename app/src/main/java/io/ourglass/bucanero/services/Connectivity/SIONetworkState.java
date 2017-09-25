package io.ourglass.bucanero.services.Connectivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by mkahn on 9/21/17.
 */

public class SIONetworkState {

    public enum SIONetState { SIO_CONNECTED, SIO_LOS_YELLOW, SIO_LOS_RED, SIO_DISCONNECTED, SIO_RESET };
    public SIONetState state;
    public Date time;

    public SIONetworkState(SIONetState state){
        this.state = state;
        this.time = new Date();
    }

    public JSONObject toJson(){
        JSONObject rval = new JSONObject();
        try {
            rval.put("state", state.name());
            rval.put("date", time.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return rval;
    }
}
