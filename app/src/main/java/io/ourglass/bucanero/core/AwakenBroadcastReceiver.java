package io.ourglass.bucanero.core;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Created by mkahn on 10/28/17.
 */


// This didn't properly awaken the app, so the manifest entry is gone.
public class AwakenBroadcastReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d("AWAKEN", "Wakey wakey");
        PackageManager pm = context.getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage("io.ourglass.bucanero");
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, launchIntent, 0);

    }
}
