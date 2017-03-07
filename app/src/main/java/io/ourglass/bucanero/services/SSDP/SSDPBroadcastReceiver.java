package io.ourglass.bucanero.services.SSDP;

/**
 * Created by mkahn on 11/14/16.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class SSDPBroadcastReceiver extends BroadcastReceiver {

    private SSDPBroadcastReceiverListener mListener;

    public interface SSDPBroadcastReceiverListener {

        void receivedSSDPUpdate(Intent intent);

    }

    public SSDPBroadcastReceiver(SSDPBroadcastReceiverListener listener){
        mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (mListener!=null){
            mListener.receivedSSDPUpdate(intent);
        }

    }

}
