package io.ourglass.bucanero.core;

import org.json.JSONObject;

/**
 * Created by mkahn on 4/3/17.
 */

public interface JSONCallback {

    public void jsonCallback(JSONObject jsonData);
    public void error(Error err);

}
