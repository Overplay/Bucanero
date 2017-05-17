package io.ourglass.bucanero.services.OGLog;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

import io.ourglass.bucanero.messages.OGLogMessage;
import io.ourglass.bucanero.services.Connectivity.ShellExecutor;

/**
 * Created by mkahn on 3/31/17.
 */

/**
 *
 * Many of the methods in here require that busybox be installed on the system!
 *
 */
public class LogCat {

    private static final String TAG = "LogCatService";

    public static void takeLogcatSnapshot(){

        Log.d(TAG, "Taking snapshot");

        ShellExecutor logcatSnappy = new ShellExecutor(new ShellExecutor.ShellExecutorListener() {
            @Override
            public void results(ArrayList<String> results) {
                Log.d(TAG, "Result of logcat exec: "+results);

            }
        });

        logcatSnappy.exec("logcat  -t 10000 -v long");

    }

    // Returns the tail of the logcat as a string
    public static void takeLogcatSnapshotAndPost(){

        Log.d(TAG, "Taking snapshot and Posting");

        ShellExecutor logcatSnappy = new ShellExecutor(new ShellExecutor.ShellExecutorListener() {
            @Override
            public void results(ArrayList<String> results) {
                Log.d(TAG, "Result of logcat exec: "+results);

                JSONObject msg = new JSONObject();
                try {
                    JSONArray lines = new JSONArray(results);
                    msg.put("logcat", lines);
                    msg.put("timestamp", new Date().getTime());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                (new OGLogMessage("logcat", msg)).post();
            }
        });

        logcatSnappy.exec("logcat  -t 10000 -v long");


    }



}
