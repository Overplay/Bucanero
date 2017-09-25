package io.ourglass.bucanero.tv.Views;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.realtek.hardware.RtkHDMIRxManager;
import com.realtek.server.HDMIRxParameters;
import com.realtek.server.HDMIRxStatus;

import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.core.OGConstants;
import io.ourglass.bucanero.messages.OGLogMessage;
import io.ourglass.bucanero.messages.SystemStatusMessage;

import io.ourglass.bucanero.services.FFmpeg.AudioStreamer;
import io.ourglass.bucanero.R;

import static io.ourglass.bucanero.messages.SystemStatusMessage.SystemStatus.AS_LOS;

public class OurglassHdmiDisplay {
    private static final String TAG = "OurglassHdmiDisplay";

    public View								mPreview				= null;
    public FloatingWindowSurfaceCallback	mCallback				= null;
    private SurfaceView						mSurfaceView			= null;
    private SurfaceHolder					mSurfaceHolder			= null;
    private RtkHDMIRxManager				mHDMIRX					= null;
    private Handler							mHandler				= null;
    private boolean							mPreviewOn				= false;
    private final static int				DISPLAY					= 0;
    private final static int				DISPLAYTIME				= 200;
    private BroadcastReceiver				mHdmiRxHotPlugReceiver	= null;
    private int								mFps					= 0;
    private int								mWidth					= 0;
    private int								mHeight					= 0;
    private boolean							isConnect				= false;
    private boolean							isDisplay				= false;
    private Context							mContext				= null;
    private ViewGroup						mRootView				= null;
    private View							mHdmiNoSignalView		= null;
    private ParcelFileDescriptor[]          ffPipe                  = null;
    private AudioStreamer 					mAudioStreamer  		= null;
    private boolean			                isStreaming			    = false;

    public OurglassHdmiDisplay(Context mContext, ViewGroup rootView) {
        this.mContext = mContext;
        this.mRootView = rootView;
        init();
    }

    private void init() {
        initView();
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case DISPLAY: {
                        if (isConnect) {
                            play();
                        }
                    }
                    break;
                    default:
                        break;
                }
            }
        };
        initHdmiConnect();
        initStreamer();
    }
    private void initStreamer() {
        mAudioStreamer = new AudioStreamer(mContext, new AudioStreamer.StreamDeadListener() {
            @Override
            public void streamDead(Context lContext) {
                Log.v(TAG, "streamDead");

                ((Activity)lContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if ((ffPipe != null) && (ffPipe.length == 2)) {
                            try {
                                ffPipe[1].closeWithError("Die Milk Face");
                                ffPipe[0].checkError();
                                Log.v(TAG, "all good");
                            } catch (IOException e) {
                                Log.e(TAG, "Exception killing", e);;
                            }
                        }

                        if (isStreaming()) {
                            stopStreamer();
                        }

                        OGLogMessage.newOGLog("streaming_failed")
                                .addFieldToMessage("description", "not sure the reason" )
                                .addFieldToMessage("exception", "pipe or process exited" )
                                .addFieldToMessage("issue_code", 8675309)
                                .post();

                        //SystemStatusMessage.sendStatusMessageWithException(AS_LOS, result);
                        SystemStatusMessage.sendStatusMessage(AS_LOS);

                    }
                });
            }
        });
    }

    private void initHdmiConnect() {
        mHdmiRxHotPlugReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean hdmiRxPlugged = intent.getBooleanExtra(HDMIRxStatus.EXTRA_HDMIRX_PLUGGED_STATE, false);
                isConnect = hdmiRxPlugged;
                mHdmiNoSignalView.setVisibility(isConnect ? View.GONE : View.VISIBLE);
                if (isConnect) {
                    play();
                } else {
                    stop();
                }
            }
        };

        isConnect = isConnect(mContext);
        IntentFilter hdmiRxFilter = new IntentFilter(HDMIRxStatus.ACTION_HDMIRX_PLUGGED);
        mContext.registerReceiver(mHdmiRxHotPlugReceiver, hdmiRxFilter);
    }

    public static boolean isConnect(Context context) {
        IntentFilter intentFilter = new IntentFilter(HDMIRxStatus.ACTION_HDMIRX_PLUGGED);
        Intent pluggedStatus = context.registerReceiver(null, intentFilter);
        boolean hdmiRxPlugged = pluggedStatus.getBooleanExtra(HDMIRxStatus.EXTRA_HDMIRX_PLUGGED_STATE, false);
        return hdmiRxPlugged;
    }

    public void stop() {
        Log.v(TAG, "stop");
        if (mPreview != null) {
            mPreview.setVisibility(View.INVISIBLE);
        }

        if (mHDMIRX != null) {
            mHDMIRX.stop();
            mHDMIRX.release();
            mHDMIRX = null;
        }
        isDisplay = false;
        mFps = 0;
        mWidth = 0;
        mHeight = 0;
        (new SystemStatusMessage(SystemStatusMessage.SystemStatus.HDMI_STOP)).post();
    }

    public void play() {
        if (mPreview == null) {
            return;
        }
        mPreview.setVisibility(View.VISIBLE);
        mHandler.removeMessages(DISPLAY);
        Log.v(TAG, "play------------- mIsPlaying = " + isDisplay + " mPreviewOn = " + mPreviewOn);
        if (!isDisplay && mPreviewOn) {
            mHDMIRX = new RtkHDMIRxManager();
            HDMIRxStatus rxStatus = mHDMIRX.getHDMIRxStatus();
            if (rxStatus != null && rxStatus.status == HDMIRxStatus.STATUS_READY) {
                int i = mHDMIRX.open();
                if (i != 0) {
                    mWidth = 0;
                    mHeight = 0;
                    mHDMIRX = null;
                    mHandler.sendEmptyMessageDelayed(DISPLAY, DISPLAYTIME);
                    return;
                }
                HDMIRxParameters hdmirxGetParam = mHDMIRX.getParameters();
                Log.v(TAG, hdmirxGetParam.flatten());
                getSupportedPreviewSize(hdmirxGetParam, rxStatus.width, rxStatus.height);
                mFps = getSupportedPreviewFrameRate(hdmirxGetParam);
                // mScanMode = rxStatus.scanMode;

            } else {
                mHandler.sendEmptyMessageDelayed(DISPLAY, DISPLAYTIME);
                return;
            }
            try {
                mHDMIRX.setPreviewDisplay(mSurfaceHolder);
                // configureTargetFormat
                HDMIRxParameters hdmirxParam = new HDMIRxParameters();
                Log.v(TAG, "hdmi setPreviewSize  mWidth = " + mWidth + "  mHeight = " + mHeight + "  mFps = " + mFps);
                hdmirxParam.setPreviewSize(mWidth, mHeight);
                hdmirxParam.setPreviewFrameRate(mFps);
                // set sorce format
                mHDMIRX.setParameters(hdmirxParam);
                // configureTargetFormat end
                mHDMIRX.play();
                isDisplay = true;
                mHDMIRX.setPlayback(true, true);
                Log.v(TAG, "hdmi mIsPlaying successful");
                (new SystemStatusMessage(SystemStatusMessage.SystemStatus.HDMI_PLAY)).post();
                // animation
            } catch (Exception e) {
                stop();
                Log.e(TAG, "Exception play", e);;
                SystemStatusMessage.sendStatusMessage(SystemStatusMessage.SystemStatus.HDMI_SEVERE_ERROR);

            }
        } else if (!mPreviewOn) {
            mHandler.sendEmptyMessageDelayed(DISPLAY, DISPLAYTIME);
        }
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

    private void getSupportedPreviewSize(HDMIRxParameters hdmirxGetParam, int rxWidth, int rxHeight) {
        List<RtkHDMIRxManager.Size> previewSizes = hdmirxGetParam.getSupportedPreviewSizes();
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

    private void initView() {
        // setup view type
        RelativeLayout rootView = (RelativeLayout) mRootView.findViewById(R.id.home_ac_hdmi_textureView);
        mHdmiNoSignalView = mRootView.findViewById(R.id.home_ac_hdmi_nosignal);
        mSurfaceView = new SurfaceView(mContext);
        mSurfaceHolder = mSurfaceView.getHolder();
        mCallback = new FloatingWindowSurfaceCallback();
        mSurfaceHolder.addCallback(mCallback);
        mPreview = mSurfaceView;
        LayoutParams param = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mPreview.setLayoutParams(param);
        rootView.addView(mPreview);
    }

    public void setSize(boolean isFull) {
        if (isFull) {
            LayoutParams param = (LayoutParams) mRootView.getLayoutParams();
            param.width = ViewGroup.LayoutParams.MATCH_PARENT;
            param.height = ViewGroup.LayoutParams.MATCH_PARENT;
            mRootView.setLayoutParams(param);
        } else {
            LayoutParams param = (LayoutParams) mRootView.getLayoutParams();
            param.width = (int) (640 * 1.5f);
            param.height = (int) (420 * 1.5f);
            mRootView.setLayoutParams(param);
        }
    }

    class FloatingWindowSurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceChanged(SurfaceHolder arg0, int arg1, int width, int height) { }

        @Override
        public void surfaceCreated(SurfaceHolder arg0) {
            Log.v(TAG, "FloatingWindowSurfaceCallback = surfaceCreated");
            mPreviewOn = true;
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder arg0) {
            Log.v(TAG, "FloatingWindowSurfaceCallback = surfaceDestroyed");
            mPreviewOn = false;
        }
    }

    public void startDisplay() {
        Log.v(TAG, "startDisplay");
        play();
    }

    public void stopDisplay() {
        Log.v(TAG, "stopDisplay");
        if (isStreaming()) {
            stopStreamer();
        }
        stop();

        // SJM: This is probably broken because the callback needs to be registered.
        //if (mSurfaceView != null && mSurfaceHolder != null && mCallback != null) {
        //    mSurfaceHolder.removeCallback(mCallback);
        //}

        if (mHdmiRxHotPlugReceiver != null) {
            mContext.unregisterReceiver(mHdmiRxHotPlugReceiver);
            mHdmiRxHotPlugReceiver = null;
        }
    }

    private void repeatDisplay() {
        Log.v(TAG, "repeatDisplay");
        stop();
        mHandler.sendEmptyMessageDelayed(DISPLAY, DISPLAYTIME + 2 * 1000);
    }

    public boolean isStreaming() { return isStreaming; }

    public void stopStreamer() {
        try {
            //if (isDisplay && isStreaming()) {
            //    repeatDisplay();
            //}
            isStreaming = false;
            if (mHDMIRX != null) {
                mHDMIRX.setTranscode(false);
            }
            if (mAudioStreamer != null) {
                mAudioStreamer.killStream();
            }
            if (isDisplay) {
                repeatDisplay();
            }

            //Toast.makeText(mContext, "Stop streamer successful!", Toast.LENGTH_SHORT).show();
            ABApplication.dbToast(mContext, "Stop streamer successful!");

        } catch (Exception e) {
            Log.e(TAG, "Exception mHDMIRX.setTranscode2", e);;
        }
    }

    public void startStreamer() {
        int videoBitrate = OGConstants.BUCANERO_AV_V_BITRATE;
        int channelCount = OGConstants.BUCANERO_AV_A_CHANNELS;
        int sampleRate   = OGConstants.BUCANERO_AV_A_SAMPLERATE;
        int audioBitrate = OGConstants.BUCANERO_AV_A_BITRATE;
        int w = mWidth;
        int h = mHeight;
        if ((w * h) > OGConstants.BUCANERO_AV_V_MAXWIDTH * OGConstants.BUCANERO_AV_V_MAXHEIGHT) {
            w = OGConstants.BUCANERO_AV_V_MAXWIDTH;
            h = OGConstants.BUCANERO_AV_V_MAXHEIGHT;
        }

        if (!isDisplay) {
            return;
        }

        try {
            ffPipe = ParcelFileDescriptor.createReliablePipe();
            if(!mAudioStreamer.runStream(ffPipe[0])) {
                return;
            }
            /* For kicks I tried to make vConfig (1,1,10) but that breaks the screen/view */
            RtkHDMIRxManager.VideoConfig vConfig = new RtkHDMIRxManager.VideoConfig(w, h, videoBitrate);
            RtkHDMIRxManager.AudioConfig aConfig = new RtkHDMIRxManager.AudioConfig(channelCount, sampleRate, audioBitrate);

            mHDMIRX.configureTargetFormat(vConfig, aConfig);
            mHDMIRX.setTargetFd(ffPipe[1], RtkHDMIRxManager.HDMIRX_FILE_FORMAT_TS);
            mHDMIRX.setTranscode(true);
            isStreaming = true;
            //Toast.makeText(mContext, "Start streamer successful ...", Toast.LENGTH_SHORT).show();
            ABApplication.dbToast(mContext, "Start streamer successful!");
        } catch (IOException e) {
            Log.e(TAG, "Exception creating ffPipe", e);
            return;
        }

        try {
        } catch (Exception e) {
            Log.e(TAG, "Exception streaming", e);;
        }
    }
}
