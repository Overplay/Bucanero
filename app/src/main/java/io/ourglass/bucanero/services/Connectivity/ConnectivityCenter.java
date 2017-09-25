package io.ourglass.bucanero.services.Connectivity;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;

import org.jdeferred.DoneCallback;
import org.jdeferred.DonePipe;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.json.JSONObject;

import java.util.ArrayList;

import io.ourglass.bucanero.Support.OGShellE;
import io.ourglass.bucanero.api.BelliniDMAPI;
import io.ourglass.bucanero.api.JSONCallback;
import io.ourglass.bucanero.api.OGHeaderInterceptor;
import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.core.OGSettings;
import io.ourglass.bucanero.core.OGSystem;
import io.ourglass.bucanero.messages.MainThreadBus;
import io.ourglass.bucanero.services.SocketIO.SocketIOManager;

import static android.content.Context.WIFI_SERVICE;

/**
 * Monitors the WiFi and Ethernet Connectivity for the system
 */


//TODO look @ ConnectivityManager.NetworkCallback instead of Broadcasts

public class ConnectivityCenter {

    public static final String TAG = "ConnectivityCenter";

    public static SocketIOManager sioManager;

    // There are the equivalent of a "link light", with no indication of whether we're talking
    // to the back-end systems.
    public static boolean isWifiPhysicallyConnected = false;
    public static boolean isEthernetPhysicallyConnected = false;

    // IPAddresses
    public static String wifiIPAddress;
    public static String ethernetIPAddress;

    // Cloud Stuff
    public static String cloudDMCookie;
    public static boolean isCloudDMReachable;
    public static boolean isDeviceAuthenticated;

    public static boolean isCloudDMWebsocketOpen;

    private MainThreadBus bus = ABApplication.ottobus;

    private ConnectivityManager cm;
    private NetworkInfo mWifiNetworkInfo;
    private NetworkInfo mEthernetNetworkInfo;
    private Context mContext = ABApplication.sharedContext;

    private Handler mHandler = new Handler();
    ;

    private static ConnectivityCenter mInstance;

    public static ConnectivityCenter getInstance() {

        if (mInstance == null) {
            mInstance = new ConnectivityCenter();
        }

        return mInstance;

    }

    private ConnectivityCenter() {
        init();
    }

    private void init() {

        // Magic and insecure device authorization header
        //This is done in HeaderINtercepter now
        //DeferredRequest.addSharedHeader("x-dev-authorization", "x-ogdevice-1234");

        cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiNetworkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        mEthernetNetworkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);

        mContext.registerReceiver(
                new ConnectivityChangeBroadcastReceiver(),
                new IntentFilter(
                        ConnectivityManager.CONNECTIVITY_ACTION)
        );

        refreshNetworkState();

    }

    public static void ipRoute(){

        OGShellE.execRoot("ip route add table wlan0 10.21.200.0/24 via 10.21.200.1 dev eth0", new OGShellE.OGShellEListener() {
            @Override
            public void stdout(ArrayList<String> results) {
                Log.d(TAG, "ip route table add completed without fail. We should be good to go, homie.");
            }

            @Override
            public void stderr(ArrayList<String> errors) {
                Log.e(TAG, "Turds in stderr after ip route add.");
                Log.e(TAG, errors.toString());
                Log.e(TAG, "That's a wrap. It might work, maybe not.");
            }

            @Override
            public void fail(Exception e) {
                Log.wtf(TAG, "Failed out at ip route add. Oyvey.");
            }
        });
    }

    public String getWiFiIPAddress() {
        WifiManager wifiMgr = (WifiManager) ABApplication.sharedContext.getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        wifiIPAddress = Formatter.formatIpAddress(ip);
        return wifiIPAddress;
    }


    public void refreshNetworkState() {
        try {
            boolean isWiFiConnected = mWifiNetworkInfo.isConnectedOrConnecting();
            boolean isEthConnected = mEthernetNetworkInfo.isConnectedOrConnecting();
            getWiFiIPAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public class ConnectivityChangeBroadcastReceiver extends BroadcastReceiver {

        private static final String TAG = "CnxCenter[CnxChangeRx]";

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Connectivity change detected");
            refreshNetworkState();
        }

    }

    public void initializeCloudComms() {

        // Reset state
        isCloudDMReachable = false;
        isDeviceAuthenticated = false;
        cloudDMCookie = null;

        Log.d(TAG, "Using the following Bellini-DM instance: "+ OGSettings.getBelliniDMAddress());
        Log.d(TAG, "Initializing cloud communications. Regsitering.");
        BelliniDMAPI.registerDeviceWithBellini()
                .then(new DonePipe<JSONObject, String, Exception, Void>() {
                    @Override
                    public Promise<String, Exception, Void> pipeDone(JSONObject result) {
                        isCloudDMReachable = true;
                        OGSystem.updateSystemFromCloudObject(result);

                        Log.d(TAG, "Authenticating.");
                        return BelliniDMAPI.authenticateDevice();
                    }
                })
                .done(new DoneCallback<String>() {
                    @Override
                    public void onDone(String result) {
                        Log.d(TAG, "Registered and logged in.");
                        isDeviceAuthenticated = true;
                        cloudDMCookie = result;
                        OGHeaderInterceptor.sessionCookie = result;
                        // promote to main thread
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {

                                if (sioManager == null) {
                                    Log.d(TAG, "Starting SocketIO after successful login");
                                    sioManager = new SocketIOManager(cloudDMCookie);
                                } else {
                                    Log.d(TAG, "Resetting SocketIO after successful login");
                                    sioManager.reset(cloudDMCookie);
                                }
                            }
                        });
                    }
                })
                .fail(new FailCallback<Exception>() {
                    @Override
                    public void onFail(Exception result) {
                        Log.e(TAG, "Cloud comms initialization FAIL. Retrying in 30 seconds!");
                        //TODO: need a timed retry here
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "Retrying failed Cloud comms initialization!");
                                initializeCloudComms();
                            }
                        }, 30000);
                    }
                });

    }

//    // Connection to cloud-dm
//
//    public void initializeCloudCommsOld(final StringCallback callback) {
//
//        Log.d(TAG, "Initializing cloud communications. Authorizing.");
//
//        BelliniDMAPI.authenticate("admin@test.com", "beerchugs", new StringCallback() {
//            @Override
//            public void stringCallback(final String cookie) {
//
//                Log.d(TAG, "Was able to authenticate with Bellini-DM");
//                isCloudDMReachable = true;
//                cloudDMCookie = cookie;
//                // promote to main thread
//                mHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//
//                        if (sioManager == null) {
//                            Log.d(TAG, "Starting SocketIO after successful login");
//                            sioManager = new SocketIOManager(cookie);
//                        } else {
//                            sioManager.reset(cookie);
//                        }
//
//                        registerDeviceWithCloud(new JSONCallback() {
//                            @Override
//                            public void jsonCallback(JSONObject jsonData) {
//                                Log.d(TAG, "Was able to register this OG device with cloud.");
//                                if (callback != null) {
//                                    callback.stringCallback("Network bringup complete");
//                                }
//                            }
//
//                            @Override
//                            public void error(NetworkException e) {
//                                Log.e(TAG, "Was NOT able to register this OG device with the cloud.");
//                                if (callback != null) {
//                                    callback.error(e);
//                                }
//                            }
//                        });
//                    }
//                });
//
//                new SystemStatusMessage(SystemStatusMessage.SystemStatus.NETWORK_CONNECTED).post();
//            }
//
//            @Override
//            public void error(NetworkException e) {
//
//                Log.e(TAG, "Was NOT able to authenticate with Bellini-DM");
//                new SystemStatusMessage(SystemStatusMessage.SystemStatus.NETWORK_LOS).post();
//                mHandler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        Log.d(TAG, "~~~ ATTEMPTING TO RESTART CLOUD COMMS ~~~");
//                        initializeCloudComms();
//                    }
//                }, 60 * 1000);
//            }
//        });
//
//    }

    public void registerDeviceWithCloud(final JSONCallback callback) {

        // 1. Run a registration pass. If this box is already registered, we get back the last settings
        // saved in the cloud which we sync into this box.
//        BelliniDMAPI.registerDeviceWithBellini(new JSONCallback() {
//            @Override
//            public void jsonCallback(JSONObject jsonData) {
//
//                Log.d(TAG, "Registered device with Bellini-DM");
//                if (OGSystem.getVenueUUID().isEmpty()) {
//                    BelliniDMAPI.associateDeviceWithVenueUUID(BelliniDMAPI.BULLPEN_VUUID, callback);
//                } else if (callback != null) {
//                    callback.jsonCallback(jsonData);
//                }
//
//            }
//
//            @Override
//            public void error(NetworkException e) {
//                Log.e(TAG, "Could NOT register device with Bellini-DM");
//                new SystemStatusMessage(SystemStatusMessage.SystemStatus.NETWORK_LOS).post();
//                if (callback != null) {
//                    callback.error(e);
//                }
//            }
//        });


    }

    public void cloudUrlChange() {
        Log.d(TAG, "Cloud URL changed, reinitializing cloud comms");
        //TODO do something with this....
    }

}

