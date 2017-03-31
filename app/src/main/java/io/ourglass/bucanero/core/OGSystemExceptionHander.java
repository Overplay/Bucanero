package io.ourglass.bucanero.core;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by mkahn on 11/27/16.
 */


public class OGSystemExceptionHander implements Thread.UncaughtExceptionHandler {

    public static final String TAG = "OGSystemExceptionHander";
    private final Context myContext;
    private final Class<?> myActivityClass;

    public OGSystemExceptionHander(Context context, Class<?> c) {

        myContext = context;
        myActivityClass = c;
    }

    public void uncaughtException(Thread thread, Throwable exception) {

        Log.e(TAG, "Uncaught exception...restarting activity");
        StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));
        System.err.println(stackTrace);// You can use LogCat too
        Intent intent = new Intent(myContext, myActivityClass);
        String s = stackTrace.toString();
        //you can use this String to know what caused the exception and in which Activity
        intent.putExtra("uncaughtException",
                "Exception is: " + stackTrace.toString());
        intent.putExtra("stacktrace", s);

        // TODO put logging somewhere else
        //OGCore.log_alert("CRASH", s);

        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(myContext, mPendingIntentId,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager)myContext.getSystemService(Context.ALARM_SERVICE);
        // Restart 5 seconds after crash!
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 5000, mPendingIntent);
        System.exit(0);
    }
}