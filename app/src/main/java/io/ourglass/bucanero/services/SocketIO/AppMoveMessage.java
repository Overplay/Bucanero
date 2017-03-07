package io.ourglass.bucanero.services.SocketIO;

/**
 * Created by mkahn on 3/7/17.
 */

public class AppMoveMessage {

    public enum AppType { WIDGET, CRAWLER };
    public enum MoveType { SLOT, XNUDGE, YNUDGE };
    public AppType appType;
    public MoveType moveType;

    public AppMoveMessage(AppType appType, MoveType moveType){
        this.appType = appType;
        this.moveType = moveType;
    }

}
