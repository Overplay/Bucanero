package io.ourglass.bucanero.services.OGLog;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import com.squareup.otto.Subscribe;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.core.OGConstants;
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

public class OGLogService extends Service {
    static final String TAG = "OGLogService";

    HandlerThread mWorkerThread = new HandlerThread("cleanAndPush");
    private Handler mWorkerThreadHandler;

    final OkHttpClient client = ABApplication.okclient;  // share it

    public OGLogService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        ABApplication.dbToast(this, "Log Process Starting");

        ABApplication.ottobus.register(this);

        if (!mWorkerThread.isAlive()){
            mWorkerThread.start();
            mWorkerThreadHandler = new Handler(mWorkerThread.getLooper());
        }

        startLoop();

        return Service.START_STICKY;
    }

    private void startLoop(){
        Log.d(TAG, "starting OGLogService");

        Runnable runLogClean = new Runnable(){
            @Override
            public void run(){

                Log.d(TAG, "In OGLogService runnable");

                Realm realm = Realm.getDefaultInstance();

                //find all logs that haven't been uploaded
                RealmResults<OGLog> logs = realm.where(OGLog.class)
                        .equalTo("uploadedAt", 0).findAll();

                if(logs.size() != 0){
                    uploadLogs(realm, logs);
                }
                else {
                    Log.v(TAG, "There are no logs to upload");
                }


                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        long weekAgo = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 7);
                        RealmResults<OGLog> oldLogs = realm.where(OGLog.class)
                                .lessThan("uploadedAt", weekAgo).findAll();
                        Log.d(TAG, "Deleting "+oldLogs.size()+" old logs from Realm");
                        oldLogs.deleteAllFromRealm();
                    }
                });

                realm.close();

                //TODO replace this with constant or settings, right now set to 15 minutes
                mWorkerThreadHandler.postDelayed(this, OGConstants.LOG_UPLOAD_INTERVAL);

            }

        };

        Runnable runLogcat = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Uploading logcat snapshot.");
                LogCat.takeLogcatSnapshotAndPost();
                mWorkerThreadHandler.postDelayed(this, 5 * 1000 * 60);
            }
        };

        Runnable heartbeat = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Uploading heartbeat.");
                OGLogMessage.newHeartbeatLog().post();
                mWorkerThreadHandler.postDelayed(this, 1 * 1000 * 60);
            }
        };

        // Put both runnables on loop
        mWorkerThreadHandler.post(runLogClean);
        mWorkerThreadHandler.post(runLogcat);
        mWorkerThreadHandler.post(heartbeat);


    }


    // Careful! This runs synchronously so should be on BG thread.
    public Response postLog(String jsonBody) throws IOException {

        final MediaType type = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(type, jsonBody);
        Log.v(TAG, jsonBody);

        String url = OGConstants.BELLINI_DM_ADDRESS + "/oglog/postlog";
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        return client.newCall(request).execute();

    }

    public void uploadLogs(Realm realm, RealmResults<OGLog> logs){

        int numUploaded = 0;

        for(final OGLog log : logs){

            JSONObject logJSON = log.toJson();
            String postBody = logJSON.toString();

            try {
                Response response = postLog(postBody);

                if(!response.isSuccessful())
                    throw new IOException("Unexpected code " + response);

                Log.v(TAG, "successfully uploaded log to Bellini, will now mark the upload time");
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        log.setUploaded();
                    }
                });

                numUploaded++;
            } catch (IOException e) {
                Log.w(TAG, "there was an error uploading log (" + e.getMessage() + "), will not mark as uploaded");
            }

        }

        Log.v(TAG, "Uploaded a total of " + numUploaded + " logs");

    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        mWorkerThreadHandler.removeCallbacksAndMessages(null);
        ABApplication.ottobus.unregister(this);
        super.onDestroy();
    }

    public void uploadLogFile(File logFile, String timeStamp){

        JSONObject metadataObj = new JSONObject();

        try {
            metadataObj.put("uuid", OGSystem.getUDID());
            metadataObj.put("timeStamp", timeStamp);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody reqBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("metadata", metadataObj.toString())
                .addFormDataPart("file", logFile.getName(),
                        RequestBody.create(MediaType.parse("text/plain"), logFile))
                .build();

        Request request = new Request.Builder()
                .url(OGConstants.BELLINI_ADDRESS + "/media/upload")
                .post(reqBody)
                .build();


        try {
            Response r = client.newCall(request).execute();
            if(!r.isSuccessful()) {
                Log.d(TAG, "Could upload OGLog, saving in Realm");
            } else {
                Log.d(TAG, "OGLog uploaded.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    public void shoveInRealm(OGLogMessage message){
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        OGLog log = realm.createObject(OGLog.class);
        log.logType = message.logType;
        log.loggedAt = message.loggedAt;
        log.message = message.message.toString();
        log.logFile = message.logFile;
        realm.commitTransaction();
    }

    @Subscribe
    public void inboundLogMessage(final OGLogMessage message){
        // This comes in on the main thread!
        Log.d(TAG, "Received inbound log message, going to try to post it.");
        mWorkerThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                String jsonBody = message.toJsonString();
                try {
                    Response r = postLog(jsonBody);
                    if(!r.isSuccessful()) {
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
        });

    }

}
