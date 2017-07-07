package io.ourglass.bucanero.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.realtek.hardware.RtkHDMIRxManager;
import com.realtek.hardware.RtkHDMIRxManager.Size;
import com.realtek.server.HDMIRxParameters;
import com.realtek.server.HDMIRxStatus;

import java.io.IOException;
import java.util.List;

import io.ourglass.bucanero.messages.SystemStatusMessage;

// Rewrite by MAK, July 2017
public class HDMIRxPlayer2 {
    private final String TAG = "HDMIRxPlayer2";

    // Class from Realtek
    private RtkHDMIRxManager mHDMIRX = null;
    private HDMIRxStatus rxStatus = null;
    // View the HDMI feed is piped into, and a wrapper (Holder) that controls it
    private SurfaceView mSurfaceView = null;
    private SurfaceHolder mSurfaceHolder = null;
    // See below, implements SurfaceCallback for the Holder
    private HDMIRXSurfaceCallback HDMIRXCallback = new HDMIRXSurfaceCallback();
    // Activity context
    private Context mContext;
    private final Handler mHandler = new Handler();

    private boolean mIsPlaying = false;
    private boolean mConfigDone = false;
    private int mPlayCount = 0;

    // HDMI feed input resolution
    private int mWidth = 0;
    private int mHeight = 0;
    private int mFps = 0;
    private int mScanMode = 0;

    private Runnable PlayWhenHDMIReady = new Runnable() {
        @Override
        public void run() {

            Log.d(TAG, "PlayWhenHDMIReady: Checking for HDMI Ready and openable.");
            rxStatus = mHDMIRX.getHDMIRxStatus();

            if (rxStatus != null && rxStatus.status == HDMIRxStatus.STATUS_READY && mHDMIRX.open() == 0) {
                Log.d(TAG, "PlayWhenHDMIReady: green light. Playing()");
                if (configure()){
                    ABApplication.dbToast(mContext, "HDMI Setup Complete");
                    (new SystemStatusMessage(SystemStatusMessage.SystemStatus.HDMI_CONFIGURED)).post();
                    play();
                } else {
                    ABApplication.dbToast(mContext, "Could not configure HDMI!!!");
                    (new SystemStatusMessage(SystemStatusMessage.SystemStatus.HDMI_SEVERE_ERROR)).post();

                }
            } else {
                Log.d(TAG, "PlayWhenHDMIReady: open HDMI RX failed. Will try again in 500ms.");
                mWidth = 0;
                mHeight = 0;
                mHandler.postDelayed(this, 500);
            }
        }
    };

    private boolean configure() {

        Log.d(TAG, "Configuring HDMI settings...");
        HDMIRxParameters hdmirxGetParam = mHDMIRX.getParameters();
        getSupportedPreviewSize(hdmirxGetParam, rxStatus.width, rxStatus.height);
        Log.d(TAG, "~~ Supported preview size is "+rxStatus.width+" by "+ rxStatus.height);
        mFps = getSupportedPreviewFrameRate(hdmirxGetParam);
        Log.d(TAG, "~~ Supported frame rate is " + mFps);
        mScanMode = rxStatus.scanMode;
        Log.d(TAG, "~~ Supported scan mode is " + mScanMode);

        try {
            mHDMIRX.setPreviewDisplay(mSurfaceHolder);
            HDMIRxParameters hdmirxParam = new HDMIRxParameters();
            hdmirxParam.setPreviewSize(mWidth, mHeight);
            hdmirxParam.setPreviewFrameRate(mFps);
            //hdmirxParam.setPreviewFrameRate(30);
            mHDMIRX.setParameters(hdmirxParam);

            Log.d(TAG, "Config complete without exceptions.");
            showStatusToast();
        } catch (IOException e) {
            e.printStackTrace();
            Log.wtf(TAG, "HDMI config FAILED." + e.toString());
            return false;
        }

        mConfigDone = true;
        return true;

    }

    /**
     *
     */
    private BroadcastReceiver hdmiRxHotPlugReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean hdmiRxPlugged = intent.getBooleanExtra(HDMIRxStatus.EXTRA_HDMIRX_PLUGGED_STATE, false);
            if (hdmiRxPlugged) {
                Log.d(TAG, "+++ HDMI Rx is plugged in. Last rez: ( " + mWidth + "x" + mHeight + " )\n");
                (new SystemStatusMessage(SystemStatusMessage.SystemStatus.HDMI_RX_LINK)).post();
                mSurfaceView.post(PlayWhenHDMIReady);
            } else {
                Log.d(TAG, "+++ HDMI Rx signal lost.\n");
                (new SystemStatusMessage(SystemStatusMessage.SystemStatus.HDMI_RX_LOS)).post();
            }
        }
    };


    private HDMIRxPlayer2(Context context, SurfaceView sv, ViewGroup parent, int width, int height) {

        mContext = context;
        boolean wasPassedSV = (sv != null) && (parent == null);

        if (wasPassedSV) {
            mSurfaceView = sv;
        } else {
            mSurfaceView = new SurfaceView(context);
        }

        // Common config options
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(HDMIRXCallback);
        mSurfaceHolder.setFixedSize(width, height);
        mSurfaceView.setZOrderOnTop(false);

        // TODO, not sure the setting here is correct for ViewGroup based...
        if (wasPassedSV)
            mSurfaceView.setVisibility(View.VISIBLE);

        mHDMIRX = new RtkHDMIRxManager();

        IntentFilter hdmiRxFilter = new IntentFilter(HDMIRxStatus.ACTION_HDMIRX_PLUGGED);
        mContext.registerReceiver(hdmiRxHotPlugReceiver, hdmiRxFilter);

    }

    public HDMIRxPlayer2(Context context, SurfaceView sv, int width, int height) {
        this(context, sv, null, width, height);
    }

    public HDMIRxPlayer2(Context context, ViewGroup parent, int width, int height) {
        this(context, null, parent, width, height);
    }

    public boolean isPlaying() {
        return mIsPlaying;
    }

    //Implementation for HDMIRxHandlerInterface
    public boolean play() {

        Log.d(TAG, "Play called");

        if (!mConfigDone){
            Log.wtf(TAG, "Play called, but I am not done config.");
            return mIsPlaying;
        }

        mSurfaceView.setVisibility(View.VISIBLE);
        mHDMIRX.play();
        mIsPlaying = true;

        (new SystemStatusMessage(SystemStatusMessage.SystemStatus.HDMI_PLAY)).post();

        return mIsPlaying;
    };


    // TODO this has not been tested by MAK at-all
    public boolean stop() {

        Log.d(TAG, "Stop called");

        boolean rlt = true;
        mSurfaceView.setVisibility(View.INVISIBLE);
        if (mIsPlaying == true) {
            mIsPlaying = false;
            Log.d(TAG, "stop HDMI RX");
            mHDMIRX.stop();
            showStatusToast();
        } else {
            rlt = false;
        }
        mWidth = 0;
        mHeight = 0;
        mFps = 0;

        (new SystemStatusMessage(SystemStatusMessage.SystemStatus.HDMI_STOP)).post();

        return rlt;
    }

    public byte[] capture() {
        byte[] data = capture(RtkHDMIRxManager.HDMIRX_FORMAT_ARGB, 0, 0, getHdmiInWidth(), getHdmiInHeight(), 320, 180);
        return data;
    }

    public byte[] capture(int format, int x, int y, int cropWidth, int cropHeight, int outWidth, int outHeight) {
        //Log.d(TAG, "Capture fmt:" + format + " (" + x + "," + y + "), " + cropWidth + "x" + cropHeight + " => " + outWidth + "x" + outHeight);
        if (mHDMIRX == null || mIsPlaying == false)
            return null;
        if (x < 0 || y < 0)
            return null;
        if (cropWidth <= 0 || cropHeight <= 0 || cropWidth > getHdmiInWidth() || cropHeight > getHdmiInHeight())
            return null;
        if (outWidth <= 0 || outHeight <= 0)
            return null;
        if (x + cropWidth > getHdmiInWidth())
            cropWidth = getHdmiInWidth() - x;
        if (x + cropHeight > getHdmiInHeight())
            cropHeight = getHdmiInHeight() - y;
        byte[] data = mHDMIRX.getSnapshot(format, x, y, cropWidth, cropHeight, outWidth, outHeight);
        return data;
    }

    public void release() {
        mContext.unregisterReceiver(hdmiRxHotPlugReceiver);
        mSurfaceHolder.removeCallback(HDMIRXCallback);
        mHandler.removeCallbacksAndMessages(null);

        if (mHDMIRX != null) {
            mHDMIRX.release();
            mHDMIRX = null;
        }
    }

    public void setSurfaceSize(int width, int height) {
        mSurfaceHolder.setFixedSize(width, height);
    }

    public int getHdmiInWidth() {
        return mWidth;
    }

    public int getHdmiInHeight() {
        return mHeight;
    }

    public int getHdmiInFps() {
        return mFps;
    }

    public int getHdmiInScanMode() {
        return mScanMode;
    }

    public void showStatusToast() {
        String str;
        if (isPlaying()) {
            str = "OG HDMI In ( " + getHdmiInWidth() + "x" + getHdmiInHeight() + ", " + getHdmiInFps() + "fps, ";
            if (getHdmiInScanMode() == HDMIRxStatus.SCANMODE_INTERLACED)
                str += "Interlace )";
            else
                str += "Progressive )";
        } else {
            str = "No Signal";
        }

        ABApplication.dbToast(mContext, str);
    }


    private void getSupportedPreviewSize(HDMIRxParameters hdmirxGetParam, int rxWidth, int rxHeight) {
        List<Size> previewSizes = hdmirxGetParam.getSupportedPreviewSizes();
        int retWidth = 0, retHeight = 0;
        if (previewSizes == null || previewSizes.size() <= 0)
            return;
        for (int i = 0; i < previewSizes.size(); i++) {
            if (previewSizes.get(i) != null && rxWidth == previewSizes.get(i).width) {
                retWidth = previewSizes.get(i).width;
                retHeight = previewSizes.get(i).height;
                if (rxHeight == previewSizes.get(i).height)
                    break;
            }
        }
        if (retWidth == 0 && retHeight == 0) {
            if (previewSizes.get(previewSizes.size() - 1) != null) {
                retWidth = previewSizes.get(previewSizes.size() - 1).width;
                retHeight = previewSizes.get(previewSizes.size() - 1).height;
            }
        }
        mWidth = retWidth;
        mHeight = retHeight;
    }

    private int getSupportedPreviewFrameRate(HDMIRxParameters hdmirxGetParam) {
        List<Integer> previewFrameRates = hdmirxGetParam.getSupportedPreviewFrameRates();
        int fps = 0;
        if (previewFrameRates != null && previewFrameRates.size() > 0)
            fps = previewFrameRates.get(previewFrameRates.size() - 1);
        else
            fps = 30;
        return fps;
    }

    class HDMIRXSurfaceCallback implements SurfaceHolder.Callback {
        boolean SurfaceIsReady = false;
        int SurfaceWidth;
        int SurfaceHeight;

        @Override
        public void surfaceChanged(SurfaceHolder arg0, int arg1, int width, int height) {
            Log.d(TAG, "HDMIRXSurfaceCallback.surfaceChanged(): " + arg0.toString() + ", width: " + width + ", height: " + height);
            SurfaceIsReady = true;
            SurfaceWidth = width;
            SurfaceHeight = height;
            mPlayCount = 0;
            Log.d(TAG, "Calling play after surface reports ready.");

            mSurfaceView.post(PlayWhenHDMIReady);
        }

        @Override
        public void surfaceCreated(SurfaceHolder arg0) {
            Log.d(TAG, "HDMIRXSurfaceCallback.surfaceCreated(): ");
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder arg0) {
            Log.d(TAG, "HDMIRXSurfaceCallback.surfaceDestroyed(): ");
            SurfaceIsReady = false;
        }
    }
}
