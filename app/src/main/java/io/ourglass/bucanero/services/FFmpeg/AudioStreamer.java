package io.ourglass.bucanero.services.FFmpeg;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import io.ourglass.bucanero.core.OGConstants;
import io.ourglass.bucanero.core.OGSystem;

public class AudioStreamer {

    public static final String TAG = "AudioStreamer";

    public String mHostURL;
    public String mFFBinaryCmd;

    private Context mContext                       = null;
    private StreamDeadListener mStreamDeadListener = null;
    private Process mFFMpegProcess                 = null;

    public interface StreamDeadListener
    {
        public void streamDead(Context context);
    }

    public static String getUDID() {
        return OGSystem.getUDID(); //"12345678";
    }

    public AudioStreamer(Context            context,
                         StreamDeadListener streamDeadListener) {
        mContext = context;
        mStreamDeadListener = streamDeadListener;

        mHostURL = OGConstants.BELLINI_AUDIO_SERVER_ADDRESS + OGConstants.BELLINI_AUDIO_SERVER_SECRET + "/" + getUDID();
        mFFBinaryCmd = mContext.getFilesDir().getAbsolutePath() + File.separator + "ffmpeg";
        Log.d(TAG, "Process   : " + mFFBinaryCmd);
        Log.d(TAG, "RemoteURL : " + mHostURL);
        Log.i(TAG, "Command   : " + getFFCommand());
    }

    public void killStream() {
        try {
            if (mFFMpegProcess != null) {
                mFFMpegProcess.destroy();
                mFFMpegProcess = null;
            }
        }
        catch (Exception e) {
            Log.w(TAG, "Exception in destroying mFFMpegProcess", e);
            // can fail if destroy() failed for some reason
        }
    }

    public boolean runStream(ParcelFileDescriptor readPipe) {

        killStream();

        File ffFile = new File( mFFBinaryCmd );
        if (ffFile.exists()) {
            try {
                String ff = getFFCommand();
                Log.i(TAG, "Streaming via " + ff);
                mFFMpegProcess = Runtime.getRuntime().exec(ff);
                if (mFFMpegProcess != null) {
                    handleDeadProcess();
                    LazyTransferThread ltt = new LazyTransferThread(new ParcelFileDescriptor.AutoCloseInputStream(readPipe),
                                                                    (FileOutputStream)mFFMpegProcess.getOutputStream(),
                                                                    OGConstants.BUCANERO_AUDIO_BUFFERSIZE_BYTES,
                                                                    OGConstants.BUCANERO_AUDIO_BUFFERFILLTIME_MS);
                    ltt.start();
                    return true;
                }
            }
            catch (IOException e) {
                //Log.e(getClass().getSimpleName(), "Exception starting exec", e);
                Log.e(TAG, "Exception starting exec", e);
            }
        }

        return false;
    }

    private void handleDeadProcess() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "SJM Checking for dead thread");
                try {
                    int rv = mFFMpegProcess.waitFor();
                    Log.w(TAG, "ffmpeg exited with code " + rv);
                    if (mFFMpegProcess != null) {
                        mFFMpegProcess.destroy();
                        mFFMpegProcess = null;
                    }
                } catch (InterruptedException ie) {
                    Log.e(TAG, "ffmpeg exited", ie);
                } catch (Exception e) {
                    Log.w(TAG, "Exception in destroying mFFMpegProcess", e);
                    // can fail if destroy() failed for some reason
                }
                if (mStreamDeadListener != null) {
                    Log.v(TAG, "mStreamDeadListener != null");
                    mStreamDeadListener.streamDead(mContext);
                }

            }
        }).start();
    }

    private String getFFCommand() {
        String cmd = new String(OGConstants.BUCANERO_FFMPEG_CMD_OPTS_SILENCE +
                                OGConstants.BUCANERO_FFMPEG_CMD_OPTS +
                                mHostURL);
        return mFFBinaryCmd + cmd;
    }

}
