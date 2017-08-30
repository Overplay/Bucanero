package io.ourglass.bucanero.tv.Activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.ourglass.bucanero.R;
import io.ourglass.bucanero.api.BelliniDMAPI;
import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.core.OGConstants;
import io.ourglass.bucanero.core.OGHardware;
import io.ourglass.bucanero.core.OGSystem;
import io.ourglass.bucanero.core.OGSystemExceptionHander;
import io.ourglass.bucanero.messages.LaunchAppMessage;
import io.ourglass.bucanero.messages.OGLogMessage;
import io.ourglass.bucanero.messages.OnScreenNotificationMessage;
import io.ourglass.bucanero.messages.SystemCommandMessage;
import io.ourglass.bucanero.messages.SystemStatusMessage;
import io.ourglass.bucanero.tv.Fragments.OGWebViewFragment;
import io.ourglass.bucanero.tv.Fragments.OverlayFragmentListener;
import io.ourglass.bucanero.tv.Fragments.SystemInfoFragment;
import io.ourglass.bucanero.tv.STBPairing.PairDirecTVFragment;
import io.ourglass.bucanero.tv.SettingsAndSetup.DeveloperSettingsFragment;
import io.ourglass.bucanero.tv.SettingsAndSetup.SettingsFragment;
import io.ourglass.bucanero.tv.SettingsAndSetup.WelcomeFragment;
import io.ourglass.bucanero.tv.Support.Frame;
import io.ourglass.bucanero.tv.Support.OGAnimations;
import io.ourglass.bucanero.tv.Support.OGApp;
import io.ourglass.bucanero.tv.Support.Size;
import io.ourglass.bucanero.tv.VenuePairing.PairVenueFragment;
import io.ourglass.bucanero.tv.Views.HDMIView;
import io.ourglass.bucanero.tv.WiFi.WiFiPickerFragment;
import io.socket.client.Socket;

import static io.ourglass.bucanero.messages.SystemStatusMessage.SystemStatus.NETWORK_ISSUE;


public class MainFrameActivity extends BaseFullscreenActivity implements OverlayFragmentListener {

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
    SurfaceView mTVSurface;

    private HDMIView mHDMIView;

    private enum OverlayMode {NONE, SYSINFO, STBPAIR, WIFI, SETUP, OTHER, VENUEPAIR, SETTINGS, WELCOME, DEVSETTINGS}

    private OverlayMode mOverlayMode = OverlayMode.NONE;

    private boolean mBooting = true;

    public boolean mFirstTimeSetupSkipped = false;  // set this to temprarily disable the sequencing thru setup screens

    Handler mHandler = new Handler();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Log.d(TAG, "OS Level: " + OGSystem.getOsVersion());
        Log.d(TAG, "Is demo H/W? " + (OGSystem.isTronsmart() ? "YES" : "NO"));
        Log.d(TAG, "Is real OG H/W? " + (OGSystem.isRealOG() ? "YES" : "NO"));
        Log.d(TAG, "Is emulated H/W? " + (OGSystem.isEmulator() ? "YES" : "NO"));
        Log.d(TAG, "Is Nexus 10?: "+ (OGSystem.isNexus()  ? "YES" : "NO" ));

        if (OGSystem.isTronsmart()) {
            setContentView(R.layout.activity_main_frame_tronsmart);
        } else if (OGSystem.isEmulator() || OGSystem.isNexus()) {
            setContentView(R.layout.activity_main_frame_emulator);
//            ((ImageView)findViewById(R.id.bkgImage)).setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    Log.d(TAG, "Background image tapped.");
//                    launchSTBPairFragment();
//                }
//            });
        } else if (OGSystem.isRealOG()) {
            setContentView(R.layout.activity_main_frame_zidoo);
        } else {
            Log.wtf(TAG, "Hmmm, this is not any recognized hardware. Exiting");
            finish();
        }

        //SJMmTVSurface = (SurfaceView) findViewById(R.id.surfaceView);
        //SJMenableHDMISurface();

        if (OGConstants.ENABLE_RESTART_ON_UNCAUGHT_EXCEPTIONS) {
            Thread.setDefaultUncaughtExceptionHandler(new OGSystemExceptionHander(this,
                    MainFrameActivity.class));
        }

        // Register to receive messages
        ABApplication.ottobus.register(this);

        mPopupSystemMessageTV = (TextView) findViewById(R.id.textViewMsg);
        mPopupSystemMessageTV.setText("");
        mPopupSystemMessageTV.setAlpha(0);

        mOverlayFragmentHolder = (RelativeLayout) findViewById(R.id.overlayFragmentHolder);

        mMainLayout = (RelativeLayout) findViewById(R.id.mainframeLayout);
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

                if (mScreenHeight>0) {
                //    return;//SJM
                }
                // Check that the activity is using the layout version with
                // the fragment_container FrameLayout
                if (findViewById(R.id.mainframeLayout) != null) {

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
                            .add(R.id.mainframeLayout, mWidgetWebViewFrag).commit();

                    getFragmentManager().beginTransaction()
                            .add(R.id.mainframeLayout, mCrawlerWebViewFrag).commit();

                    getSavedStateFromCloud();

                }

            }
        });

        mBootBugImageView = (ImageView) findViewById(R.id.bootBugIV);
        mHDMIView = (HDMIView)findViewById(R.id.home_hdmi_parent);
        Log.d(TAG, "onCreate done");

    }

    private void enableHDMISurface() {

        if (OGSystem.isTronsmart()) {
            Log.d(TAG, "Enabling Video for ZidooX9/MStar");
            OGHardware.enableTronsmartHDMI();
        } else if (OGSystem.isRealOG()) {
            Log.d(TAG, "Enabling Video for ZidooX9S/Realtek");
        }
    }



    private void getSavedStateFromCloud() {

        BelliniDMAPI.getAppStatusFromCloud()
                .done(new DoneCallback<JSONObject>() {
                    @Override
                    public void onDone(JSONObject jsonData) {
                        Log.d(TAG, "Got app status from cloud!");
                        JSONArray runningApps = jsonData.optJSONArray("running");
                        if (runningApps != null) {
                            restoreAppState(runningApps);
                        }
                    }
                })
                .fail(new FailCallback<Exception>() {
                    @Override
                    public void onFail(Exception result) {

                        Log.e(TAG, "FAILED getting app status from cloud!");
                        OGLogMessage.newOGLog("network_issue")
                                .addFieldToMessage("description", "Failure getting saved app status in MainframeActivity")
                                .addFieldToMessage("exception", result.toString()  )
                                .addFieldToMessage("issue_code", 1004)  // this is just some BS to test the generics
                                .post();

                        SystemStatusMessage.sendStatusMessageWithException(NETWORK_ISSUE, result);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showSystemToast("Error restoring app state!");
                            }
                        });
                    }
                });


    }

    private void restoreAppState(final JSONArray runningApps) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (mWidgetWebViewFrag.getWebView()==null || mCrawlerWebViewFrag.getWebView()==null){
                    Log.e(TAG, "Trying to update app state and Activity not done booting!");
                } else {

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


            }
        });
    }

    // This has to be here or you'll crash entering Android settings!!!!!
    @Override
    public void onSaveInstanceState(Bundle outState) {
        //Do not call super class method here.
        //super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {

        super.onResume();

        mDebouncing = false;
        //showSystemToast("Starting up...");
        Log.d(TAG, "onResume done");
        enableHDMISurface();
        mHDMIView.startDisplay();
    }

    @Override
    public void onPause() {
        mHDMIView.stopDisplay();
        super.onPause();
    }

    //This needs to be here to prevent the dreaded illegal state exception
    @Override
    public void onResumeFragments(){
        super.onResumeFragments();
        // If the venueID is set, it can't be first time setup
        if (OGSystem.isFirstTimeSetup() && OGSystem.getVenueId().isEmpty()){
            mBootBugImageView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    launchWelcomeFragment();
                }
            }, 6000);
        }
    }

    public void endBoot() {

        mBooting = false;

        mBootBugImageView.animate()
                .scaleX(0f)
                .scaleY(0f)
                .rotationY(90f)
                .translationY(-1000)
                .setDuration(1000)
                .setStartDelay(3000)
                .start();

        // TODO this seems like there's a race condition with the below
        //getSavedStateFromCloud();

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

        Log.d(TAG, "Button with this code pressed: " + keyCode);

        // The remote control does not debounce and we can get multiple onKeyDown per click
        if (mDebouncing || mBooting) {
            return false;
        }
        mDebouncing = true;
        startDebounceTimer();


        // 82 = Menu button,

        Log.d(TAG, "Button with this code being processed: " + keyCode);


        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Log.d(TAG, "Pressed back button.");
            if (mOverlayMode!=OverlayMode.NONE){
                dismissOverlayFragment();
            }
            return true;
        }

        if ( keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ) {
            Log.d(TAG, "Pressed play-pause button.");
            mHDMIView.streamAudio();
        }

        // Launch settings from button 0 on remote
        if ( keyCode == KeyEvent.KEYCODE_0 ) {
            startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
            return true;
        }

//        // Button One on Remote
//        if (keyCode == 8) {
//            launchSTBPairFragment();
//        }

        // Button Two on Remote
        if ( keyCode == 9 ) {
            launchSysInfoFragment();
            return true;

        }

//        if (keyCode == 10) {
//
//            launchVenuePairFragment();
//        }

        if (keyCode == KeyEvent.KEYCODE_1 || keyCode == KeyEvent.KEYCODE_4) {
            launchSettingsFragment();
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_7){
            launchDevSettingsFragment();
            return true;
        }

//        if (keyCode == 12) {
//
//            launchWiFiFragment();
//
//        }

//        if (keyCode == 13) {
//            OGLogMessage.newHeartbeatLog().post();
//            return true;
//        }

        if ((keyCode == 16) && OGConstants.CRASH_TEST_DUMMY) {
            //int zed = 1 / 0;
        }

        return false;
    }

    public boolean dismissIfNeeded(OverlayMode mode){

        if (mOverlayMode == mode) {
            mOverlayMode = OverlayMode.NONE;
            dismissOverlayFragment();
            return true;
        }

        return false;

    }

    public void launchSTBPairFragment() {

        if (dismissIfNeeded(OverlayMode.STBPAIR))
            return;

        Log.d(TAG, "Launching TV Pair fragment");
        mOverlayMode = OverlayMode.STBPAIR;
        //SimpleHeaderTextFragment sif = SimpleHeaderTextFragment.newInstance("Hey There", "Here's the subtext", "Some shit\nI wanted\nTo say!");
        PairDirecTVFragment pdtvf = PairDirecTVFragment.newInstance();
        insertOverlayFragment(pdtvf);

    }

    public void launchSysInfoFragment() {

        if (dismissIfNeeded(OverlayMode.SYSINFO))
            return;

        Log.d(TAG, "Launching System Info fragment");
        mOverlayMode = OverlayMode.SYSINFO;
        //SimpleHeaderTextFragment sif = SimpleHeaderTextFragment.newInstance("Hey There", "Here's the subtext", "Some shit\nI wanted\nTo say!");
        SystemInfoFragment sif = SystemInfoFragment.newInstance();
        insertOverlayFragment(sif);
    }

    public void launchVenuePairFragment() {

        if (dismissIfNeeded(OverlayMode.VENUEPAIR))
            return;

        Log.d(TAG, "Launching Venue Pair fragment");
        mOverlayMode = OverlayMode.VENUEPAIR;
        PairVenueFragment sif = PairVenueFragment.newInstance();
        insertOverlayFragment(sif);
    }

    public void launchWiFiFragment() {

        if (dismissIfNeeded(OverlayMode.WIFI))
            return;

        Log.d(TAG, "Launching WiFi  fragment");
        mOverlayMode = OverlayMode.WIFI;
        WiFiPickerFragment wif = WiFiPickerFragment.newInstance();
        insertOverlayFragment(wif);
    }

    public void launchSettingsFragment() {

        if (dismissIfNeeded(OverlayMode.SETTINGS))
            return;

        Log.d(TAG, "Launching Settings  fragment");
        mOverlayMode = OverlayMode.SETTINGS;
        SettingsFragment wif = SettingsFragment.newInstance();
        insertOverlayFragment(wif);
    }

    public void launchDevSettingsFragment() {

        if (dismissIfNeeded(OverlayMode.SETTINGS))
            return;

        Log.d(TAG, "Launching Dev Settings  fragment");
        mOverlayMode = OverlayMode.DEVSETTINGS;
        DeveloperSettingsFragment wif = DeveloperSettingsFragment.newInstance();
        insertOverlayFragment(wif);
    }

    public void launchWelcomeFragment() {

        if (dismissIfNeeded(OverlayMode.WELCOME))
            return;

        Log.d(TAG, "Launching Welcome fragment");
        mOverlayMode = OverlayMode.WELCOME;
        WelcomeFragment wif = WelcomeFragment.newInstance();
        insertOverlayFragment(wif);

    }

    public void removeOverlayFragment() {

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
    public void dismissOverlayFragment() {
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

    private void showSystemToast(String message) {
        // submessage not used right now
        mPopupSystemMessageTV.setText(message);
        OGAnimations.animateAlphaTo(mPopupSystemMessageTV, 1.0f);
        mPopupSystemMessageTV.postDelayed(new Runnable() {
            @Override
            public void run() {
                OGAnimations.animateAlphaTo(mPopupSystemMessageTV, 0.0f);
            }
        }, 2000);
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

    @Subscribe
    public void inboundSystemStatusMsg(SystemStatusMessage status) {

        switch ( status.status ){
            case BOOT_COMPLETE:
                if (mBooting == true )
                    endBoot();
                break;

            case NETWORK_CONNECTED:
                getSavedStateFromCloud();
                break;

            case HDMI_RX_LOS:
                showSystemToast("HDMI input was unplugged.");
                break;

            case HDMI_RX_LINK:
                showSystemToast("HDMI plugged in.");
                break;

            case HDMI_SEVERE_ERROR:
                showSystemToast("Severe HDMI error, HDMI is probably locked by another application.");
                break;

        }


    }

    @Subscribe
    public void inboundOnScreenNotificationMessage(OnScreenNotificationMessage message) {
        //dismissOverlayFragment();
        showSystemToast(message.message);
    }

    @Subscribe
    public void inboundSystemMessage(SystemCommandMessage message){

        switch (message.status){

            case DISMISS_OVERLAY:
                dismissMe();
                break;

            case REBOOT:
                Log.d(TAG, "Received REBOOT request. Going down in 5 seconds.");
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                        pm.reboot(null);
                    }
                }, 5000);
                break;
        }

    }

    @Override
    public void dismissMe() {
        mDebouncing = false;
        //removeOverlayFragment();
        dismissOverlayFragment();
    }


}
