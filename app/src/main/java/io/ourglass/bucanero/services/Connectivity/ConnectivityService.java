package io.ourglass.bucanero.services.Connectivity;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.format.Formatter;
import android.util.Log;

import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.messages.MainThreadBus;

/**
 *
 * Monitors the WiFi and Ethernet Connectivity for the system
 *
 */

public class ConnectivityService extends Service  {

    public static final String TAG = "ConnectivityService";

    private MainThreadBus bus = ABApplication.ottobus;

    private ConnectivityManager cm;
    private NetworkInfo mWifiNetworkInfo;
    private NetworkInfo mEthernetNetworkInfo;

    // Stock stuff that needs to be here for all services

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        cm = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiNetworkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        mEthernetNetworkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);

        registerReceiver(
                new ConnectivityChangeBroadcastReceiver(),
                new IntentFilter(
                        ConnectivityManager.CONNECTIVITY_ACTION));

        return Service.START_STICKY;

    }

    private void processIntent(Intent intent){
        boolean isWiFiConnected = mWifiNetworkInfo.isConnectedOrConnecting();
        boolean isEthConnected = mEthernetNetworkInfo.isConnectedOrConnecting();

    }


    public void onDestroy() {

        Log.d(TAG, "onDestroy");
        super.onDestroy();

    }

    private void checkConnectivityStatus(){


        WifiManager wifiMgr = (WifiManager)ABApplication.sharedContext.getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        String ipAddress = Formatter.formatIpAddress(ip);

    }

    private void connChange(Intent intent){

    }

    public class ConnectivityChangeBroadcastReceiver extends BroadcastReceiver {

        private static final String TAG = "CnxChangeRecv";

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Connectivity change detected");
            connChange(intent);
        }

    }


}

