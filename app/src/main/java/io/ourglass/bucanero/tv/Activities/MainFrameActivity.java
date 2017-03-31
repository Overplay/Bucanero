package io.ourglass.bucanero.tv.Activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import io.ourglass.bucanero.R;
import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.core.OGConstants;
import io.ourglass.bucanero.core.OGHardware;
import io.ourglass.bucanero.core.OGSystem;
import io.ourglass.bucanero.core.OGSystemExceptionHander;
import io.ourglass.bucanero.messages.LaunchAppMessage;
import io.ourglass.bucanero.tv.Fragments.OGWebViewFragment;
import io.ourglass.bucanero.tv.Support.Frame;
import io.ourglass.bucanero.tv.Support.Size;
import io.socket.client.Socket;


public class MainFrameActivity extends Activity {

    private static final String TAG = "MainFrameActivity";
    private RelativeLayout mMainLayout;
    private int mScreenWidth;
    private int mScreenHeight;
    public Socket mSocket;

    private static OGWebViewFragment mCrawlerWebViewFrag;
    private static OGWebViewFragment mWidgetWebViewFrag;

    private TextView mPopupSystemMessageTV;
    private ImageView mBootBugImageView;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main_frame);

        Thread.setDefaultUncaughtExceptionHandler(new OGSystemExceptionHander(this,
                MainFrameActivity.class));


        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Log.d(TAG, "OS Level: " + OGSystem.getOsVersion());
        Log.d(TAG, "Is demo H/W? " + (OGSystem.isTronsmart() ? "YES" : "NO"));
        Log.d(TAG, "Is real OG H/W? " + (OGSystem.isRealOG() ? "YES" : "NO"));
        Log.d(TAG, "Is emulated H/W? " + (OGSystem.isEmulator() ? "YES" : "NO"));


        // Register to receive messages
        ABApplication.ottobus.register(this);

        mPopupSystemMessageTV = (TextView) findViewById(R.id.textViewMsg);
        mPopupSystemMessageTV.setText("");
        mPopupSystemMessageTV.setAlpha(0);


        mMainLayout = (RelativeLayout) findViewById(R.id.activity_main_frame);
        Log.d(TAG, "MainframeLayout id is: " + mMainLayout.getId());
        mMainLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                Log.d(TAG, "Layout done, updating screen sizing.");
                // Need to figure out when the status bar is actually in the mix. Probably base size on x res only
                mScreenHeight = mMainLayout.getHeight(); //+getStatusBarHeight();
                mScreenWidth = mMainLayout.getWidth();
                OGSystem.setTVResolution(new Size(mScreenWidth, mScreenHeight));
                mMainLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                //onScreenInitComplete

                // Check that the activity is using the layout version with
                // the fragment_container FrameLayout
                if (findViewById(R.id.activity_main_frame) != null) {

                    // However, if we're being restored from a previous state,
                    // then we don't need to do anything and should return or else
                    // we could end up with overlapping fragments.
                    if (savedInstanceState != null) {
                        return;
                    }

                    mWidgetWebViewFrag = OGWebViewFragment.newInstance("widget", 0,
                            new Frame(new Point(0, 0), new Size(295, 600)));

                    mCrawlerWebViewFrag = OGWebViewFragment.newInstance("crawler", 0,
                            new Frame(new Point(0, 0), new Size(OGSystem.getTVResolution().width, 80)));

                    // Add the fragment to the 'fragment_container' FrameLayout
                    getFragmentManager().beginTransaction()
                            .add(R.id.activity_main_frame, mWidgetWebViewFrag).commit();

                    getFragmentManager().beginTransaction()
                            .add(R.id.activity_main_frame, mCrawlerWebViewFrag).commit();

                }

            }
        });

        enableHDMISurface();

        mBootBugImageView = (ImageView) findViewById(R.id.bootBugIV);
        Log.d(TAG, "onCreate done");

    }

    private void enableHDMISurface(){

        if (OGSystem.isEmulator()){

            Log.d(TAG, "Inserting backdrop for emulator");
            ImageView backdrop = new ImageView(this);
            backdrop.setImageResource(R.drawable.nbabackdrop);
            backdrop.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            mMainLayout.addView(backdrop, 0);


        } else if (OGSystem.isTronsmart()) {

            Log.d(TAG, "Inserting Surface for Tronsmart/Zidoo");
            SurfaceView backdrop = new SurfaceView(this);
            backdrop.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            mMainLayout.addView(backdrop, 0);
            OGHardware.enableTronsmartHDMI();

        } else if (OGSystem.isRealOG()) {


        }
    }

    @Override
    public void onResume() {

        super.onResume();
        mBootBugImageView.animate()
                .scaleX(0f)
                .scaleY(0f)
                .rotationY(90f)
                .setDuration(1000)
                .setStartDelay(5000)
                .start();

        Log.d(TAG, "onResume done");


    }

    /**
     * This was copied over from AB as placeholder
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        // 82 = Menu button,

        if (keyCode == 82 || keyCode == 41) {
            //toggleAppMenu();
            return false;
        }

        // Launch settings from button 0 on remote
        if ( keyCode == 7 || keyCode == 4 ){
            startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
        }

        if (keyCode == 8){
//            Intent intent = new Intent(this, SetTopBoxPairActivity.class);
//            startActivity(intent);
        }

        if ((keyCode == 12) && OGConstants.CRASH_TEST_DUMMY){
            int zed = 1/0;
        }

        return false;
    }

    @Subscribe
    public void inboundLaunch(LaunchAppMessage launchMsg) {
        Log.d(TAG, "Got a launch message, yo!");

        OGWebViewFragment target = launchMsg.appType.equalsIgnoreCase("widget") ?
                mWidgetWebViewFrag : mCrawlerWebViewFrag;

        target.launchApp(launchMsg.appId);
        target.setSizeAsPctOfScreen(launchMsg.appSize);


    }


}
