package io.ourglass.bucanero.services.STB;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONObject;

import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.core.OGConstants;
import io.ourglass.bucanero.core.OGSystem;
import io.ourglass.bucanero.messages.MainThreadBus;
import io.ourglass.bucanero.objects.SetTopBox;
import io.ourglass.bucanero.services.Connectivity.ConnectivityCenter;
import io.ourglass.bucanero.services.STB.DirecTV.DirecTVAPI;
import io.ourglass.bucanero.services.STB.DirecTV.DirecTVSetTopBox;

/**
 * Created by atorres on 4/19/16.
 */

// TODO This service shouldn't event run when not paired. It should be started only after a pair
// event occurs, or upon bootup if an existing pair is saved.


public class STBPollingService extends Service {

    static final String TAG = "STBPollingService";
    static final boolean VERBOSE = true;
    static STBPollingService sInstance;

    private MainThreadBus bus = ABApplication.ottobus;

    public static STBPollingService getInstance() {
        return sInstance;
    }

    // TODO: Count failed polls and take a reconnect action if the number of failed polls exceeds a
    // threshold. Several things could cause a disconnect: IP address changed for box, WiFi is down,
    // etc.

    public static STBPollStatus lastPollStatus;

    HandlerThread stbLooperThread = new HandlerThread("stbPollLooper");
    private Handler mPollThreadHandler;

    private void logd(String message) {
        if (VERBOSE) {
            Log.d(TAG, message);
        }
    }

    public void checkHardSTBConnection() {

        Runnable checkHardRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Checking hard pair thread running");
                JSONObject stbJson = DirecTVAPI.stbInfo(OGConstants.ETHERNET_HARD_PAIR_IP_ADDRESS);
                if (stbJson != null || OGConstants.SIMULATE_HARD_PAIR) {
                    Log.d(TAG, "We are hard paired!");
                    //OGSystem.setPairedSTBIpAddress("10.21.200.2");
                    DirecTVSetTopBox newSTB = new DirecTVSetTopBox(null,
                            OGConstants.ETHERNET_HARD_PAIR_IP_ADDRESS,
                            SetTopBox.STBConnectionType.IPGENERIC,
                            null);
                    OGSystem.setPairedSTB(newSTB);
                    bus.post(newSTB);
                } else {
                    Log.d(TAG, "Hard pair check failed!");
                    if (OGSystem.isHardPaired()){
                        Log.d(TAG, "System thinks it is hard paired, but no response from STB, unpairing.");
                        OGSystem.unpairSTB();
                    }
                    Log.d(TAG, "Gonna twiddle the routing table a tiny bit, see if I can some action.");
                    ConnectivityCenter.ipRoute();

                }
            }
        };

        Thread cht = new Thread(checkHardRunnable);
        cht.start();

    }

    private void startPollLooper() {
        Log.d(TAG, "Starting STB poll looper");

        Runnable txRunnable = new Runnable() {
            @Override
            public void run() {

                checkHardSTBConnection(); // Always choose hard pair
                logd("STB Update Loop");
                SetTopBox stb = OGSystem.getPairedSTB();
                if (stb == null) {
                    Log.d(TAG, "Not paired to STB, skipping update");
                } else {
                    Log.d(TAG, "Paired to: " + stb.ipAddress + ", updating now.");
                    lastPollStatus = stb.updateAllSync();
                    OGSystem.setCurrentTVShow(stb.nowPlaying);
                }

                mPollThreadHandler.postDelayed(this, OGConstants.TV_POLL_INTERVAL);
            }
        };

        mPollThreadHandler.post(txRunnable);

    }

    private void stopPoll() {
        Log.d(TAG, "Stopping TV polling.");
        mPollThreadHandler.removeCallbacksAndMessages(null);
    }


    @Override
    public void onCreate() {

        Log.d(TAG, "onCreate");
        sInstance = this;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        ABApplication.dbToast("Starting STB Polling");

        checkHardSTBConnection();

        if (!stbLooperThread.isAlive()) {
            stbLooperThread.start();
            mPollThreadHandler = new Handler(stbLooperThread.getLooper());
        }

        startPollLooper();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopPoll();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
