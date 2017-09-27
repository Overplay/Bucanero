package io.ourglass.bucanero.services.FFmpeg;

import android.content.Context;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;

import io.ourglass.bucanero.core.ABApplication;

/**
 * Rewritten as a "Worker" and not a Service
 */
public class FFmpegBinary {

    public static final String TAG = "FFmpegBinary";
    public static final String BINNAME = "ffmpeg";


    public static void load() {
        Log.d(TAG, "onCreate");
        Log.d(TAG, "cmd: " + ffBinaryCmd());
        ffLoad();
    }

    public static String ffBinaryCmd() {
        return ABApplication.sharedContext.getFilesDir().getAbsolutePath() + File.separator + BINNAME;
    }

    public static boolean ffExists() {
        String ffCmd = ffBinaryCmd();
        File ffFile = new File( ffCmd );
        return ffFile.exists();
    }

    private static void ffLoad() {

        Context context = ABApplication.sharedContext;
        FFmpeg ffmpeg = FFmpeg.getInstance(context);

        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                @Override
                public void onStart() { Log.d(TAG, "ffLoad start"); }

                @Override
                public void onFailure() { Log.w(TAG, "ffLoad failure"); }

                @Override
                public void onSuccess() {
                    Log.i(TAG, "ffLoad success");
                    ffCheck();
                }

                @Override
                public void onFinish() { Log.d(TAG, "ffLoad finish"); }
            });
        } catch (FFmpegNotSupportedException e) {
            // Handle if FFmpeg is not supported by device
            Log.e(TAG, "ffLoad", e);
        }
    }

    private static void ffCheck() {
        Context context = ABApplication.sharedContext;
        FFmpeg ffmpeg = FFmpeg.getInstance(context);
        try {
            // to execute "ffmpeg -version" command you just need to pass "-version"
            String[] cmd = {"-version"};
            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {}

                @Override
                public void onProgress(String message) { Log.d(TAG, "ffCheck progress " + message); }

                @Override
                public void onFailure(String message) { Log.w(TAG, "ffCheck failure " + message); }

                @Override
                public void onSuccess(String message) { Log.i(TAG, "ffCheck success " + message); }

                @Override
                public void onFinish() { Log.d(TAG, "ffCheck finish"); }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
            Log.e(TAG, "ffCheck", e);
        }
    }

}
