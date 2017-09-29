package io.ourglass.bucanero.core;

import android.util.Log;

import io.ourglass.bucanero.tv.Views.HDMIView;

/**
 * Created by mkahn on 11/27/16.
 */


// Yeah, I had no idea what to call this...
public class OGHygenicExceptionHander implements Thread.UncaughtExceptionHandler {

    public static final String TAG = "OGHygenicExHander";
    private final HDMIView hdmiView;

    public OGHygenicExceptionHander(HDMIView hdmiView) {

        this.hdmiView = hdmiView;
    }

    public void uncaughtException(Thread thread, Throwable exception) {

        Log.e(TAG, "Uncaught exception...");
        Log.e(TAG, exception.toString());
        //hdmiView.onPause();

    }
}