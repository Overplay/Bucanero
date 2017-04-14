package io.ourglass.bucanero.services.AudioSample;


import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.messages.MainThreadBus;

/**
 * All this service does is grab a sample of the audio off the HDMI
 * It's not intended to be used except as an example for Miracle Ear
 *
 * This service actually does nothing right now, because there is no call to start/stop.
 *
 */

public class AudioSampleService extends Service implements MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {

    public static final String TAG = "AudioSampleService";

    MediaRecorder mRecorder;

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

        //processIntent(intent);
        mRecorder = new MediaRecorder();
        mRecorder.setOnErrorListener(this);
        mRecorder.setOnInfoListener(this);

        return Service.START_STICKY;

    }

    private void processIntent(Intent intent){

    }


    public void onDestroy() {

        mRecorder.release();
        mRecorder = null;

        Log.d(TAG, "onDestroy");
        super.onDestroy();

    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        Toast.makeText(this, "Media Recorder Error", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {

    }

    private void startAudioRecording() {

        File audioDir = new File( Environment.getExternalStorageDirectory() + "/ogaudio" );

        // create audio folder
        if ( !audioDir.exists() ) {
            audioDir.mkdir();
        }

        File output = new File(audioDir, "ogaudiosample.mp3");

        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(output.getAbsolutePath());
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mRecorder.setAudioEncodingBitRate(160 * 1024);
        mRecorder.setAudioChannels(2);

        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(),
                    "Exception in preparing recorder", e);
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

    }

    private void stopAudioRecording(){

        mRecorder.stop();

    }

}

