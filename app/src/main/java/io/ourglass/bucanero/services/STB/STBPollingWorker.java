package io.ourglass.bucanero.services.STB;

import android.os.Handler;
import android.os.HandlerThread;
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
 * STBPollingWorker
 *
 * This is a rewrite of STBPollingService that does not use the Android Service funtionality (because
 * it is not needed and results in zombie services).
 *
 */


public class STBPollingWorker  {

    static final String TAG = "STBPollingWorker";

    private MainThreadBus bus = ABApplication.ottobus;

    // TODO: Count failed polls and take a reconnect action if the number of failed polls exceeds a
    // threshold. Several things could cause a disconnect: IP address changed for box, WiFi is down,
    // etc.

    public static STBPollStatus lastPollStatus;

    HandlerThread stbLooperThread = new HandlerThread("stbPollLooper");
    private Handler mPollThreadHandler;


    public void checkHardSTBConnection() {

        Runnable checkHardRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Checking hard pair thread running");
                JSONObject stbJson = DirecTVAPI.stbInfo(OGConstants.ETHERNET_HARD_PAIR_IP_ADDRESS);

                if (stbJson != null || OGConstants.SIMULATE_HARD_PAIR) {
                    Log.d(TAG, "We are hard paired!");
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

    public void stop() {
        Log.d(TAG, "Stopping TV polling.");
        mPollThreadHandler.removeCallbacksAndMessages(null);
        stbLooperThread.quitSafely();
    }


    public void start() {

        ABApplication.dbToast("Starting STB Polling");

        checkHardSTBConnection();

        if (!stbLooperThread.isAlive()) {
            stbLooperThread.start();
            mPollThreadHandler = new Handler(stbLooperThread.getLooper());
        }

        startPollLooper();

    }


}
