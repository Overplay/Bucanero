package io.ourglass.bucanero.tv.Activities;

import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.ourglass.bucanero.R;
import io.ourglass.bucanero.api.BelliniDMAPI;
import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.core.JSONCallback;
import io.ourglass.bucanero.core.OGConstants;
import io.ourglass.bucanero.core.OGHardware;
import io.ourglass.bucanero.core.OGSystem;
import io.ourglass.bucanero.core.OGSystemExceptionHander;
import io.ourglass.bucanero.messages.LaunchAppMessage;
import io.ourglass.bucanero.tv.Fragments.OGWebViewFragment;
import io.ourglass.bucanero.tv.STBPairing.PairDirecTVFragment;
import io.ourglass.bucanero.tv.Fragments.SystemInfoFragment;
import io.ourglass.bucanero.tv.Support.Frame;
import io.ourglass.bucanero.tv.Support.OGApp;
import io.ourglass.bucanero.tv.Support.Size;
import io.socket.client.Socket;


public class MainFrameActivity extends FragmentActivity  {

    private static final String TAG = "MainFrameActivity";
    private RelativeLayout mMainLayout;
    private int mScreenWidth;
    private int mScreenHeight;
    public Socket mSocket;
    private boolean mDebouncing = false;

    private static OGWebViewFragment mCrawlerWebViewFrag;
    private static OGWebViewFragment mWidgetWebViewFrag;

    private TextView mPopupSystemMessageTV;
    private ImageView mBootBugImageView;
    RelativeLayout mOverlayFragmentHolder;


    private enum OverlayMode {NONE, SYSINFO, STBPAIR, WIFI, SETUP, OTHER}

    private OverlayMode mOverlayMode = OverlayMode.NONE;


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

        mOverlayFragmentHolder = (RelativeLayout) findViewById(R.id.overlayFragmentHolder);


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

                    getSavedStateFromCloud();

                }

            }
        });

        enableHDMISurface();

        mBootBugImageView = (ImageView) findViewById(R.id.bootBugIV);
        Log.d(TAG, "onCreate done");

    }

    private void enableHDMISurface() {

        if (OGSystem.isEmulator()) {

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

    private void getSavedStateFromCloud() {

        BelliniDMAPI.getAppStatusFromCloud(new JSONCallback() {
            @Override
            public void jsonCallback(JSONObject jsonData) {
                Log.d(TAG, "Got app status from cloud!");
                JSONArray runningApps = jsonData.optJSONArray("running");
                if (runningApps != null) {
                    restoreAppState(runningApps);
                }
            }

            @Override
            public void error(Error err) {
                //TODO some intelligent handling!
                Log.e(TAG, "FAILED getting app status from cloud!");

            }
        });
    }

    private void restoreAppState(final JSONArray runningApps) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                for (int i = 0; i < runningApps.length(); i++) {
                    try {
                        JSONObject app = runningApps.getJSONObject(i);
                        OGApp ogApp = new OGApp(app);
                        OGWebViewFragment target = ogApp.appType.equalsIgnoreCase("widget") ?
                                mWidgetWebViewFrag : mCrawlerWebViewFrag;
                        target.launchApp(ogApp.appId);
                        target.setSizeAsPctOfScreen(ogApp.getSize());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
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

        mDebouncing = false;


        Log.d(TAG, "onResume done");


    }

    @Override
    public void onPause() {

        super.onPause();
    }

    private void endDebounce() {
        mDebouncing = false;
    }

    private void startDebounceTimer() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                endDebounce();
            }
        }).start();

    }

    /**
     * This was copied over from AB as placeholder
     *
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        // The remote control does not debounce and we can get multiple onKeyDown per click
        if (mDebouncing) {
            return false;
        }
        mDebouncing = true;
        startDebounceTimer();


        // 82 = Menu button,

        Log.d(TAG, "Button with this code pressed: " + keyCode);

        if (keyCode == 82 || keyCode == 41) {
            //toggleAppMenu();
        }

        // Launch settings from button 0 on remote
        if (keyCode == 7 || keyCode == 4) {
            startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS),0);
        }

        if (keyCode == 8) {
            launchSTBPairFragment();
        }

        if (keyCode == 9) {
            launchSysInfoFragment();
        }

        if (keyCode == 10) {
            Log.d(TAG, "Dissmissing overlay fragment");
            dismissOverlayFragment();
        }

        if (keyCode == 11) {

            //recordAudio("testaudio.mp3");

        }

        if ((keyCode == 12) && OGConstants.CRASH_TEST_DUMMY) {
            //int zed = 1 / 0;
        }

        return false;
    }


    private void launchSTBPairFragment() {

        if (mOverlayMode == OverlayMode.STBPAIR) {
            mOverlayMode = OverlayMode.NONE;
            dismissOverlayFragment();
            return;
        }

        Log.d(TAG, "Launching TV Pair fragment");
        mOverlayMode = OverlayMode.STBPAIR;
        //SimpleHeaderTextFragment sif = SimpleHeaderTextFragment.newInstance("Hey There", "Here's the subtext", "Some shit\nI wanted\nTo say!");
        PairDirecTVFragment pdtvf = PairDirecTVFragment.newInstance();
        insertOverlayFragment(pdtvf);

    }

    private void launchSysInfoFragment() {

        if (mOverlayMode == OverlayMode.SYSINFO) {
            mOverlayMode = OverlayMode.NONE;
            dismissOverlayFragment();
            return;
        }

        Log.d(TAG, "Launching System Info fragment");
        mOverlayMode = OverlayMode.SYSINFO;
        //SimpleHeaderTextFragment sif = SimpleHeaderTextFragment.newInstance("Hey There", "Here's the subtext", "Some shit\nI wanted\nTo say!");
        SystemInfoFragment sif = SystemInfoFragment.newInstance();
        insertOverlayFragment(sif);
    }

    private void removeOverlayFragment() {

        Fragment oldFrag = getSupportFragmentManager()
                .findFragmentById(R.id.overlayFragmentHolder);

        if (oldFrag != null) {
            FragmentTransaction ft = getSupportFragmentManager()
                    .beginTransaction();

            ft.setCustomAnimations(R.anim.overlay_frag_in, R.anim.overlay_frag_out);
            ft.remove(oldFrag)
                    .commit();
        }

    }

    // Call this when you aren't doing a replace in order to get the widgets to 100% alpha
    private void dismissOverlayFragment() {
        removeOverlayFragment();
        mCrawlerWebViewFrag.fadeIn();
        mWidgetWebViewFrag.fadeIn();
    }

    private void insertOverlayFragment(Fragment fragment) {

        mCrawlerWebViewFrag.halfFade();
        mWidgetWebViewFrag.halfFade();

        removeOverlayFragment();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.overlay_frag_in, R.anim.overlay_frag_out);
        ft.add(R.id.overlayFragmentHolder, fragment).commit();

        mOverlayFragmentHolder.bringToFront();

    }

    @Subscribe
    public void inboundLaunch(LaunchAppMessage launchMsg) {
        Log.d(TAG, "Got a launch message, yo!");

        OGWebViewFragment target = launchMsg.appType.equalsIgnoreCase("widget") ?
                mWidgetWebViewFrag : mCrawlerWebViewFrag;

        target.launchApp(launchMsg.appId);
        target.setSizeAsPctOfScreen(launchMsg.appSize);

        //TODO add slot info
        BelliniDMAPI.appLaunchAck(launchMsg.appId, 0);


    }



}
