package io.ourglass.bucanero.services.SocketIO;

import android.util.Log;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.ourglass.bucanero.api.BelliniDMAPI;
import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.core.OGConstants;
import io.ourglass.bucanero.core.OGSystem;
import io.ourglass.bucanero.messages.AppLaunchAckMessage;
import io.ourglass.bucanero.messages.BadOttoMessageException;
import io.ourglass.bucanero.messages.KillAppMessage;
import io.ourglass.bucanero.messages.LaunchAppMessage;
import io.ourglass.bucanero.messages.MoveAppMessage;
import io.ourglass.bucanero.messages.TVControlMessage;
import io.ourglass.bucanero.objects.TVShow;
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
    private static Bus bus = ABApplication.ottobus;

    private Thread mKeepAliveThread;

    public interface SailsSIOCallback {
        public void sioCallback(Object... args);
    }

    private SocketIOManager() {
        setupSocketIO();
        // Register to receive messages
        ABApplication.ottobus.register(this);
    }

    private void setupSocketIO() {

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

    private void attachToBellini() {
        Log.d(TAG, "Attaching to Bellini-DM");
        // Successful completion calls down thru the chain.
        mSocket.connect();
    }

    private void registerOnConnectListener() {

        Log.d(TAG, "Registering SIO onConnect listener");
        mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Socket Connected!");
                joinDeviceRoom();
                keepAlive();
            }
        });

    }

    private void registerDisconnectListener() {

        Log.d(TAG, "Registering SIO onDisconnectConnect listener");
        mSocket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.wtf(TAG, "Socket DISConnected!");
                mKeepAliveThread.interrupt();
                setupSocketIO();
            }
        });

    }

    private void keepAlive() {

        mKeepAliveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean looping = true;
                while (looping) {
                    Log.d(TAG, "Emitting keep alive beep");
                    mSocket.emit("beep", "beep");
                    try {
                        Thread.sleep(15000);
                    } catch (InterruptedException e) {
                        looping = false;
                    }
                }
            }
        });

        mKeepAliveThread.start();
    }

    private void joinDeviceRoom() {

        Log.d(TAG, "Joining SIO room for this device");
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("url", "/ogdevice/joinroom?deviceUDID=" + OGSystem.getUDID());
            jsonObject.put("deviceUDID", OGSystem.getUDID());
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

    private void registerDeviceDMListener() {
        Log.d(TAG, "Room joined, registering DM listener.");
        mSocket.on("DEVICE-DM", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Received DEV-DM message.");
                processMessage(args[0]);
            }
        });
    }

    private void processMessage(Object o) {
        JSONObject robj = (JSONObject) o;
        Log.d(TAG, "Received inbound device DM: " + robj.toString());

        String action = robj.optString("action", "noop");
        switch (action) {
            case "ping":
                sailsSIODeviceMessage(new PingAckMessage().toJson(), null);
                break;

            case "identify":
                sailsSIODeviceMessage(new IdentifyMessage().toJson(), null);
                break;

            case "launch":
                launchApp(robj);
                break;

            case "kill":
                killApp(robj);
                break;

            case "move":
                moveApp(robj);
                break;

            case "tune":
                changeChannel(robj);
                break;

            case "cloud_record_update":
                OGSystem.updateFromOGCloud();
                break;

            default:
                Log.d(TAG, "Did not recognize inbound action");
        }
    }


    public void sailsSIODeviceMessage(JSONObject messageJson, final SailsSIOCallback callback) {
        Log.d(TAG, "Sending sails SIO device DM");
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("url", "/ogdevice/dm?deviceUDID=" + OGSystem.getUDID());
            jsonObject.put("data", messageJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mSocket.emit("post", jsonObject, new Ack() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "DM got response: " + args);
                if (callback != null) {
                    callback.sioCallback(args);
                }
            }
        });

    }

    private void launchApp(JSONObject launchObj) {
        try {
            bus.post(new LaunchAppMessage(launchObj));
        } catch (BadOttoMessageException e) {
            Log.e(TAG, "Malformed launch command, ignoring");
        }
    }

    private void killApp(JSONObject killObj) {
        try {
            bus.post(new KillAppMessage(killObj));
        } catch (BadOttoMessageException e) {
            Log.e(TAG, "Malformed kill command, ignoring");
        }
    }

    private void moveApp(JSONObject moveObj) {
        try {
            bus.post(new MoveAppMessage(moveObj));
        } catch (BadOttoMessageException e) {
            Log.e(TAG, "Malformed move command, ignoring");
        }
    }

    private void changeChannel(JSONObject tuneObj) {
        TVControlMessage tvcm = new TVControlMessage(tuneObj);
        bus.post(tvcm); // in case anyone cares
        OGSystem.changeTVChannel(tvcm.toChannel);
    }

    public static SocketIOManager getInstance() {
        return instance;
    }

    public void disconnect() {
        mSocket.disconnect();
    }


    @Subscribe
    public void inboundLaunchAck(AppLaunchAckMessage msg) {
        // TODO: React to the event somehow!
        Log.d(TAG, "Got a launch ack message, yo!");
        BelliniDMAPI.appLaunchAck(msg.appId, msg.layoutSlot);
    }

    @Subscribe
    public void programChange(TVShow newShow){
        Log.d(TAG, "Got a program change bus message!");

    }

}

