package io.ourglass.bucanero.services.SSDP;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.messages.MainThreadBus;


/**
 * Created by mitch on 11/10/16.
 * Does ssdp / upnp discovery for settop boxes, etc.
 *
 *  USAGE:
 *
 *  This service can be either bound or "intent started". The bound mechanism is not well tested!!
 *
 *  To use with intents, you will need a BroadcastListener to catch the found devices.
 *
 *      SSDPBroadcastReceiver ssdpBR = new SSDPBroadcastReceiver(new SSDPBroadcastReceiver.SSDPBroadcastReceiverListener() {
 *           @Override
 *               public void receivedSSDPUpdate(Intent intent) {
 *                  Log.d(TAG, "Got an SSDP update!");
 *                  HashMap<String, String> devices = intent.getSerializableExtra("devices");
 *                 }
 *          });
 *
 *      IntentFilter filter = new IntentFilter("tv.ourglass.amstelbrightserver.ssdpresponse");
 *      registerReceiver(ssdpBR, filter);
 *
 *
 *  Once you have such a receiver set up, you can issue a startService command like so:
 *
 *      Intent ssdpi = new Intent(this, OGDiscoService.class);
 *      ssdpi.putExtra("deviceFilter", "DIRECTV");
 *      startService(ssdpi);
 *
 *  The "deviceFilter" is optional and, if ommitted, all devices will be returned.
 *  You can call startService() even if it is already running. No problem.
 *
 */

public class SSDPService extends Service implements SSDPHandlerThread.SSDPListener {

    public static final String TAG = "OGDiscoService";
    public static final long CONSIDERED_FRESH = 1 * 1000; // 1 seconds

    private SSDPHandlerThread mSSDPDicoveryThread;

    public HashMap<String, String> mAllDevices = new HashMap<>();
    public HashSet<String> mAllAddresses = new HashSet<>();

    private String mDeviceFilter = null;

    private long mLastDiscovery;

    private MainThreadBus bus = ABApplication.ottobus;

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

        ABApplication.dbToast(this, "Starting SSDP Enumerator");

        // optional flag to not do a discovery immediately
        processIntent(intent);

        // TODO this should not be start sticky
        return Service.START_STICKY;

    }

    private void processIntent(Intent intent){

        mDeviceFilter = intent.getStringExtra("deviceFilter");
        discover();

    }

    // Triggers a discovery pass
    public void discover(){

        prepThread();

        long deltaT = System.currentTimeMillis() - mLastDiscovery;
        if ( deltaT > CONSIDERED_FRESH ) {
            mLastDiscovery = System.currentTimeMillis();
            mSSDPDicoveryThread.discover();
        } else
        {
            // we got reasonably fresh data, just spit it back
            notifyNewDevices();
        }

    }

    private void prepThread(){

        if (mSSDPDicoveryThread==null){
            mSSDPDicoveryThread = new SSDPHandlerThread("ssdpdicso");
            mSSDPDicoveryThread.start(this, this);
        }
    }


    public HashSet<String> getFilteredAddresses(String filterTerm){

        if (filterTerm==null)
            return mAllAddresses;

        HashSet<String> filteredAddresses = new HashSet<>();

        for(Map.Entry<String, String> device : mAllDevices.entrySet()){
            if ( device.getValue().contains(filterTerm)){
                filteredAddresses.add(device.getKey());
            }
        }

        return filteredAddresses;

    }

    public HashMap<String, String> getFilteredDevices(String filterTerm){

        if (filterTerm==null)
            return mAllDevices;

        HashMap<String, String> filteredDevices = new HashMap<>();

        for(Map.Entry<String, String> device : mAllDevices.entrySet()){
            if ( device.getValue().contains(filterTerm)){
                filteredDevices.put(device.getKey(), device.getValue());
            }
        }

        return filteredDevices;

    }

    public void onDestroy() {

        Log.d(TAG, "onDestroy");

        if (mSSDPDicoveryThread!=null)
            mSSDPDicoveryThread.quit();

        super.onDestroy();

    }

    private void notifyNewDevices(){

        SSDPResult result = new SSDPResult();
        result.addresses = getFilteredAddresses(mDeviceFilter);
        result.devices = getFilteredDevices(mDeviceFilter);
        result.filtered = true;
        bus.post(result);

//        Intent intent = new Intent();
//        intent.setAction("tv.ourglass.amstelbrightserver.ssdpresponse");
//        intent.putExtra("devices", getFilteredDevices(mDeviceFilter));
//        intent.putExtra("addresses", getFilteredAddresses(mDeviceFilter));
//        ABApplication.sharedContext.sendBroadcast(intent);

    }

    private void notifyError(String message){

        Intent intent = new Intent();
        intent.setAction("tv.ourglass.amstelbrightserver.ssdperror");
        intent.putExtra("error", message);
        ABApplication.sharedContext.sendBroadcast(intent);

    }

    // Discovery thread interface
    @Override
    public void foundDevices(HashMap<String, String> devices, HashSet<String> addresses) {
        mAllDevices = devices;
        mAllAddresses = addresses;
        notifyNewDevices();
    }

    @Override
    public void encounteredError(String errString) {
        Log.e(TAG, "Error enumerating SSDP: "+errString);
        notifyError(errString);
    }


}

