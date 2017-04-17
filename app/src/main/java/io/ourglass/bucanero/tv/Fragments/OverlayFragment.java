package io.ourglass.bucanero.tv.Fragments;

import android.content.Context;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;


/**
 * Created by mkahn on 4/17/17.
 */

public abstract class OverlayFragment extends Fragment {

    private static final String TAG = "OverlayFragment";
    private OverlayFragmentListener mListener;
    protected Handler mHandler = new Handler();


    protected void dismissMe(){
        if (mListener!=null){
            mListener.dismissMe();
        }
    }

    /**
     * Stoppable dismiss timer. Good for stuff that does not have interaction. To stop the timer,
     * call cancelRunnables().
     * @param delayMs
     */
    protected void dismissMeAfter(final int delayMs){

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dismissMe();
            }
        }, delayMs);

    }

    protected void cancelRunnables(){
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OverlayFragmentListener) {
            mListener = (OverlayFragmentListener) context;
        } else {
            Log.wtf(TAG, "This overlay should be pushed from an instance of OverlayFragListener, but was not!");
        }
    }

    @Override
    public void onDetach(){
        cancelRunnables();
        super.onDetach();
    }
}
