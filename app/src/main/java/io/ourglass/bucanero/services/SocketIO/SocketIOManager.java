package io.ourglass.bucanero.services.SocketIO;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.ourglass.bucanero.core.OGConstants;
import io.ourglass.bucanero.core.OGSystem;
import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Created by atorres on 3/6/17.
 */

// TODO Auto-reconnect happens, it just takes too long. This is probably an IO.Options setting

public class SocketIOManager {

    private static final String TAG = "SocketIOManager";
    private static SocketIOManager instance = new SocketIOManager();
    public static Socket mSocket;

    private SocketIOManager() {

        IO.Options opts = new IO.Options();

        opts.forceNew = true;
        // This is required because sails does an idiotic version check
        opts.query = "__sails_io_sdk_version=0.11.0";
        opts.reconnection = true;
        opts.reconnectionDelay = 1000; // TODO is this right?

        try {
            mSocket = IO.socket(OGConstants.SOCKET_IO_ADDRESS, opts);
            registerOnConnectListener();
            registerDisconnectListener();
            attachToBellini();
        } catch (URISyntaxException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }

    }

    private void attachToBellini(){
        Log.d(TAG, "Attaching to Bellini-DM");
        // Successful completion calls down thru the chain.
        mSocket.connect();
    }

    private void registerOnConnectListener(){

        Log.d(TAG, "Registering SIO onConnect listener");
        mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Socket Connected!");
                joinDeviceRoom();
            }
        });

    }

    private void registerDisconnectListener(){

        Log.d(TAG, "Registering SIO onDisconnectConnect listener");
        mSocket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.wtf(TAG, "Socket DISConnected!");
            }
        });

    }

    private void joinDeviceRoom(){

        Log.d(TAG, "Joining SIO room for this device");
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("url", "/ogdevice/joinroom?deviceUDID="+ OGSystem.getUDID());
            jsonObject.put("deviceUDID",OGSystem.getUDID() );
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mSocket.emit("post", jsonObject, new Ack() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "records: " + args[0].toString());
                registerDeviceDMListener();
            }
        });

    }

    private void registerDeviceDMListener(){
        Log.d(TAG, "Room joined, registering DM listener.");
        mSocket.on("DEV-DM", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Received DEV-DM message.");
                processMessage(args[0]);
            }
        });
    }

    private void processMessage(Object o){
        JSONObject robj = (JSONObject)o;
        Log.d(TAG, "Received inbound device DM: "+robj.toString());

    }

    public static SocketIOManager getInstance() {
        return instance;
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

