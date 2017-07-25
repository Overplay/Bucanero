package io.ourglass.bucanero.core;

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

import com.realtek.hardware.RtkHDMIRxManager;
import com.realtek.server.HDMIRxParameters;
import com.realtek.server.HDMIRxStatus;

import java.util.List;

import io.ourglass.bucanero.R;

public class ZidooHdmiDisPlay {
    private final String TAG = "ZidooHdmiDisPlay";

    public FloatingWindowTextureListener	mListener					= null;
    public View mPreview					= null;
    public static final int					TYPE_SURFACEVIEW			= 0;
    public static final int					TYPE_TEXTUREVIEW			= 1;
    private int								mViewType					= TYPE_TEXTUREVIEW;
    public TextureView mTextureView				= null;
    public SurfaceTexture mSurfaceTextureForNoPreview	= null;
    public FloatingWindowSurfaceCallback	mCallback					= null;
    private SurfaceView mSurfaceView				= null;
    private SurfaceHolder mSurfaceHolder				= null;
    private RtkHDMIRxManager mHDMIRX						= null;
    private Handler mHandler					= null;
    private boolean							mPreviewOn					= false;
    private final static int				DISPLAY						= 0;
    private final static int				DISPLAYTIME					= 200;
    private BroadcastReceiver mHdmiRxHotPlugReceiver		= null;
    private int								mFps						= 0;
    private int								mWidth						= 0;
    private int								mHeight						= 0;
    private boolean							isConnect					= false;
    private boolean							isDisPlay					= false;
    private Context mContext					= null;
    private ViewGroup mRootView					= null;
    //SJM NO SIG private View							mHdmieSigleView				= null;
    private HdmiInFristDisplayListener		mHdmiInFristDisplayListener	= null;
    private boolean							isFirstDisplay				= true;

    public interface HdmiInFristDisplayListener {
        public void fristDisplay();
    }

    public ZidooHdmiDisPlay(Context mContext, ViewGroup rootView, HdmiInFristDisplayListener mHdmiInFristDisplayListener,int type) {
        this.mContext = mContext;
        this.mHdmiInFristDisplayListener = mHdmiInFristDisplayListener;
        this.mViewType = type;
        init(rootView);
    }

    public void init(ViewGroup rootView) {
        mRootView = rootView;
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
                //SJM NO SIG mHdmieSigleView.setVisibility(isConnect ? View.GONE : View.VISIBLE);
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
        if (mPreview != null) {
            mPreview.setVisibility(View.INVISIBLE);
        }

        boolean rlt = true;
        if (mHDMIRX != null) {
            mHDMIRX.stop();

            try {
                mHDMIRX.release();
            } catch (Exception e) {
                e.printStackTrace();
            }

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
        if (mPreview == null) {
            return false;
        }
        mPreview.setVisibility(View.VISIBLE);
        mHandler.removeMessages(DISPLAY);
        Log.d(TAG, "play------------- mIsPlaying = " + isDisPlay + " mPreviewOn = " + mPreviewOn);
        if (!isDisPlay && mPreviewOn) {
            mHDMIRX = new RtkHDMIRxManager();
            HDMIRxStatus rxStatus = mHDMIRX.getHDMIRxStatus();
            if (rxStatus != null && rxStatus.status == HDMIRxStatus.STATUS_READY) {
                if (mHDMIRX.open() != 0) {
                    mWidth = 0;
                    mHeight = 0;
                    mHDMIRX = null;
                    mHandler.sendEmptyMessageDelayed(DISPLAY, DISPLAYTIME);
                    return false;
                }
                HDMIRxParameters hdmirxGetParam = mHDMIRX.getParameters();
                getSupportedPreviewSize(hdmirxGetParam, rxStatus.width, rxStatus.height);
                mFps = getSupportedPreviewFrameRate(hdmirxGetParam);
                // mScanMode = rxStatus.scanMode;

            } else {
                mHandler.sendEmptyMessageDelayed(DISPLAY, DISPLAYTIME);
                return false;
            }
            try {
                Log.d(TAG, "hdmi setPreviewSize  mViewType = " + mViewType);
                if (mViewType == TYPE_SURFACEVIEW) {
                    mHDMIRX.setPreviewDisplay(mSurfaceHolder);
                } else if (mViewType == TYPE_TEXTUREVIEW) {
                    SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
                    // mTextureView.setRotation(180);
                    mHDMIRX.setPreviewDisplay3(surfaceTexture);
                }
                // configureTargetFormat
                HDMIRxParameters hdmirxParam = new HDMIRxParameters();
                Log.d(TAG, "hdmi setPreviewSize  mWidth = " + mWidth + "  mHeight = " + mHeight + "  mFps = " + mFps);
                hdmirxParam.setPreviewSize(mWidth, mHeight);
                hdmirxParam.setPreviewFrameRate(mFps);
                // set sorce format
                mHDMIRX.setParameters(hdmirxParam);
                // configureTargetFormat end
                mHDMIRX.play();
                isDisPlay = true;
                Log.d(TAG, "hdmi mIsPlaying  successfull");
                // animation

                if (isFirstDisplay) {
                    isFirstDisplay = false;
                    if (mHdmiInFristDisplayListener != null) {
                        mHdmiInFristDisplayListener.fristDisplay();
                    }
                }
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
        RelativeLayout rooView = null;

        if (OGSystem.isTronsmart() || OGSystem.isNexus() || OGSystem.isRealOG()) {
            rooView = (RelativeLayout) mRootView.findViewById(R.id.home_ac_video_hdmi_textureView);
        } else if (OGSystem.isEmulator()) {
            Log.wtf(TAG, "Hmmm, this is not any recognized hardware. Exiting");
            //rooView = (RelativeLayout) mRootView.findViewById(R.id.home_ac_video_hdmi_textureView);
        } else {
            Log.wtf(TAG, "Hmmm, this is not any recognized hardware. Exiting");
            //rooView = (RelativeLayout) mRootView.findViewById(R.id.home_ac_video_hdmi_textureView);
        }
        //SJM NO SIG mHdmieSigleView = mRootView.findViewById(R.id.home_ac_video_hdmi_nosigle);

        if (mViewType == TYPE_SURFACEVIEW) {
            mSurfaceView = new SurfaceView(mContext);
            mSurfaceHolder = mSurfaceView.getHolder();
            mCallback = new FloatingWindowSurfaceCallback();
            mSurfaceHolder.addCallback(mCallback);
            mPreview = mSurfaceView;
        } else if (mViewType == TYPE_TEXTUREVIEW) {
            mTextureView = new TextureView(mContext);
            mListener = new FloatingWindowTextureListener();
            mTextureView.setSurfaceTextureListener(mListener);
            mPreview = mTextureView;
        }
        RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mPreview.setLayoutParams(param);
        rooView.addView(mPreview);
    }

    class FloatingWindowTextureListener implements TextureView.SurfaceTextureListener {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            mPreviewOn = true;
            Log.d(TAG, "FloatingWindowTextureListener = onSurfaceTextureAvailable");
            // play();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            // stop();
            mPreviewOn = false;
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    }

    class FloatingWindowSurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceChanged(SurfaceHolder arg0, int arg1, int width, int height) {
        }

        @Override
        public void surfaceCreated(SurfaceHolder arg0) {
            Log.d(TAG, "FloatingWindowSurfaceCallback = surfaceCreated");
            mPreviewOn = true;
            // play();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder arg0) {
            // stop();
            mPreviewOn = false;
        }
    }

    public boolean startDisPlay() {
        play();
        return false;
    }

    public boolean stopDisPlay() {
        stop();
        return false;
    }

    public boolean exit() {
        stopDisPlay();
        if (mViewType == TYPE_SURFACEVIEW) {
            if (mSurfaceView != null && mSurfaceHolder != null && mCallback != null) {
                mSurfaceHolder.removeCallback(mCallback);
            }
        }
        mContext.unregisterReceiver(mHdmiRxHotPlugReceiver);
        mHdmiRxHotPlugReceiver = null;
        Log.d(TAG, "exit");
        return false;
    }

    public void setSize(boolean isFull) {
        if (isFull) {
            RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams) mRootView.getLayoutParams();
            param.width = ViewGroup.LayoutParams.MATCH_PARENT;
            param.height = ViewGroup.LayoutParams.MATCH_PARENT;
            mRootView.setLayoutParams(param);
        } else {
            RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams) mRootView.getLayoutParams();
            param.width = (int) (640 * 1.5f);
            param.height = (int) (420 * 1.5f);
            mRootView.setLayoutParams(param);
        }
    }

    public boolean setAudio(boolean isOpenAudio) {
        try {
            if (mHDMIRX != null && isDisPlay) {
                mHDMIRX.setPlayback(true, isOpenAudio);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}