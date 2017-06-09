package io.ourglass.bucanero.services.FFmpeg;

import java.io.File;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;


public class FFmpegBinaryService extends Service {

    public static final String TAG = "FFmpegBinaryService";
    public static final String BINNAME = "ffmpeg";

    public FFmpegBinaryService() {
    }

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
        Log.d(TAG, "cmd: " + ffBinaryCmd());

        ffLoad();
        String[] c = {"-version"};
        ffCheck(c);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    public String ffBinaryCmd() {
        String cc = ((Context)this).getFilesDir().getAbsolutePath() + File.separator + BINNAME;
        return cc;
    }

    public boolean ffExists() {
        String ffCmd = ffBinaryCmd();
        File ffFile = new File( ffCmd );
        return ffFile.exists();
    }

    private void ffLoad() {
        Context context = this;
        FFmpeg ffmpeg = FFmpeg.getInstance(context);
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                @Override
                public void onStart() { Log.d(TAG, "ffLoad start"); }

                @Override
                public void onFailure() { Log.d(TAG, "ffLoad failure"); }

                @Override
                public void onSuccess() { Log.d(TAG, "ffLoad success"); }

                @Override
                public void onFinish() { Log.d(TAG, "ffLoad finish"); }
            });
        } catch (FFmpegNotSupportedException e) {
            // Handle if FFmpeg is not supported by device
            Log.e(TAG, "ffLoad", e);
        }
    }

    private void ffCheck(String[] cmd) {
        Context context = this;
        FFmpeg ffmpeg = FFmpeg.getInstance(context);
        try {
            // to execute "ffmpeg -version" command you just need to pass "-version"
            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {}

                @Override
                public void onProgress(String message) { Log.d(TAG, "ffCheck progress " + message); }

                @Override
                public void onFailure(String message) { Log.d(TAG, "ffCheck failure " + message); }

                @Override
                public void onSuccess(String message) { Log.d(TAG, "ffCheck success " + message); }

                @Override
                public void onFinish() { Log.d(TAG, "ffCheck finish"); }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
            Log.e(TAG, "ffCheck", e);
        }
    }

}
