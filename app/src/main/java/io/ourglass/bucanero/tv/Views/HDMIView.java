package io.ourglass.bucanero.tv.Views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import io.ourglass.bucanero.R;

public class HDMIView extends RelativeLayout {
    private static final String TAG = "HDMIView";
    public ZidooHdmiDisPlay mRealtekeHdmi = null;
    private Context	mContext = null;
    private LayoutInflater mInflater;

    public HDMIView(Context context) {
        super(context);
        mContext = context;
        mInflater = LayoutInflater.from(context);
        init();
    }

    public HDMIView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mInflater = LayoutInflater.from(context);
        init();
    }

    public HDMIView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        mInflater = LayoutInflater.from(context);
        init();
    }

    public HDMIView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
        mInflater = LayoutInflater.from(context);
        init();
    }

    public void init()
    {
        View v = mInflater.inflate(R.layout.hdmi_view, this, true);
        RelativeLayout rl = (RelativeLayout) v.findViewById(R.id.home_ac_hdmi);
        //TextView tv = (TextView) v.findViewById(R.id.textView1);
        //tv.setText(" Custom RelativeLayout");
        mRealtekeHdmi = new ZidooHdmiDisPlay(mContext, rl);
    }

    public void startDisplay() {
        if (mRealtekeHdmi != null) {
            mRealtekeHdmi.startDisplay();
        }
    }

    public void stopDisplay() {
        if (mRealtekeHdmi != null) {
            mRealtekeHdmi.stopDisplay();
        }
    }
}
