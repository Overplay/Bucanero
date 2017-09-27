package io.ourglass.bucanero.services.SSDP;

import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.messages.MainThreadBus;

/**
 * HISTORICAL:
 *
 * SSDP used to be an intent based service, but this is really unecessary and zombie services
 * were left running. SSDP is a one-and-done function used only by this app, so no need to use an
 * Android Service.
 * 
 */


/**
 * Based on the original Android Service, now deprecated
 * Created by mitch on 11/10/16.
 * Does ssdp / upnp discovery for settop boxes, etc.
 *
 *
 *  The "deviceFilter" is optional and, if ommitted, all devices will be returned.
 *
 *  Usage:
 *  1. Add an ottobus listener for SSDPResults
 *  2. (new SSDPWorker()).discover("DIRECTV")
 *  3. Results/Errors returned via Ottobus
 *
 */

public class SSDPWorker implements SSDPHandlerThread.SSDPListener {

    public static final String TAG = "OGDiscoWorker";
    public static final long CONSIDERED_FRESH = 1 * 1000; // 1 seconds

    private SSDPHandlerThread mSSDPDicoveryThread;

    public HashMap<String, String> mAllDevices = new HashMap<>();
    public HashSet<String> mAllAddresses = new HashSet<>();

    public String deviceFilter = null;

    private long mLastDiscovery;

    private MainThreadBus bus = ABApplication.ottobus;

    // Triggers a discovery pass
    public void discover(){

        ABApplication.dbToast("Searching for Set Top Boxes");

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

    public void discover(String deviceFilter){
        this.deviceFilter = deviceFilter;
        discover();
    }

    private void prepThread(){
        if (mSSDPDicoveryThread==null){
            mSSDPDicoveryThread = new SSDPHandlerThread("ssdpdicso");
            mSSDPDicoveryThread.start(ABApplication.sharedContext, this);
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

    private void notifyNewDevices(){

        SSDPResult result = new SSDPResult();
        result.addresses = getFilteredAddresses(deviceFilter);
        result.devices = getFilteredDevices(deviceFilter);
        result.filtered = true;
        bus.post(result);

    }

    private void notifyError(String message){

//        Intent intent = new Intent();
//        intent.setAction("tv.ourglass.amstelbrightserver.ssdperror");
//        intent.putExtra("error", message);
//        ABApplication.sharedContext.sendBroadcast(intent);

        SSDPResult result = new SSDPResult();
        result.errorThrown = true;
        result.errorMessage = message;
        bus.post(result);

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

