package io.ourglass.bucanero.services.AudioSampler;


import android.content.Context;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

/**
 * All this service does is grab a sample of the audio off the HDMI
 * It's not intended to be used except as an example for Miracle Ear
 *
 * This service actually does nothing right now, because there is no call to start/stop.
 *
 */

public class AudioSampler  implements MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {

    public static final String TAG = "AudioSampleService";

    private MediaRecorder mRecorder;
    private Context mContext;


    public AudioSampler(Context context){
        //processIntent(intent);
        mRecorder = new MediaRecorder();
        mRecorder.setOnErrorListener(this);
        mRecorder.setOnInfoListener(this);
        mContext = context;
    }



    public void release() {
        mRecorder.release();
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        Toast.makeText(mContext, "Media Recorder Error", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        // Is not usually called, supposedly
    }

    public void startAudioRecording() {

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
            Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_LONG).show();
        }

    }

    public void stopAudioRecording(){
        mRecorder.stop();
    }

}

