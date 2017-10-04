package io.ourglass.bucanero.tv.Views;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import com.realtek.server.HDMIRxStatus;

import io.ourglass.bucanero.R;

public class HDMIView extends RelativeLayout {
    private static final String TAG = "HDMIView";
    private Context	mContext = null;
    private LayoutInflater mInflater;
    private View							mHdmiNoSignalView		= null;
    private OurglassHdmiDisplay mRealtekeHdmi = null;
    private BroadcastReceiver mHdmiRxHotPlugReceiver	= null;
    private boolean							hdmiConnectedState		= false;

    public HDMIView(Context context) {
        super(context);
        init(context);
    }

    public HDMIView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HDMIView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(21)
    public HDMIView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public void init(Context context)
    {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        View v = mInflater.inflate(R.layout.hdmi_view, this, true);
        RelativeLayout rl = (RelativeLayout) v.findViewById(R.id.home_ac_hdmi);
        //TextView tv = (TextView) v.findViewById(R.id.textView1);
        //tv.setText(" Custom RelativeLayout");
        mHdmiNoSignalView = rl.findViewById(R.id.home_ac_hdmi_nosignal);

        mRealtekeHdmi = new OurglassHdmiDisplay(mContext, rl);
    }

    private void initHdmiConnect() {

        mHdmiRxHotPlugReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                hdmiConnectedState = intent.getBooleanExtra(HDMIRxStatus.EXTRA_HDMIRX_PLUGGED_STATE, false);
                Log.v(TAG, "initHdmiConnect Connected state received is " + hdmiConnectedState);
                mHdmiNoSignalView.setVisibility(hdmiConnectedState ? View.GONE : View.VISIBLE);
                if (mRealtekeHdmi != null) {
                    if (hdmiConnectedState) {
                        mRealtekeHdmi.startDisplay();
                    } else {
                        mRealtekeHdmi.stopDisplay();
                    }
                }
            }
        };

        IntentFilter hdmiRxFilter = new IntentFilter(HDMIRxStatus.ACTION_HDMIRX_PLUGGED);

        // Read it once.
        Intent pluggedStatus = mContext.registerReceiver(null, hdmiRxFilter);
        hdmiConnectedState = pluggedStatus.getBooleanExtra(HDMIRxStatus.EXTRA_HDMIRX_PLUGGED_STATE, false);
        Log.v(TAG, "initHdmiConnect Connected state read directly is " + hdmiConnectedState);

        // Watch it.
        mContext.registerReceiver(mHdmiRxHotPlugReceiver, hdmiRxFilter);
    }

    public void onResume() {

        initHdmiConnect();

        if (mRealtekeHdmi != null) {
            mRealtekeHdmi.startDisplay();
            mRealtekeHdmi.setSize(true);
        }
    }

    public void onPause() {

        if (mHdmiRxHotPlugReceiver != null) {
            mContext.unregisterReceiver(mHdmiRxHotPlugReceiver);
            mHdmiRxHotPlugReceiver = null;
        }

        if (mRealtekeHdmi != null) {
            if (mRealtekeHdmi.isStreaming()) {
                mRealtekeHdmi.stopStreamer();
            }

            mRealtekeHdmi.stopDisplay();
        }
    }

    public void streamAudio() {
        if (mRealtekeHdmi.isStreaming()) {
            mRealtekeHdmi.stopStreamer();
        } else {
            mRealtekeHdmi.startStreamer();
        }
    }
}
