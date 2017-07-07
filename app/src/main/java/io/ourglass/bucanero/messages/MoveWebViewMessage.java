package io.ourglass.bucanero.messages;

/**
 * USED TO MOVE A WebView regardless of app to an explicit slot
 * normally used for channel change moves
 */

public class MoveWebViewMessage extends OttobusMainThreadMessage {

    public String type = "crawler";  // or widget
    public int slot = 0;

    public MoveWebViewMessage(String type, int slot) {

        this.slot = slot;
        this.type = type;

    }

}
