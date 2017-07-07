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
import android.widget.Toast;

import com.realtek.hardware.RtkHDMIRxManager;
import com.realtek.hardware.RtkHDMIRxManager.Size;
import com.realtek.server.HDMIRxParameters;
import com.realtek.server.HDMIRxStatus;

import java.io.IOException;
import java.util.List;

/****************************
 *
 *
 *  D E P R E C A T E D
 *
 *  Here for reference only
 *
 ****************************/

@Deprecated
public class HDMIRxPlayer
{
    private final String TAG = "HDMIRxPlayer";
    private RtkHDMIRxManager mHDMIRX = null;
    private SurfaceView mSurfaceView = null;
    private SurfaceHolder mSurfaceHolder = null;
    private HDMIRXSurfaceCallback HDMIRXCallback = new HDMIRXSurfaceCallback();
    private Context mContext;
    private final Handler mHandler = new Handler();
    private boolean mIsPlaying = false;
    private int mPlayCount = 0;
    private int mWidth = 0;
    private int mHeight = 0;
    private int mFps = 0;
    private int mScanMode = 0;
    private Toast mToast;
    private String mToastStr;

    private final Runnable mPlay = new Runnable() {
        @Override
        public void run() {
            if(mPlayCount++<4)
                play();
        }
    };

    private final Runnable mShowToast = new Runnable() {
        @Override
        public void run() {
            if(mToast != null)
                mToast.cancel();
            mToast = Toast.makeText(mContext, mToastStr, Toast.LENGTH_LONG);
            mToast.show();
        }
    };

    private BroadcastReceiver hdmiRxHotPlugReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
                boolean hdmiRxPlugged = intent.getBooleanExtra(HDMIRxStatus.EXTRA_HDMIRX_PLUGGED_STATE, false);
                if(hdmiRxPlugged) {
                    mPlayCount=0;
                    play();
                    Log.d(TAG, "HDMI Rx is plugged in ( "+ mWidth + "x" + mHeight +" )\n");
                } else {
                    stop();
                    Log.d(TAG, "HDMI Rx is pulled out\n");
                }
        }
    };

    private BroadcastReceiver hdmiRxHotPlugReceiverPassive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean hdmiRxPlugged = intent.getBooleanExtra(HDMIRxStatus.EXTRA_HDMIRX_PLUGGED_STATE, false);
            if(hdmiRxPlugged) {
                //mPlayCount=0;
                play();
                Log.d(TAG, "HDMI Rx is plugged in ( "+ mWidth + "x" + mHeight +" )\n");
            } else {
                //stop();
                Log.d(TAG, "HDMI Rx is pulled out\n");
            }
        }
    };

    public HDMIRxPlayer(Context context, SurfaceView sv, int width, int height) {
        mContext = context;
        mSurfaceView = sv;
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(HDMIRXCallback);
        mSurfaceHolder.setFixedSize(width, height);
        mSurfaceView.setZOrderOnTop(false);
        mSurfaceView.setVisibility(View.VISIBLE);

        Log.d(TAG, "Setting up mHDMIRX");
        mHDMIRX = new RtkHDMIRxManager();

        IntentFilter hdmiRxFilter = new IntentFilter(HDMIRxStatus.ACTION_HDMIRX_PLUGGED);
        mContext.registerReceiver(hdmiRxHotPlugReceiverPassive, hdmiRxFilter);
    }

    public HDMIRxPlayer(Context context, ViewGroup parent, int width, int height)
    {
        mContext = context;
        mSurfaceView = new SurfaceView(context);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(HDMIRXCallback);
        mSurfaceHolder.setFixedSize(width, height);
        parent.addView(mSurfaceView);
        mSurfaceView.setZOrderOnTop(false);
        //mSurfaceView.setVisibility(View.VISIBLE);

        mHDMIRX = new RtkHDMIRxManager();

        IntentFilter hdmiRxFilter = new IntentFilter(HDMIRxStatus.ACTION_HDMIRX_PLUGGED);
        mContext.registerReceiver(hdmiRxHotPlugReceiverPassive, hdmiRxFilter);
    }

    public boolean isPlaying()
    {
        return mIsPlaying;
    }

    //Implementation for HDMIRxHandlerInterface
    public boolean play()
    {
        mSurfaceView.setVisibility(View.VISIBLE);

        if (mIsPlaying==false && HDMIRXCallback.SurfaceIsReady==true)
        {
            HDMIRxStatus rxStatus = mHDMIRX.getHDMIRxStatus();
            if (rxStatus!=null && rxStatus.status ==HDMIRxStatus.STATUS_READY){
                if (mHDMIRX.open()!=0){
                    mWidth = 0;
                    mHeight = 0;
                    Log.d(TAG, "open HDMI RX failed.");
                    mHandler.postDelayed(mPlay, 500);
                    return false;
                }
                HDMIRxParameters hdmirxGetParam = mHDMIRX.getParameters();
                getSupportedPreviewSize(hdmirxGetParam, rxStatus.width, rxStatus.height);
                mFps=getSupportedPreviewFrameRate(hdmirxGetParam);
                mScanMode=rxStatus.scanMode;
            }
            else{
                Log.d(TAG, "HDMIRX not ready");
                mHandler.postDelayed(mPlay, 500);
                return false;
            }
            try{
                mHDMIRX.setPreviewDisplay(mSurfaceHolder);
                HDMIRxParameters hdmirxParam = new HDMIRxParameters();
                hdmirxParam.setPreviewSize(mWidth, mHeight);
                hdmirxParam.setPreviewFrameRate(mFps);
                //hdmirxParam.setPreviewFrameRate(30);
                mHDMIRX.setParameters(hdmirxParam);
                mHDMIRX.play();
                mIsPlaying = true;
                Log.d(TAG, "play HDMI RX");
                showStatusToast(true);
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        else if(HDMIRXCallback.SurfaceIsReady==false)
        {
            Log.d(TAG, "Surface is not ready !");
            mHandler.postDelayed(mPlay, 500);
            return false;
        }
        else
        {
            Log.d(TAG, "It's playing");
            return false;
        }
        return true;
    }
    public boolean stop()
    {
        boolean rlt = true;
        mSurfaceView.setVisibility(View.INVISIBLE);
        if (mIsPlaying==true)
        {
            mIsPlaying = false;
            Log.d(TAG, "stop HDMI RX");
            mHDMIRX.stop();
            showStatusToast(true);
        }
        else
        {
            rlt =false;
        }
        mWidth = 0;
        mHeight = 0;
        mFps = 0;
        return rlt;
    }
    
    public byte[] capture()
    {
        byte[] data = capture(RtkHDMIRxManager.HDMIRX_FORMAT_ARGB, 0, 0, getHdmiInWidth(), getHdmiInHeight(), 320, 180);
        return data;
    }

    public byte[] capture(int format, int x, int y, int cropWidth, int cropHeight, int outWidth, int outHeight)
    {
        //Log.d(TAG, "Capture fmt:" + format + " (" + x + "," + y + "), " + cropWidth + "x" + cropHeight + " => " + outWidth + "x" + outHeight);
        if(mHDMIRX==null || mIsPlaying==false)
            return null;
        if(x < 0 || y < 0)
            return null;
        if(cropWidth <= 0 || cropHeight <= 0 || cropWidth > getHdmiInWidth() || cropHeight > getHdmiInHeight())
            return null;
        if(outWidth <= 0 || outHeight <= 0)
            return null;
        if(x + cropWidth > getHdmiInWidth())
            cropWidth = getHdmiInWidth() - x;
        if(x + cropHeight > getHdmiInHeight())
            cropHeight = getHdmiInHeight() - y;
        byte[] data = mHDMIRX.getSnapshot(format, x, y, cropWidth, cropHeight, outWidth, outHeight);
        return data;
    }

    public void release()
    {
        mContext.unregisterReceiver(hdmiRxHotPlugReceiver);
        mSurfaceHolder.removeCallback(HDMIRXCallback);
        mHandler.removeCallbacks(mShowToast);
        if(mToast != null)
            mToast.cancel();
        if (mHDMIRX!= null)
        {
            mHDMIRX.release();
            mHDMIRX = null;
        }
    } 

    public void setSurfaceSize(int width, int height)
    {
        mSurfaceHolder.setFixedSize(width, height);
    }

    public int getHdmiInWidth()
    {
        return mWidth;
    } 

    public int getHdmiInHeight()
    {
        return mHeight;
    } 

    public int getHdmiInFps()
    {
        return mFps;
    }

    public int getHdmiInScanMode()
    {
        return mScanMode;
    }

    public void showStatusToast(boolean delay)
    {
        String str;
        if(isPlaying())
        {
            str = "OG HDMI In ( "+ getHdmiInWidth() + "x" + getHdmiInHeight() +", "+getHdmiInFps()+"fps, ";
            if(getHdmiInScanMode()==HDMIRxStatus.SCANMODE_INTERLACED)
                str += "Interlace )";
            else
                str += "Progressive )";
            showToast(str, 0);
        }
        else
        {
            str = "No Signal";
            if(delay==false)
                showToast(str, 0);
            else
                showToast(str, 5000);
        }
    }

    public void showToast(String str, int delay)
    {
        mHandler.removeCallbacks(mShowToast);
        mToastStr = str;
        mHandler.postDelayed(mShowToast, delay);
    }

    private void getSupportedPreviewSize(HDMIRxParameters hdmirxGetParam, int rxWidth, int rxHeight) {
        List<Size> previewSizes = hdmirxGetParam.getSupportedPreviewSizes();
        int retWidth=0, retHeight=0;
        if(previewSizes==null || previewSizes.size()<=0)
            return;
        for(int i=0;i<previewSizes.size();i++) {
            if(previewSizes.get(i)!=null && rxWidth==previewSizes.get(i).width) {
                retWidth=previewSizes.get(i).width;
                retHeight=previewSizes.get(i).height;
                if(rxHeight==previewSizes.get(i).height)
                    break;
            }
        }
        if(retWidth==0 && retHeight==0) {
            if(previewSizes.get(previewSizes.size()-1)!=null) {
                retWidth=previewSizes.get(previewSizes.size()-1).width;
                retHeight=previewSizes.get(previewSizes.size()-1).height;
            }
        }
        mWidth = retWidth;
        mHeight = retHeight;
    }

    private int getSupportedPreviewFrameRate(HDMIRxParameters hdmirxGetParam) {
        List<Integer> previewFrameRates = hdmirxGetParam.getSupportedPreviewFrameRates();
        int fps=0;
        if(previewFrameRates!=null && previewFrameRates.size()>0)
            fps = previewFrameRates.get(previewFrameRates.size()-1);
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
            mPlayCount=0;
            play();
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
