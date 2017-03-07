package io.ourglass.bucanero.services.SocketIO;

import android.util.Log;

import java.net.URISyntaxException;

import io.ourglass.bucanero.core.OGConstants;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Created by atorres on 3/6/17.
 */

public class SocketIOManager {

    private static final String TAG = "SocketIOManager";

    private static SocketIOManager instance = new SocketIOManager();

    public static Socket mSocket;

    private SocketIOManager() {
        IO.Options opts = new IO.Options();
        //opts.forceNew = true;
        opts.query = "__sails_io_sdk_version=0.11.0";
        try {
            mSocket = IO.socket(OGConstants.SOCKET_IO_ADDRESS, opts);
        } catch (URISyntaxException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    public static SocketIOManager getInstance() {
        return instance;
    }

    public void connect() {
        mSocket.connect();
    }

    public void disconnect() {
        mSocket.disconnect();
    }

    /* example of sending data */
    public void sendMessage(String message, String nickname) {
        mSocket.emit("chatMessage", nickname, message);
    }

    /* example of listening for data */
    public void getMessage(Emitter.Listener listener) {
        mSocket.on("newChatMessage", listener);
    }
}

