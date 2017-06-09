package io.ourglass.bucanero.services.FFmpeg;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import io.ourglass.bucanero.core.OGSystem;
//import io.ourglass.bucanero.services.FFmpeg.LazyTransferThread;
//import io.ourglass.bucanero.core.ABApplication;
//SJMimport io.ourglass.bucanero.messages.MainThreadBus;

public class AudioStreamer implements MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {

    public static final String TAG = "AudioSampler";

    public static final String AUDIO_HOST = "192.241.217.88";
    public static final String AUDIO_PORT = "3000";
    public static final String AUDIO_SECRET = "supersecret";
    public static final int AUDIO_BUFFERFILLTIME_MS = 100;
    public static final long AUDIO_SERVERRECONNECTTIME_MS = 60000;

    public String mHostURL;

    //SJMpublic MainThreadBus mBus = ABApplication.ottobus;
    private Context mContext;

    private MediaRecorder mMediaRecorder = null;
    private Process mFFMpegProcess = null;
    private LazyTransferThread ltt = null;
    private ParcelFileDescriptor[] ffPipe = null;

    public AudioStreamer(Context context) {
        mContext = context;
        mHostURL = "http://" + AUDIO_HOST + ":" + AUDIO_PORT + "/" + AUDIO_SECRET + "/" + OGSystem.getUDID();
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
                recorderStop();
                recorderDestroy();
                recorderInit();
                //ltt.interrupt();
                recorderStart();
            }
        }).start();
    }

    private void recorderInit() {
        mMediaRecorder=new MediaRecorder();
        mMediaRecorder.setOnErrorListener(this);
        mMediaRecorder.setOnInfoListener(this);
    }

    private void recorderStart() {
        if ((mMediaRecorder != null) && ffExists()) {
            configAudioZidoo(true);

            try {
                mMediaRecorder.prepare();
                mMediaRecorder.start();
            }
            catch (Exception e) {
                Log.e(TAG, "Exception in preparing recorder", e);
                //Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void recorderStop() {
        try {
            if (mMediaRecorder != null) {
                mMediaRecorder.stop();
            }
        }
        catch (Exception e) {
            Log.w(TAG, "Exception in stopping recorder", e);
            // can fail if start() failed for some reason
        }

        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
        }

        try {
            if (mFFMpegProcess != null) {
                mFFMpegProcess.destroy();
            }
        }
        catch (Exception e) {
            Log.w(TAG, "Exception in destroying mFFMpegProcess", e);
            // can fail if destroy() failed for some reason
        }

    }
    private void recorderDestroy() {
        if (mMediaRecorder != null) {
            mMediaRecorder.release();
        }
        mMediaRecorder=null;
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        String msg = "Media Recorder Error";//getString(R.string.strange);
        switch (what) {
            case MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN:
                msg="Media Recorder Error Unknown";//getString(R.string.unknown_error);
                break;

            case MediaRecorder.MEDIA_ERROR_SERVER_DIED:
                msg="Media Recorder Server Died";//getString(R.string.server_died);
                recorderRekick(AUDIO_SERVERRECONNECTTIME_MS);
                break;
        }
        Log.w(TAG, msg);
        //SJMToast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        // Is not usually called, supposedly
        String msg="Media Recorder Info";//getString(R.string.strange);
        switch (what) {
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                msg="Media Recorder Max Duration Reached";//getString(R.string.max_duration);
                break;

            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                msg="Media Recorder Max File Size Reached";//getString(R.string.max_size);
                break;
        }
        Log.d(TAG, msg);
        //SJMToast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void configAudioZidoo(boolean inStereo) {
        if ((mMediaRecorder != null) && ffExists()) {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setOutputFormat(8);
            mMediaRecorder.setOutputFile(getStreamFd());
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            if (inStereo) {
                mMediaRecorder.setAudioChannels(2);
                mMediaRecorder.setAudioSamplingRate(44100);
            } else {
                mMediaRecorder.setAudioChannels(1);
                mMediaRecorder.setAudioSamplingRate(22050);
            }
            mMediaRecorder.setAudioEncodingBitRate(128000); // Was 44100
        }
    }

    private FileDescriptor getStreamFd() {

        try {
            mFFMpegProcess = Runtime.getRuntime().exec(getFFCommand());
        }
        catch (IOException e) {
            //Log.e(getClass().getSimpleName(), "Exception starting exec", e);
            Log.e(TAG, "Exception starting exec", e);
        }

        try {
            if (mFFMpegProcess != null) {
                ffPipe = ParcelFileDescriptor.createReliablePipe();

                //SJMThread.UncaughtExceptionHandler h = new Thread.UncaughtExceptionHandler() {
                //SJM    public void uncaughtException(Thread th, Throwable ex) {
                //SJM        Log.e("SJM", "Uncaught exception: " + ex);
                //SJM    }
                //SJM};

                ltt = new LazyTransferThread(new ParcelFileDescriptor.AutoCloseInputStream(ffPipe[0]),
                        (FileOutputStream)mFFMpegProcess.getOutputStream(),
                        AUDIO_BUFFERFILLTIME_MS);
                //SJMltt.setUncaughtExceptionHandler(h);
                ltt.start();

            }
        }
        catch (IOException e) {
            Log.e(TAG, "Exception opening ffPipe", e);
        }

        if (ffPipe != null) {
            return ffPipe[1].getFileDescriptor();
        } else {
            return null;
        }
    }

    private String ffBinaryCmd() {
        //String c = getApplicationContext().getFilesDir()
        String cc = mContext.getFilesDir().getAbsolutePath() + File.separator + "ffmpeg";
        //String cc = "/data/data/io.ourglass.bucanero/files" + File.separator + FFmpegBinaryService.BINNAME;
        Log.d(TAG, "This is cc: " + cc);
        return cc;
    }

    public boolean ffExists() {
        String ffCmd = ffBinaryCmd();
        File ffFile = new File( ffCmd );
        return ffFile.exists();
    }

    private String getFFCommand() {
        String bin = ffBinaryCmd();
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
        Log.d(TAG, "starting ffmpeg " + cmd);

        // ffmpeg
        // -hide_banner -loglevel quiet -nostats
        // -re -stream_loop -1
        // -i <fn>
        // -dn -sn -vn -bsf:v dump_extra
        // -codec:a mp2 -b:a 128k -ar 44100 -ac 2 -muxdelay 0.001 -f mpegts http://localhost:3000/supersecret/abc
        // For testing:  -re -stream_loop -1
        return bin +  " -hide_banner -loglevel quiet -nostats" + cmd;
    }

}
