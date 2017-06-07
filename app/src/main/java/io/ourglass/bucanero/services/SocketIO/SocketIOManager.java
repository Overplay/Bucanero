package io.ourglass.bucanero.services.SocketIO;

import android.util.Log;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

import io.ourglass.bucanero.api.BelliniDMAPI;
import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.core.OGSystem;
import io.ourglass.bucanero.messages.AppLaunchAckMessage;
import io.ourglass.bucanero.messages.BadOttoMessageException;
import io.ourglass.bucanero.messages.KillAppMessage;
import io.ourglass.bucanero.messages.LaunchAppMessage;
import io.ourglass.bucanero.messages.MoveAppMessage;
import io.ourglass.bucanero.messages.SystemCommandMessage;
import io.ourglass.bucanero.messages.TVControlMessage;
import io.ourglass.bucanero.messages.VenuePairCompleteMessage;
import io.ourglass.bucanero.objects.TVShow;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Created by atorres on 3/6/17.
 */

// TODO Auto-reconnect happens, it just takes too long. This is probably an IO.Options setting

public class SocketIOManager {

    private static final String TAG = "SocketIOManager";
    private static SocketIOManager instance;
    private static Bus bus = ABApplication.ottobus;

    HashMap<Long, Long> mDedupMap = new HashMap<>();

    private static boolean hasConnected = false;

    private Thread mKeepAliveThread;

    public static SocketIOManager getInstance(String cookie) {
        if (instance == null) {
            instance = new SocketIOManager(cookie);
        }
        return instance;
    }


    private SocketIOManager(String cookie) {

        try {
            SailsSocketIO.getSailsSocket(cookie);
            registerOnConnectListener();
            registerDisconnectListener();
            registerJoinListener();
            registerDeviceDMListener();
            attachToBellini();
        } catch (URISyntaxException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }

        // Register to receive messages
        ABApplication.ottobus.register(this);
    }


    private void attachToBellini() {
        Log.d(TAG, "Attaching to Bellini-DM");
        // Successful completion calls down thru the chain.
        SailsSocketIO.socket.connect();
    }

    private void registerJoinListener() {

        Log.d(TAG, "Registering SIO Join Room listener");
        SailsSocketIO.socket.on("DEVICE-JOIN", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Device room join complete!");
            }
        });

    }


    private void registerOnConnectListener() {
        Log.d(TAG, "Registering SIO onConnect listener");
        SailsSocketIO.socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
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
        SailsSocketIO.socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.wtf(TAG, "Socket DISConnected!");
                //mKeepAliveThread.interrupt();
                //setupSocketIO();
            }
        });
    }

    private void registerDeviceDMListener() {
        Log.d(TAG, "Registering inbound device DM listener.");
        SailsSocketIO.socket.on("DEVICE-DM", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Received DEV-DM message.");
                processMessage(args[0]);
            }
        });
    }

    private void keepAlive() {

        SailsSocketIO.socket.on("CHECK-GOOD", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Received a CHECK-GOOD");
            }
        });

        mKeepAliveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean looping = true;

                while (looping) {
                    Log.d(TAG, "Doing connect check");
                    String url = "/ogdevice/checkconnection";
                    HashMap<String, String> params = new HashMap<String, String>();
                    params.put("deviceUDID", OGSystem.getUDID() );

                    SailsSocketIO.post(url, params, new SailsSocketIO.SailsSIOCallback() {
                        @Override
                        public void sioCallback(Object... args) {
                            Log.d(TAG, "Check connection POST returned");
                        }
                    });

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

        HashMap<String, String> params = new HashMap<>();
        params.put("deviceUDID", OGSystem.getUDID());
        SailsSocketIO.post("/ogdevice/joinroom?deviceUDID=" + OGSystem.getUDID(), params, new SailsSocketIO.SailsSIOCallback() {
            @Override
            public void sioCallback(Object... args) {
                hasConnected = true;
            }
        });


    }


    private void processMessage(Object o) {
        JSONObject robj = (JSONObject) o;
        Log.d(TAG, "Received inbound device DM: " + robj.toString());

        String action = robj.optString("action", "noop");
        Log.d(TAG, "Inbound command: " + action);

        Long ts = robj.optLong("ts", 0L);
        if (deDedup(ts)) {

            Log.d(TAG, "Inbound command accepted: " + action);

            switch (action) {
                case "ping":
                    Log.d(TAG, "Socket ping received");
                    sailsSIODeviceMessage(new PingAckMessage().toJson(), null);
                    break;

                case "identify":
                    Log.d(TAG, "Socket identify received");
                    sailsSIODeviceMessage(new IdentifyMessage().toJson(), null);
                    break;

                case "launch":
                    Log.d(TAG, "Socket app launch received");
                    launchApp(robj);
                    break;

                case "kill":
                    Log.d(TAG, "Socket app kill received");
                    killApp(robj);
                    break;

                case "move":
                    Log.d(TAG, "Socket app move received");
                    moveApp(robj);
                    break;

                case "tune":
                    Log.d(TAG, "Socket channel tune received");
                    changeChannel(robj);
                    break;

                case "cloud_record_update":
                    Log.d(TAG, "Socket cloud record update received");
                    OGSystem.updateFromOGCloud();
                    try {
                        JSONObject change = robj.getJSONObject("change");
                        if (change.has("atVenueUUID")) {
                            (new VenuePairCompleteMessage()).post();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    break;

                case "venue_pair_done":
                    Log.d(TAG, "Socket venue pair done received");
                    (new SystemCommandMessage(SystemCommandMessage.SystemCommand.VENUE_PAIR_DONE)).post();
                    break;

                default:
                    Log.d(TAG, "Socket did not recognize inbound action");
            }

        }


    }


    public void sailsSIODeviceMessage(JSONObject messageJson, final SailsSocketIO.SailsSIOCallback callback) {

        Log.d(TAG, "Sending sails SIO device message to clients");

        String url = "/ogdevice/message?deviceUDID=" + OGSystem.getUDID() + "&destination=clients";

        try {
            SailsSocketIO.post(url, messageJson, callback);

        } catch (JSONException e) {
            e.printStackTrace();
        }


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


    public void disconnect() {
        SailsSocketIO.socket.disconnect();
    }


    @Subscribe
    public void inboundLaunchAck(AppLaunchAckMessage msg) {
        // TODO: React to the event somehow!
        Log.d(TAG, "Got a launch ack message, yo!");
        BelliniDMAPI.appLaunchAck(msg.appId, msg.layoutSlot);
    }

    @Subscribe
    public void programChange(TVShow newShow) {
        Log.d(TAG, "Got a program change bus message!");

    }


    // Hail Mary
    private boolean deDedup(Long timeStamp) {

        Long now = System.currentTimeMillis();
        Long stale = now - 10000; // 10 seconds ago
        ArrayList<Long> staleEntries = new ArrayList<>();

        for (Long ts : mDedupMap.keySet()) {
            if (mDedupMap.get(ts) < stale) {
                Log.d(TAG, "Inbound stale command removed from: " + ts);
                staleEntries.add(ts);
            }
        }

        for (Long ts : staleEntries) {
            mDedupMap.remove(ts);
        }

        if (mDedupMap.get(timeStamp) == null) {
            mDedupMap.put(timeStamp, System.currentTimeMillis());
            return true;
        }

        Log.d(TAG, "Inbound: This is a dup, tossing");
        return false;
    }


}

