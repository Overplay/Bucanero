package io.ourglass.bucanero.tv.Views;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.realtek.hardware.RtkHDMIRxManager;
import com.realtek.server.HDMIRxParameters;
import com.realtek.server.HDMIRxStatus;

import io.ourglass.bucanero.R;

public class ZidooHdmiDisPlay {
    private static final String TAG = "ZidooHdmiDisPlay";

    public View								mPreview				= null;
    public static final int					TYPE_SURFACEVIEW		= 0;
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
    private boolean							isDisPlay				= false;
    private Context							mContext				= null;
    private ViewGroup						mRootView				= null;
    private View							mHdmiNoSignalView			= null;

    public interface HdmiInFristDisplayListener {
        public void fristDisplay();
    }

    public ZidooHdmiDisPlay(Context mContext, ViewGroup rootView) {
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
        Intent batteryStatus = context.registerReceiver(null, intentFilter);
        boolean hdmiRxPlugged = batteryStatus.getBooleanExtra(HDMIRxStatus.EXTRA_HDMIRX_PLUGGED_STATE, false);
        return hdmiRxPlugged;
    }

    public boolean stop() {
        Log.v(TAG, "stop");
        if (mPreview != null) {
            mPreview.setVisibility(View.INVISIBLE);
        }

        boolean rlt = true;
        if (mHDMIRX != null) {
            mHDMIRX.stop();
            mHDMIRX.release();
            mHDMIRX = null;
        } else {
            rlt = false;
        }
        isDisPlay = false;
        mFps = 0;
        mWidth = 0;
        mHeight = 0;
        return rlt;
    }

    public boolean play() {
        Log.v(TAG, "play");
        if (mPreview == null) {
            return false;
        }
        mPreview.setVisibility(View.VISIBLE);
        mHandler.removeMessages(DISPLAY);
        Log.v(TAG, "play------------- mIsPlaying = " + isDisPlay + " mPreviewOn = " + mPreviewOn);
        if (!isDisPlay && mPreviewOn) {
            mHDMIRX = new RtkHDMIRxManager();
            HDMIRxStatus rxStatus = mHDMIRX.getHDMIRxStatus();
            if (rxStatus != null && rxStatus.status == HDMIRxStatus.STATUS_READY) {
                int i = mHDMIRX.open();
                if (i != 0) {
                    mWidth = 0;
                    mHeight = 0;
                    mHDMIRX = null;
                    mHandler.sendEmptyMessageDelayed(DISPLAY, DISPLAYTIME);
                    return false;
                }
                HDMIRxParameters hdmirxGetParam = mHDMIRX.getParameters();
                Log.v(TAG, hdmirxGetParam.flatten());
                getSupportedPreviewSize(hdmirxGetParam, rxStatus.width, rxStatus.height);
                mFps = getSupportedPreviewFrameRate(hdmirxGetParam);
                // mScanMode = rxStatus.scanMode;

            } else {
                mHandler.sendEmptyMessageDelayed(DISPLAY, DISPLAYTIME);
                return false;
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
                isDisPlay = true;
                mHDMIRX.setPlayback(true, true);
                Log.v(TAG, "hdmi mIsPlaying  successfull");
                // animation
            } catch (Exception e) {
                stop();
                e.printStackTrace();
                Log.e(TAG, "play erro = " + e.getMessage());
            }
        } else if (!mPreviewOn) {
            mHandler.sendEmptyMessageDelayed(DISPLAY, DISPLAYTIME);
            return false;
        } else {
            return false;
        }
        return true;
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
        List<com.realtek.hardware.RtkHDMIRxManager.Size> previewSizes = hdmirxGetParam.getSupportedPreviewSizes();
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
        RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mPreview.setLayoutParams(param);
        rootView.addView(mPreview);
    }

    class FloatingWindowSurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceChanged(SurfaceHolder arg0, int arg1, int width, int height) {
        }

        @Override
        public void surfaceCreated(SurfaceHolder arg0) {
            Log.v(TAG, "FloatingWindowSurfaceCallback = surfaceCreated");
            mPreviewOn = true;
            // play();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder arg0) {
            // stop();
            mPreviewOn = false;
        }
    }

    public void startDisplay() {
        Log.v(TAG, "startDisplay");
        play();
    }

    public void stopDisplay() {
        Log.v(TAG, "stopDisplay");
        stop();
        if (mHdmiRxHotPlugReceiver != null) {
            mContext.unregisterReceiver(mHdmiRxHotPlugReceiver);
            mHdmiRxHotPlugReceiver = null;
        }
    }

}