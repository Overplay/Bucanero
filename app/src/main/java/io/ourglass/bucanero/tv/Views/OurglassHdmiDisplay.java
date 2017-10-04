package io.ourglass.bucanero.tv.Views;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
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

import java.io.IOException;
import java.util.List;

import io.ourglass.bucanero.R;
import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.core.OGConstants;
import io.ourglass.bucanero.messages.OGLogMessage;
import io.ourglass.bucanero.messages.SystemStatusMessage;
import io.ourglass.bucanero.services.FFmpeg.AudioStreamer;

import static io.ourglass.bucanero.messages.SystemStatusMessage.SystemStatus.AS_LOS;

public class OurglassHdmiDisplay {
    private static final String TAG = "OurglassHdmiDisplay";

    //public View								mPreview				= null;

    private SurfaceView						mSurfaceView			= null;
    private SurfaceHolder					mSurfaceHolder			= null;
    private RtkHDMIRxManager				mHDMIRX					= null;

    private Handler							mHandler				= new Handler();


    private boolean                         mHDMISurfaceReady = false;
    private final static int				DISPLAY					= 0;
    private final static int				DISPLAYTIME				= 200;
    private int								mFps					= 0;
    private int								mWidth					= 0;
    private int								mHeight					= 0;
    private boolean							hdmiConnectedState		= false;

    // This variable is actually disconnected from reality. It is what the code *thinks* is happening
    // with no feedback from H/W
    private boolean                         iThinkHDMIisPlaying = false;
    private Context							mContext				= null;
    private ViewGroup						mRootView				= null;
    private ParcelFileDescriptor[]          ffPipe                  = null;
    private AudioStreamer 					mAudioStreamer  		= null;
    private boolean			                isStreaming			    = false;

    public OurglassHdmiDisplay(Context mContext, ViewGroup rootView) {
        this.mContext = mContext;
        this.mRootView = rootView;
        //init();
        initView();
        initStreamer();
    }

    private void delayPlayHDMI(int delayMs){

        Log.v(TAG, "delayPlayHDMI called. Waiting: "+delayMs);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "delayPlayHDMI executing");
                play();
            }
        }, delayMs);

    }

    private void initView() {
        // setup view type
        RelativeLayout rootView = (RelativeLayout) mRootView.findViewById(R.id.home_ac_hdmi_textureView);
        mSurfaceView = new SurfaceView(mContext);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {

                @Override
                public void surfaceChanged(SurfaceHolder arg0, int arg1, int width, int height) { }

                @Override
                public void surfaceCreated(SurfaceHolder arg0) {
                    Log.v(TAG, "SurfaceCreated");
                    mHDMISurfaceReady = true;
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder arg0) {
                    Log.v(TAG, "SurfaceDestroyed");
                    mHDMISurfaceReady = false;
                }

            });

        // Why is this done?
        //mPreview = mSurfaceView;
        LayoutParams param = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mSurfaceView.setLayoutParams(param);
        rootView.addView(mSurfaceView);
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

    public void stop() {

        Log.v(TAG, "stop() called.");
        if (mSurfaceView != null) {
            Log.v(TAG, "stop() hiding Surface View");
            mSurfaceView.setVisibility(View.INVISIBLE);
        }

        if (mHDMIRX != null) {
            Log.v(TAG, "stop() calling stop() on driver..");
            int stopResult = mHDMIRX.stop();
            Log.v(TAG, "stop() result of driver stop was (0=good) "+ stopResult);
            Log.v(TAG, "stop() releasing native driver, he's probably shitty at it anyway.");
            mHDMIRX.release();
            mHDMIRX = null;
        }


        iThinkHDMIisPlaying = false;
        mFps = 0;
        mWidth = 0;
        mHeight = 0;
        (new SystemStatusMessage(SystemStatusMessage.SystemStatus.HDMI_STOP)).post();
    }

    public void play() {

        Log.v(TAG, "play() called.");

        mHandler.removeCallbacksAndMessages(null);
        // OK, so this was probably here to prevent multiple play calls
        //mHandler.removeMessages(DISPLAY);

        if (mSurfaceView == null) {
            // TODO this should throw
            Log.v(TAG, "play() called on a null surface view, bailing");
            return;
        }

        if (!mHDMISurfaceReady){
            Log.v(TAG, "play() called and HDMI SurfaceView isn't ready yet, gonna chill a bit (1 sec) and retry.");
            delayPlayHDMI(1000);
            return;
        }

        if (mHDMIRX == null) {
            Log.v(TAG, "play() called and there is no HDMIRxManager (null), creating");
            mHDMIRX = new RtkHDMIRxManager();
        }

        mSurfaceView.setVisibility(View.VISIBLE);

        Log.v(TAG, "play------------- What I *think* HDMI is playing = " + iThinkHDMIisPlaying + " HDMI surface ready = " + mHDMISurfaceReady);

        if (!iThinkHDMIisPlaying) {

            HDMIRxStatus rxStatus = mHDMIRX.getHDMIRxStatus();

            if ( rxStatus == null ){
                Log.wtf(TAG, "rxStatus from chipset is NULL. This is a hard fucking fail!");
            }
            else if ( rxStatus.status == HDMIRxStatus.STATUS_READY ) {

                Log.v(TAG, "play(): HDMI status is STATUS_READY, opening driver.");
                int i = mHDMIRX.open();  // This registers package name with underlying shitty C code

                if (i != 0) {
                    // Could not open the driver, so we are probably really fucked
                    Log.wtf(TAG, "Could not open driver. Probably we are fucked. Trying again in 1 second.");
                    mWidth = 0;
                    mHeight = 0;
                    mHDMIRX = null;
                    delayPlayHDMI(1000);
                    return;
                }

                Log.v( TAG, "play() successfully opened the driver. So we got that going for us.");
                HDMIRxParameters hdmirxGetParam = mHDMIRX.getParameters();
                Log.v(TAG, "Params from driver: " + hdmirxGetParam.flatten());
                getSupportedPreviewSize(hdmirxGetParam, rxStatus.width, rxStatus.height);
                mFps = getSupportedPreviewFrameRate(hdmirxGetParam);
                // mScanMode = rxStatus.scanMode;

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
                    // MAK: not really sure what play() and setPlayback do differently
                    mHDMIRX.play();
                    iThinkHDMIisPlaying = true;
                    mHDMIRX.setPlayback(true, true);
                    Log.v(TAG, "hdmi mIsPlaying successfully, I hope");
                    (new SystemStatusMessage(SystemStatusMessage.SystemStatus.HDMI_PLAY)).post();
                    // animation
                } catch (Exception e) {
                    stop();
                    Log.e(TAG, "Exception deep in play()", e);;
                    SystemStatusMessage.sendStatusMessage(SystemStatusMessage.SystemStatus.HDMI_SEVERE_ERROR);
                    delayPlayHDMI(1000);
                }


            } else {
                Log.d(TAG, "play() got an non-ready status from the driver. Fuck.");
                Log.d(TAG, "Status ( 0 = not ready ): " + rxStatus.status);
                Log.d(TAG, "play() is gonna try again in a second, maybe the status will be ready?");
                delayPlayHDMI(1000);
                return;
            }



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


    public void startDisplay() {
        Log.v(TAG, "startDisplay");
        play();
    }

    public void stopDisplay() {
        Log.v(TAG, "stopDisplay");
        stop();

        // SJM: This is probably broken because the callback needs to be registered.
        //if (mSurfaceView != null && mSurfaceHolder != null && mCallback != null) {
        //    mSurfaceHolder.removeCallback(mCallback);
        //}
    }

    private void repeatDisplay() {
        Log.v(TAG, "repeatDisplay");
        stop();
        mHandler.sendEmptyMessageDelayed(DISPLAY, DISPLAYTIME + 2 * 1000);
    }

    public boolean isStreaming() { return isStreaming; }

    public void stopStreamer() {
        try {
            isStreaming = false;
            if (mHDMIRX != null) {
                mHDMIRX.setTranscode(false);
            }
            if (mAudioStreamer != null) {
                mAudioStreamer.killStream();
            }

            //Toast.makeText(mContext, "Stop streamer successful!", Toast.LENGTH_SHORT).show();
            ABApplication.dbToast("Stop streamer successful!");

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

        if (!iThinkHDMIisPlaying) {
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
            ABApplication.dbToast("Start streamer successful!");
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
