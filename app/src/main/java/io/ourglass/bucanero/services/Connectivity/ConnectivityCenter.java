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

import org.json.JSONObject;

import io.ourglass.bucanero.api.BelliniDMAPI;
import io.ourglass.bucanero.api.JSONCallback;
import io.ourglass.bucanero.api.StringCallback;
import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.core.OGSystem;
import io.ourglass.bucanero.messages.MainThreadBus;
import io.ourglass.bucanero.messages.SystemStatusMessage;
import io.ourglass.bucanero.objects.NetworkException;
import io.ourglass.bucanero.services.SocketIO.SocketIOManager;

import static android.content.Context.WIFI_SERVICE;

/**
 *
 * Monitors the WiFi and Ethernet Connectivity for the system
 *
 */

public class ConnectivityCenter  {

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
    public static boolean isCloudDMWebsocketOpen;

    private MainThreadBus bus = ABApplication.ottobus;

    private ConnectivityManager cm;
    private NetworkInfo mWifiNetworkInfo;
    private NetworkInfo mEthernetNetworkInfo;
    private Context mContext = ABApplication.sharedContext;

    private Handler mHandler = new Handler();;

    private static ConnectivityCenter mInstance;

    public static ConnectivityCenter getInstance(){

        if (mInstance==null){
            mInstance = new ConnectivityCenter();
        }

        return mInstance;

    }

    private ConnectivityCenter(){
        init();
    }

    private void init() {

        cm = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiNetworkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        mEthernetNetworkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);

        mContext.registerReceiver(
                new ConnectivityChangeBroadcastReceiver(),
                new IntentFilter(
                        ConnectivityManager.CONNECTIVITY_ACTION)
        );

        refreshNetworkState();

    }

    public String getWiFiIPAddress(){
        WifiManager wifiMgr = (WifiManager)ABApplication.sharedContext.getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        wifiIPAddress = Formatter.formatIpAddress(ip);
        return wifiIPAddress;
    }


    public void refreshNetworkState(){
        try {
            boolean isWiFiConnected = mWifiNetworkInfo.isConnectedOrConnecting();
            boolean isEthConnected = mEthernetNetworkInfo.isConnectedOrConnecting();
            getWiFiIPAddress();
        } catch (Exception e){
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

    // Connection to cloud-dm

    public void initializeCloudComms(final StringCallback callback){

        Log.d(TAG, "Initializing cloud communications. Authorizing.");

        BelliniDMAPI.authenticate("admin@test.com", "beerchugs", new StringCallback() {
            @Override
            public void stringCallback(final String cookie) {

                Log.d(TAG, "Was able to authenticate with Bellini-DM");
                isCloudDMReachable = true;
                cloudDMCookie = cookie;
                // promote to main thread
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {

                        if (sioManager==null){
                            Log.d(TAG, "Starting SocketIO after successful login");
                            sioManager = new SocketIOManager(cookie);
                        } else {
                            sioManager.reset(cookie);
                        }

                        registerDeviceWithCloud(new JSONCallback() {
                            @Override
                            public void jsonCallback(JSONObject jsonData) {
                                Log.d(TAG, "Was able to register this OG device with cloud.");
                                if (callback!=null){
                                    callback.stringCallback("Network bringup complete");
                                }
                            }

                            @Override
                            public void error(NetworkException e) {
                                Log.e(TAG, "Was NOT able to register this OG device with the cloud.");
                                if (callback!=null){
                                    callback.error(e);
                                }
                            }
                        });
                    }
                });

                new SystemStatusMessage(SystemStatusMessage.SystemStatus.NETWORK_CONNECTED).post();
            }

            @Override
            public void error(NetworkException e) {

                Log.e(TAG, "Was NOT able to authenticate with Bellini-DM");
                new SystemStatusMessage(SystemStatusMessage.SystemStatus.NETWORK_LOS).post();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "~~~ ATTEMPTING TO RESTART CLOUD COMMS ~~~");
                        initializeCloudComms(null);
                    }
                }, 60*1000);
            }
        });

    }

    public void registerDeviceWithCloud(final JSONCallback callback){

        // 1. Run a registration pass. If this box is already registered, we get back the last settings
        // saved in the cloud which we sync into this box.
        BelliniDMAPI.registerDeviceWithBellini(new JSONCallback() {
            @Override
            public void jsonCallback(JSONObject jsonData) {

                Log.d(TAG, "Registered device with Bellini-DM");
                if (OGSystem.getVenueId().isEmpty()){
                    BelliniDMAPI.associateDeviceWithVenueUUID(BelliniDMAPI.BULLPEN_VUUID, callback);
                } else if (callback!=null){
                    callback.jsonCallback(jsonData);
                }

            }

            @Override
            public void error(NetworkException e) {
                Log.e(TAG, "Could NOT register device with Bellini-DM");
                new SystemStatusMessage(SystemStatusMessage.SystemStatus.NETWORK_LOS).post();
               if (callback!=null){
                   callback.error(e);
               }
            }
        });


    }

}

