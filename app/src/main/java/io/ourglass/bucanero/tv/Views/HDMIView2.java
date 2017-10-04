package io.ourglass.bucanero.tv.Views;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import io.ourglass.bucanero.R;

public class HDMIView2 extends RelativeLayout {
    private static final String TAG = "HDMIView2";
    private Context mContext = null;
    private LayoutInflater mInflater;
    private View mHdmiNoSignalView = null;
    private OurglassHdmiDisplay2 mOGHdmiDisp = null;
    private boolean hdmiConnectedState = false;

    RelativeLayout mHdmiHolder;
    TextView mHdmiErrorTextView;
    HDMIViewListener mListener;

    public interface HDMIViewListener {
        public void ready();
        public void error(OurglassHdmiDisplay2.OGHdmiError error);
    }

    public HDMIView2(Context context) {
        super(context);
        init(context);
    }

    public HDMIView2(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HDMIView2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(21)
    public HDMIView2(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public void init(Context context) {

        mContext = context;
        mInflater = LayoutInflater.from(context);
        View v = mInflater.inflate(R.layout.hdmi_view2, this, true);
        RelativeLayout rl = (RelativeLayout) v.findViewById(R.id.home_ac_hdmi);
        mHdmiHolder = (RelativeLayout) v.findViewById(R.id.home_ac_hdmi_textureView);

        //TextView tv = (TextView) v.findViewById(R.id.textView1);
        //tv.setText(" Custom RelativeLayout");
        mHdmiNoSignalView = rl.findViewById(R.id.home_ac_hdmi_nosignal);
        mHdmiErrorTextView = (TextView)v.findViewById(R.id.home_ac_hdmi_nosignal_text_view);

    }

    public void start(HDMIViewListener listener){

        mListener = listener;

        mOGHdmiDisp = new OurglassHdmiDisplay2(mContext, mHdmiHolder, new OurglassHdmiDisplay2.OGHdmiListener() {
            @Override
            public void error(OurglassHdmiDisplay2.OGHdmiError error, String msg) {

                Log.e(TAG, "HDMI error: " + error.name());
                Log.e(TAG, "HDMI error msg: " + msg);

                switch (error){

                    case HDMI_CANT_OPEN_DRIVER:
                        // this is a fucking, we're done
                        mHdmiErrorTextView.setText("CAN'T ACQUIRE DRIVER");
                        break;

                    default:
                        mHdmiErrorTextView.setText(error.name());
                }

                mHdmiHolder.setVisibility(View.INVISIBLE);
                mHdmiErrorTextView.setVisibility(View.VISIBLE);
            }

            @Override
            public void hdmiStateChange(OurglassHdmiDisplay2.OGHdmiState state) {

                Log.d(TAG, "HDMI state change: " + state.name());

            }
        });

        mOGHdmiDisp.initHDMIDriver();

    }



    public void resume() {

        if (mOGHdmiDisp != null) {
            mOGHdmiDisp.play();
            //mOGHdmiDisp.setSize(true);
        }
    }

    public void pause() {

        if (mOGHdmiDisp != null) {
            if (mOGHdmiDisp.isStreaming()) {
                mOGHdmiDisp.stopStreamer();
            }

            mOGHdmiDisp.pause();
        }
    }

    public void destroy(){

        if (mOGHdmiDisp != null) {
            if (mOGHdmiDisp.isStreaming()) {
                mOGHdmiDisp.stopStreamer();
            }

            mOGHdmiDisp.kill();
        }

    }

    public void streamAudio() {
        if (mOGHdmiDisp.isStreaming()) {
            mOGHdmiDisp.stopStreamer();
        } else {
            mOGHdmiDisp.startStreamer();
        }
    }
}
