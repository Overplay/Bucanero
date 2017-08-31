package io.ourglass.bucanero.tv.Views;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import io.ourglass.bucanero.R;

public class HDMIView extends RelativeLayout {
    private static final String TAG = "HDMIView";
    private Context	mContext = null;
    private LayoutInflater mInflater;
    private OurglassHdmiDisplay mRealtekeHdmi = null;

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
        mRealtekeHdmi = new OurglassHdmiDisplay(mContext, rl);
    }

    public void onResume() {
        if (mRealtekeHdmi != null) {
            mRealtekeHdmi.startDisplay();
            mRealtekeHdmi.setSize(true);
        }
    }

    public void onPause() {
        if (mRealtekeHdmi != null) {
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
