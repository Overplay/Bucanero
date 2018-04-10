package io.ourglass.bucanero.services.OGLog;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.snatik.storage.Storage;
import com.squareup.otto.Subscribe;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.core.OGConstants;
import io.ourglass.bucanero.core.OGSettings;
import io.ourglass.bucanero.core.OGSystem;
import io.ourglass.bucanero.messages.OGLogMessage;
import io.ourglass.bucanero.realm.OGLog;
import io.realm.Realm;
import io.realm.RealmResults;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OGLogWorker {

    static final String TAG = "OGLogWorker";

    //HandlerThread mWorkerThread = new HandlerThread("cleanAndPush");
    //private Handler mWorkerThreadHandler;

    private Handler mOGLogHandler;

    final OkHttpClient client = ABApplication.okclient;  // share it

    public OGLogWorker() {
        init();
    }


    private void init() {

        if (true){

            ABApplication.dbToast("Logging Starting");

            ABApplication.ottobus.register(this);

            HandlerThread handlerThread = new HandlerThread("OGLogHandlerThread");
            handlerThread.start();
            Looper looper = handlerThread.getLooper();
            mOGLogHandler = new Handler(looper);

//        if (!mWorkerThread.isAlive()) {
//            mWorkerThread.start();
//            mWorkerThreadHandler = new Handler(mWorkerThread.getLooper());
//        }

            startLoop();

        }


    }

    private void startLoop() {
        Log.d(TAG, "starting OGLogWorker");

        Runnable runLogClean = new Runnable() {
            @Override
            public void run() {

                Log.d(TAG, "In OGLogWorker runnable");

                Realm realm = Realm.getDefaultInstance();

                //find all logs that haven't been uploaded
                RealmResults<OGLog> logs = realm.where(OGLog.class)
                        .equalTo("uploadedAt", 0).findAll();

                if (logs.size() != 0) {
                    uploadLogs(realm, logs);
                } else {
                    Log.v(TAG, "There are no logs to upload");
                }

                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        long weekAgo = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 7);
                        RealmResults<OGLog> oldLogs = realm.where(OGLog.class)
                                .lessThan("uploadedAt", weekAgo).findAll();
                        Log.d(TAG, "Deleting " + oldLogs.size() + " old logs from Realm");
                        oldLogs.deleteAllFromRealm();
                    }
                });

                realm.close();

                mOGLogHandler.postDelayed(this, OGConstants.LOG_UPLOAD_INTERVAL);

            }

        };

        Runnable runLogcat = new Runnable() {
            @Override
            public void run() {
                if (OGSettings.getLogcatUploadMode() == true) {
                    Log.d(TAG, "Uploading logcat snapshot.");
                    LogCat.takeLogcatSnapshotAndPost();
                } else {
                    Log.d(TAG, "Logcat uploads are off, doing nada.");
                }
                mOGLogHandler.postDelayed(this, 5 * 1000 * 60);
            }
        };

        Runnable heartbeat = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Uploading heartbeat.");
                OGLogMessage.newHeartbeatLog().post();
                mOGLogHandler.postDelayed(this, 1 * 1000 * 60);
                try {
                    Crashlytics.setString("last_sysinfo", OGSystem.getSystemInfoString());
                } catch (Exception e){
                    // the above can crash during boot, not a big deal
                }
            }
        };

        // Put runnables on loop
        mOGLogHandler.post(runLogClean);
        mOGLogHandler.post(runLogcat);
        mOGLogHandler.post(heartbeat);


    }


    public void uploadLogs(Realm realm, RealmResults<OGLog> logs) {

        int numUploaded = 0;

        for (final OGLog log : logs) {

            Log.d(TAG, "Moving log from Realm to Cloud. Type: " + log.logType);

            try {
                Response r = (log.logType.equalsIgnoreCase("logcat")) ? postOGLogJSONWithFile(log.toJson()) :
                        postLog(log.toJson().toString());
                if (!r.isSuccessful())
                    throw new IOException("Unexpected code " + r);

                Log.v(TAG, "successfully uploaded log to Bellini, will now mark the upload time");
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        log.setUploaded();
                    }
                });
                numUploaded++;


            } catch (IOException e) {
                e.printStackTrace();
                Log.w(TAG, "there was an error uploading log (" + e.getMessage() + "), will not mark as uploaded");

            }
        }

        Log.v(TAG, "Uploaded a total of " + numUploaded + " logs");

    }


    public void kill() {
        Log.d(TAG, "Kill called");
        ABApplication.ottobus.unregister(this);
        mOGLogHandler.removeCallbacksAndMessages(null);
    }

    public void shoveInRealm(OGLogMessage message) {
        Log.d(TAG, "Putting log into Realm. Type: " + message.logType);
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        OGLog log = realm.createObject(OGLog.class);
        log.logType = message.logType;
        log.loggedAt = message.loggedAt;
        log.message = message.message.toString();
        log.logFile = message.logFile;
        realm.commitTransaction();
    }

    /**
     * Handles the case of simple text OGLogs without file attachment
     *
     * @param message
     */
    private void postSimpleOGLog(final OGLogMessage message) {

        (new Thread(new Runnable() {
            @Override
            public void run() {
                String jsonBody = message.toJsonString();
                try {
                    Log.d(TAG, "Attempting to POST up a log of type: " + message.logType);
                    Response r = postLog(jsonBody);
                    if (!r.isSuccessful()) {
                        Log.d(TAG, "Couldn't upload OGLog, saving in Realm");
                        shoveInRealm(message);
                    } else {
                        Log.d(TAG, "OGLog uploaded.");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.wtf(TAG, "IOException trying to post log");
                    shoveInRealm(message);
                }
            }
        })).start();

    }

    // Careful! This runs synchronously so should be on BG thread.
    public Response postLog(String jsonBody) throws IOException {

        final MediaType type = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(type, jsonBody);
        Log.v(TAG, jsonBody);

        String url = OGSettings.getBelliniDMAddress() + "/oglog/postlog";
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        return client.newCall(request).execute();

    }

    private void postOGLogWithFile(final OGLogMessage message) {

        (new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject jsonBody = message.toJson();

                try {
                    Log.d(TAG, "Attempting to POST up a log with file of type: " + message.logType);

                    Response r = postOGLogJSONWithFile(jsonBody);
                    if (!r.isSuccessful()) {
                        Log.d(TAG, "Could upload OGLog with File, saving in Realm");
                        shoveInRealm(message);
                    } else {
                        Log.d(TAG, "OGLog uploaded.");
                        Storage storage = new Storage(ABApplication.sharedContext);
                        storage.deleteFile(message.logFile);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        })).start();

    }

    private Response postOGLogJSONWithFile(JSONObject logObj) throws IOException {

        RequestBody reqBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("logType", "logcat")
                .addFormDataPart("deviceUDID", OGSystem.getUDID())
                .addFormDataPart("message", logObj.optString("message"))
                .addFormDataPart("loggedAt", "" + logObj.optString("loggedAt"))
                .addFormDataPart("file", logObj.optString("logFile"),
                        RequestBody.create(MediaType.parse("text/plain"),
                                new File(logObj.optString("logFile"))))
                .build();

        Request request = new Request.Builder()
                .url(OGSettings.getBelliniDMAddress() + "/oglog/postFile")
                .post(reqBody)
                .build();

        return client.newCall(request).execute();

    }


    @Subscribe
    public void inboundLogMessage(final OGLogMessage message) {
        // This comes in on the main thread!
        Log.d(TAG, "Received inbound log message, going to try to post it.");
        mOGLogHandler.post(new Runnable() {
            @Override
            public void run() {
                if (message.logType.equalsIgnoreCase("logcat")) {
                    postOGLogWithFile(message);
                } else {
                    postSimpleOGLog(message);
                }
            }
        });

    }

}
