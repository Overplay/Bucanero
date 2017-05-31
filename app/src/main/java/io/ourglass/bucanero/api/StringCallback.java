package io.ourglass.bucanero.api;

import io.ourglass.bucanero.objects.NetworkException;

/**
 * Created by mkahn on 4/3/17.
 */

public interface StringCallback {

    public void stringCallback(String string);
    public void error(NetworkException e);

}
