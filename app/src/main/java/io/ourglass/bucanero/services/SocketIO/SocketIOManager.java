package io.ourglass.bucanero.services.SocketIO;

import android.os.Handler;
import android.util.Log;

//import com.crashlytics.android.answers.Answers;
//import com.crashlytics.android.answers.CustomEvent;
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
import io.ourglass.bucanero.messages.OGLogMessage;
import io.ourglass.bucanero.messages.OnScreenNotificationMessage;
import io.ourglass.bucanero.objects.TVShow;
import io.ourglass.bucanero.services.Connectivity.ConnectivityCenter;
import io.ourglass.bucanero.services.Connectivity.SIONetworkState;
import io.ourglass.bucanero.services.SocketIO.SIOActions.ChannelChangeAction;
import io.ourglass.bucanero.services.SocketIO.SIOActions.CloudUpdateAction;
import io.ourglass.bucanero.services.SocketIO.SIOActions.IdentifyAction;
import io.ourglass.bucanero.services.SocketIO.SIOActions.KillAppAction;
import io.ourglass.bucanero.services.SocketIO.SIOActions.LaunchAppAction;
import io.ourglass.bucanero.services.SocketIO.SIOActions.MoveAppAction;
import io.ourglass.bucanero.services.SocketIO.SIOActions.PingAction;
import io.ourglass.bucanero.services.SocketIO.SIOActions.SIOActionDispatcher;
import io.ourglass.bucanero.services.SocketIO.SIOActions.SystemSettingAction;
import io.ourglass.bucanero.services.SocketIO.SIOActions.VenuePairDoneAction;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static io.ourglass.bucanero.services.SocketIO.SailsSocketIO.socket;

/****
 *
 * Flow:
 *
 * 1) Constructor is passed a cookie that it gets via logging in (?)
 * 2) All action handlers are registered with the action dispatcher (mDispatcher)
 * 3) A "SailsSocket" is created which is configured the way Sails likes it. This is a static
 *    socket (singleton).
 * 4)
 *
 *
 *
 *
 *
 * Loopback Flow
 * -------------
 * The `keepAliveRunnable` runs every `KEEP_ALIVE_DELAY` which is set at 15 seconds. This runnable
 * POSTS to /ogdevice/checkconnection which responds with `CHECK-GOOD` on the device room.
 *
 */


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

    SIOActionDispatcher mDispatcher = new SIOActionDispatcher();

    private boolean hasConnected = false;

    private Handler mHandler = new Handler();

    public SocketIOManager(String cookie) {

        Log.d(TAG, "Creating SocketIOManager");
        mCookie = cookie;
        registerActions();
        initialize();

        // Register to receive messages
        ABApplication.ottobus.register(this);
    }

    // Reset is called when the cookie changes, usually when there's been a connect/disconnect issue
    public void reset(String newCookie){

        Log.d(TAG, "Resetting socket IO from the top.");
        mCookie = newCookie;
        initialize();
        OGLogMessage.newSIOStatusLog(SIONetworkState.SIONetState.SIO_RESET).post();

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
                OGLogMessage.newSIOStatusLog(SIONetworkState.SIONetState.SIO_CONNECTED).post();
                joinDeviceRoom();
//                Answers.getInstance().logCustom(new CustomEvent("SIO Event")
//                        .putCustomAttribute("detail", "Connected"));
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
                OGLogMessage.newSIOStatusLog(SIONetworkState.SIONetState.SIO_DISCONNECTED).post();
//                Answers.getInstance().logCustom(new CustomEvent("SIO Event")
//                        .putCustomAttribute("detail", "Disconnected"));
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

                    OGLogMessage.newSIOStatusLog(SIONetworkState.SIONetState.SIO_LOS_RED).post();

//                    Answers.getInstance().logCustom(new CustomEvent("SIO LoS")
//                            .putCustomAttribute("severity", 2));

                    Log.d(TAG, "~~~ LOS LEVEL: 2 ~~~~");
                    Log.d(TAG, "~~~ SHUTTING DOWN SOCKETS ~~~~");
                    shutDownSockets();

                    (new OnScreenNotificationMessage("We've lost the network! Trying to reconnect.")).post();

                    Log.d(TAG, "~~~ ISSUING RECONNECT ~~~~");
                    ConnectivityCenter.getInstance().initializeCloudComms();

                } else {

                    Log.d(TAG, "Doing connect check");
                    String url = "/ogdevice/checkconnection";
                    HashMap<String, String> params = new HashMap<String, String>();
                    params.put("deviceUDID", OGSystem.getUDID());

                    SailsSocketIO.post(url, params, new SailsSocketIO.SailsSIOCallback() {
                        @Override
                        public void sioCallback(Object... args) {
                            Log.d(TAG, "Check connection POST returned from Bellini");
                        }
                    });

                    if ( delta < WARN_YELLOW ) {
                        Log.d(TAG, "~~~ LOS LEVEL: 0 ~~~~");
                        losLevel = 0;
                    } else {
                        Log.d(TAG, "~~~ LOS LEVEL: 1 ~~~~");
                        (new OnScreenNotificationMessage("We seem to have a slight network issue...")).post();
                        losLevel = 1;
                        OGLogMessage.newSIOStatusLog(SIONetworkState.SIONetState.SIO_LOS_YELLOW).post();
//                        Answers.getInstance().logCustom(new CustomEvent("SIO LoS")
//                                .putCustomAttribute("severity", 1));

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

        Long ts = (Long) robj.optLong("ts", 0L);
        if (deDedup(ts)) {

            Log.d(TAG, "Inbound command accepted: " + action);
            boolean actionResult = mDispatcher.processAction(action, robj);
            Log.d(TAG, actionResult ? "Command processed." : "Command not recognized.");
//            Answers.getInstance().logCustom(new CustomEvent("SIO RX Action")
//                    .putCustomAttribute("action", action));

        }


    }


    public static void sailsSIODeviceMessage(JSONObject messageJson, final SailsSocketIO.SailsSIOCallback callback) {

        Log.d(TAG, "Sending sails SIO device message to clients");

        String url = "/ogdevice/message?deviceUDID=" + OGSystem.getUDID() + "&destination=clients";

        try {
            SailsSocketIO.post(url, messageJson, callback);

        } catch (JSONException e) {
            e.printStackTrace();
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

        Long now = (Long) System.currentTimeMillis();
        Long stale = (Long) (now - 10000); // 10 seconds ago
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
            mDedupMap.put(timeStamp, Long.valueOf(System.currentTimeMillis()));
            return true;
        }

        Log.d(TAG, "Inbound: This is a dup, tossing");
        return false;
    }

    private void registerActions(){

        mDispatcher.registerAction(new ChannelChangeAction());
        mDispatcher.registerAction(new CloudUpdateAction());
        mDispatcher.registerAction(new IdentifyAction());
        mDispatcher.registerAction(new KillAppAction());
        mDispatcher.registerAction(new LaunchAppAction());
        mDispatcher.registerAction(new MoveAppAction());
        mDispatcher.registerAction(new PingAction());
        mDispatcher.registerAction(new SystemSettingAction());
        mDispatcher.registerAction(new VenuePairDoneAction());


    }


}

