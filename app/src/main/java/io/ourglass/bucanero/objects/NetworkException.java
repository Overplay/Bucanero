package io.ourglass.bucanero.objects;

/**
 * Created by mkahn on 4/19/17.
 */

public class NetworkException extends Exception {

    public int statusCode;

    public NetworkException(String message, int statusCode){
        super(message);
        this.statusCode = statusCode;
    }

    public NetworkException(String message){
        super(message);
        this.statusCode = 0;
    }


}
