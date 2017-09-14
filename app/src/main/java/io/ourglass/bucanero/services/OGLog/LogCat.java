package io.ourglass.bucanero.services.OGLog;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

import io.ourglass.bucanero.Support.OGShellE;
import io.ourglass.bucanero.messages.OGLogMessage;

/**
 * Created by mkahn on 3/31/17.
 */

/**
 * Many of the methods in here require that busybox be installed on the system!
 */
public class LogCat {

    private static final String TAG = "LogCatService";

    public static void takeLogcatSnapshot() {

        Log.d(TAG, "Taking snapshot");

        OGShellE.execRoot("logcat -t 1000 -v long", new OGShellE.OGShellEListener() {
            @Override
            public void stdout(ArrayList<String> results) {
                Log.d(TAG, "Result of logcat exec: " + results);

            }

            @Override
            public void stderr(ArrayList<String> errors) {
                Log.e(TAG, "Errors from logcat exec: " + errors);

            }

            @Override
            public void fail(Exception e) {
                Log.wtf(TAG, "Straight up exception taking logcat snappy");
            }
        });


    }

    // Returns the tail of the logcat as a string
    public static void takeLogcatSnapshotAndPost() {

        Log.d(TAG, "Taking snapshot and Posting");


        final String fname = "/mnt/sdcard/lc-" + System.currentTimeMillis() + ".log";

        OGShellE.execRoot("/system/bin/logcat -d -v threadtime > " + fname, new OGShellE.OGShellEListener() {
            @Override
            public void stdout(ArrayList<String> results) {

                // This is a pig, toss on background thread
                (new Thread(new Runnable() {
                    @Override
                    public void run() {
//                        Storage storage = new Storage(ABApplication.sharedContext);
//                        String path = storage.getExternalStorageDirectory();
//                        String logcat = storage.readTextFile(fname);

                        //Log.d(TAG, "Result of logcat exec: " + logcat.length() + " bytes");
                        JSONObject msg = new JSONObject();
                        try {
                            msg.put("logcat", fname);
                            msg.put("timestamp", new Date().getTime());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        (new OGLogMessage("logcat", msg, fname)).post();

                    }
                })).start();


            }

            @Override
            public void stderr(ArrayList<String> errors) {
                Log.e(TAG, "Errors from logcat exec: " + errors);
            }

            @Override
            public void fail(Exception e) {
                Log.wtf(TAG, "Straight up exception taking logcat snappy");
            }


        });


    }


}
