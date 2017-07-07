package io.ourglass.bucanero.services.SocketIO;

import android.os.Handler;
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
import io.ourglass.bucanero.messages.MoveWebViewMessage;
import io.ourglass.bucanero.messages.OnScreenNotificationMessage;
import io.ourglass.bucanero.messages.SystemCommandMessage;
import io.ourglass.bucanero.messages.TVControlMessage;
import io.ourglass.bucanero.messages.VenuePairCompleteMessage;
import io.ourglass.bucanero.objects.TVShow;
import io.ourglass.bucanero.services.Connectivity.ConnectivityCenter;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static io.ourglass.bucanero.services.SocketIO.SailsSocketIO.socket;


// TODO Auto-reconnect happens, it just takes too long. This is probably an IO.Options setting

public class SocketIOManager {

    private static final String TAG = "SocketIOManager";
    private static final int KEEP_ALIVE_DELAY = 15000;
    private static final int WARN_YELLOW = KEEP_ALIVE_DELAY * 2; // 1 minute
    private static final int WARN_RED = WARN_YELLOW * 2; // 3 minutes

    private Bus bus = ABApplication.ottobus;

    private long lastLoopback = 0;
    private int losLevel = 0;  // 0 = minor, 1 = middle, 2 = oh shit!

    private String mCookie;

    HashMap<Long, Long> mDedupMap = new HashMap<>();

    private boolean hasConnected = false;

    private Handler mHandler = new Handler();

    public SocketIOManager(String cookie) {

        mCookie = cookie;
        initialize();

        // Register to receive messages
        ABApplication.ottobus.register(this);
    }

    // Reset is called when the cookie changes, usually when there's been a connect/disconnect issue
    public void reset(String newCookie){

        Log.d(TAG, "Resetting socket IO from the top.");
        mCookie = newCookie;
        initialize();

    }

    public void shutDownSockets(){

        Log.d(TAG, "Shutting down socket IO.");

        mHandler.removeCallbacksAndMessages(null);
        SailsSocketIO.close();
        mCookie = null;

    }

    private void initialize() {
        try {
            SailsSocketIO.getSailsSocket(mCookie);
            registerOnConnectListener();
            registerDisconnectListener();
            registerJoinListener();
            registerDeviceDMListener();
            attachToBellini();
        } catch (URISyntaxException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    private void attachToBellini() {
        Log.d(TAG, "Attaching to Bellini-DM");
        // Successful completion calls down thru the chain.
        socket.connect();
        lastLoopback = System.currentTimeMillis();
    }

    private void registerJoinListener() {

        Log.d(TAG, "Registering SIO Join Room listener");
        socket.on("DEVICE-JOIN", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Device room join complete!");
            }
        });

    }


    private void registerOnConnectListener() {
        Log.d(TAG, "Registering SIO onConnect listener");
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
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
        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
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
        socket.on("DEVICE-DM", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Received DEV-DM message.");
                processMessage(args[0]);
            }
        });

        socket.on("CHECK-GOOD", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Received a CHECK-GOOD");
                lastLoopback = System.currentTimeMillis();
            }
        });

    }

    private Runnable keepAliveRunnable = new Runnable() {
        @Override
        public void run() {

                long delta = System.currentTimeMillis() - lastLoopback;
                Log.d(TAG, "~~~ LOS Delta: "+delta+" ~~~");

                if ( delta > WARN_RED ){

                    Log.d(TAG, "~~~ LOS LEVEL: 2 ~~~~");
                    Log.d(TAG, "~~~ SHUTTING DOWN SOCKETS ~~~~");
                    shutDownSockets();

                    (new OnScreenNotificationMessage("We've lost the network! Trying to reconnect.")).post();

                    Log.d(TAG, "~~~ ISSUING RECONNECT ~~~~");
                    ConnectivityCenter.getInstance().initializeCloudComms(null);

                } else {

                    Log.d(TAG, "Doing connect check");
                    String url = "/ogdevice/checkconnection";
                    HashMap<String, String> params = new HashMap<String, String>();
                    params.put("deviceUDID", OGSystem.getUDID());

                    SailsSocketIO.post(url, params, new SailsSocketIO.SailsSIOCallback() {
                        @Override
                        public void sioCallback(Object... args) {
                            Log.d(TAG, "Check connection POST returned");
                        }
                    });

                    if ( delta < WARN_YELLOW ) {
                        Log.d(TAG, "~~~ LOS LEVEL: 0 ~~~~");
                        losLevel = 0;
                    } else {
                        Log.d(TAG, "~~~ LOS LEVEL: 1 ~~~~");
                        (new OnScreenNotificationMessage("We seem to have a slight network issue...")).post();
                        losLevel = 1;
                    }

                    mHandler.postDelayed(this, KEEP_ALIVE_DELAY);

                }

        }

    };

    private void keepAlive() {

        mHandler.post(keepAliveRunnable);

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

        // TODO: Hack Alert!!

        if ( tuneObj.optInt("channel", 0) == 620 ){
            Log.d(TAG, "BeIn Hack, moving to floor");
            (new MoveWebViewMessage("crawler", 1)).post();
        } else {
            (new MoveWebViewMessage("crawler", 0)).post();
        }
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

