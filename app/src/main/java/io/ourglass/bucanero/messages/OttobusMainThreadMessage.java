package io.ourglass.bucanero.messages;

import io.ourglass.bucanero.core.ABApplication;

/**
 * Created by mkahn on 4/17/17.
 */

public abstract class OttobusMainThreadMessage {
    public void post(){
        ABApplication.ottobus.post(this);
    }
}
