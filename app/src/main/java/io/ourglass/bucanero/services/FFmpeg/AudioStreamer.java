package io.ourglass.bucanero.services.FFmpeg;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import io.ourglass.bucanero.core.OGSystem;
//import io.ourglass.bucanero.services.FFmpeg.LazyTransferThread;
//import io.ourglass.bucanero.core.ABApplication;
//SJMimport io.ourglass.bucanero.messages.MainThreadBus;

public class AudioStreamer {

    public static final String TAG = "AudioStreamer";

    public static boolean USE_HTTPS = false;

    public static final String AUDIO_HOST = USE_HTTPS ? "cloud-listen.ourglass.tv" : "192.241.217.88";
    public static final int AUDIO_PORT = USE_HTTPS ? 80 : 3000;
    //public static final int AUDIO_PORT = 80;

    public static final String AUDIO_SECRET = "supersecret";
    public static final int AUDIO_BUFFERFILLTIME_MS = 100;
    public static final long AUDIO_SERVERRECONNECTTIME_MS = 60000;

    public String mHostURL;
    public String mFFBinaryCmd;

    //SJMpublic MainThreadBus mBus = ABApplication.ottobus;
    private Context mContext;

    private Process mFFMpegProcess = null;
    private LazyTransferThread ltt = null;
    private ParcelFileDescriptor[] ffPipe = null;
    private StreamDeadListener mStreamDeadListener = null;

    public interface StreamDeadListener
    {
        public void streamDead(Context context);
    }

    public AudioStreamer(Context context, StreamDeadListener streamDeadListener) {
        mContext = context;
        mStreamDeadListener = streamDeadListener;

        if ( AUDIO_PORT != 80 ){
            mHostURL = "http://" + AUDIO_HOST + ":" + AUDIO_PORT + "/" + AUDIO_SECRET + "/" + getUDID();
        } else {
            // nginx barfs with the port 80 explicit
            mHostURL = "https://" + AUDIO_HOST + "/" + AUDIO_SECRET + "/" + getUDID();
        }

        mHostURL = "http://192.241.217.88:4000/as/supersecret/" + getUDID(); //SJMDBG
        mFFBinaryCmd = mContext.getFilesDir().getAbsolutePath() + File.separator + "ffmpeg";
        Log.d(TAG, "This is binary: " + mFFBinaryCmd);
        Log.d(TAG, "This is hostURL: " + mHostURL);
    }

    public String getUDID() {
        return OGSystem.getUDID(); //"12345678";
    }

    public void recorderRekick(final long millisDelay) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(millisDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //ltt.interrupt();
            }
        }).start();
    }

    public ParcelFileDescriptor getStreamFd() {

        try {
            if (mFFMpegProcess != null) {
                mFFMpegProcess.destroy();
            }
        }
        catch (Exception e) {
            Log.w(TAG, "Exception in destroying mFFMpegProcess", e);
            // can fail if destroy() failed for some reason
        }

        File ffFile = new File( mFFBinaryCmd );
        if (!ffFile.exists()) {
            Log.w(TAG, "Can't find exec");
            return null;
        }

        try {
            String ff = getFFCommand();
            Log.d(TAG, "starting " + ff);
            mFFMpegProcess = Runtime.getRuntime().exec(ff);


            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int rv = mFFMpegProcess.waitFor();
                        Log.w(TAG, "ffmpeg exited with code " + rv);
                    } catch (InterruptedException e) {
                        //e.printStackTrace();
                        Log.e(TAG, "ffmpeg exited", e);
                    }
                    ltt.interrupt();
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
                    if (mStreamDeadListener != null) {
                        Log.v(TAG, "mStreamDeadListener != null");
                        mStreamDeadListener.streamDead(mContext);
                    }

                }
            }).start();

        }
        catch (IOException e) {
            //Log.e(getClass().getSimpleName(), "Exception starting exec", e);
            Log.e(TAG, "Exception starting exec", e);
        }

        try {
            if (mFFMpegProcess != null) {
                ffPipe = ParcelFileDescriptor.createReliablePipe();

                Thread.UncaughtExceptionHandler h = new Thread.UncaughtExceptionHandler() {
                    public void uncaughtException(Thread th, Throwable ex) {
                        Log.e("SJM", "Uncaught exception: " + ex);
                    }
                };

                ltt = new LazyTransferThread(new ParcelFileDescriptor.AutoCloseInputStream(ffPipe[0]),
                        (FileOutputStream)mFFMpegProcess.getOutputStream(),
                        AUDIO_BUFFERFILLTIME_MS);
                ltt.setUncaughtExceptionHandler(h);
                ltt.start();

            }
        }
        catch (IOException e) {
            Log.e(TAG, "Exception opening ffPipe", e);
        }

        if (ffPipe != null) {
            //return ffPipe[1].getFileDescriptor();
            return ffPipe[1];
        } else {
            return null;
        }
    }

    private String getFFCommand() {
        String cmd = new String(
                " -i -" +
                        " -dn -sn -vn" +
                        //" -codec:v mpeg1video" +
                        //" -b:v 1000k" +
                        //" -s 320x240" +
                        //
                        " -bsf:v dump_extra" +
                        " -codec:a mp2" +
                        " -b:a 128k" +
                        " -ar 44100" +
                        " -ac 2" +
                        //
                        //" -bf 16" +
                        //" -bsf:v dump_extra" +
                        //" -muxdelay 0.001" +
                        " -f mpegts" +
                        //        " http://192.168.86.105:8081/supersecret/12345678" +
                        " " + mHostURL +
                        "");

        // ffmpeg
        // -hide_banner -loglevel quiet -nostats
        // -re -stream_loop -1
        // -i <fn>
        // -dn -sn -vn -bsf:v dump_extra
        // -codec:a mp2 -b:a 128k -ar 44100 -ac 2 -muxdelay 0.001 -f mpegts http://localhost:3000/supersecret/abc
        // For testing:  -re -stream_loop -1
        return mFFBinaryCmd +  " -hide_banner -loglevel quiet -nostats" + cmd;
    }

}
