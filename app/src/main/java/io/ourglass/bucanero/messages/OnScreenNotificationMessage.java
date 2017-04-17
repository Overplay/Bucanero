package io.ourglass.bucanero.messages;

/**
 * Created by mkahn on 4/17/17.
 */


// TODO Add foreground and background colors

public class OnScreenNotificationMessage extends OttobusMainThreadMessage {
    public String message;
    public String subMessage;

    public OnScreenNotificationMessage(String message){
        this.message = message;
        this.subMessage = null;
    }

    public OnScreenNotificationMessage(String message, String subMessage){
        this.message = message;
        this.subMessage = subMessage;
    }

}
