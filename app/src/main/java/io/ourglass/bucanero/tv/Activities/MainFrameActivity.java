package io.ourglass.bucanero.tv.Activities;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
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

import java.util.ArrayList;
import java.util.Date;

import io.ourglass.bucanero.R;
import io.ourglass.bucanero.Support.OGShellE;
import io.ourglass.bucanero.api.BelliniDMAPI;
import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.core.MemInfo;
import io.ourglass.bucanero.core.OGConstants;
import io.ourglass.bucanero.core.OGHardware;
import io.ourglass.bucanero.core.OGSettings;
import io.ourglass.bucanero.core.OGSystem;
import io.ourglass.bucanero.messages.LaunchAppMessage;
import io.ourglass.bucanero.messages.OGLogMessage;
import io.ourglass.bucanero.messages.OnScreenNotificationMessage;
import io.ourglass.bucanero.messages.SystemCommandMessage;
import io.ourglass.bucanero.messages.SystemStatusMessage;
import io.ourglass.bucanero.services.FFmpeg.FFmpegBinary;
import io.ourglass.bucanero.services.FFmpeg.FFmpegBinaryService;
import io.ourglass.bucanero.services.STB.STBPollingWorker;
import io.ourglass.bucanero.tv.Fragments.OGWebViewFragment;
import io.ourglass.bucanero.tv.Fragments.OverlayFragmentListener;
import io.ourglass.bucanero.tv.Fragments.SystemInfoFragment;
import io.ourglass.bucanero.tv.HDMI.HDMIStateException;
import io.ourglass.bucanero.tv.HDMI.HDMIView;
import io.ourglass.bucanero.tv.HDMI.RtkHdmiWrapper;
import io.ourglass.bucanero.tv.STBPairing.PairDirecTVFragment;
import io.ourglass.bucanero.tv.SettingsAndSetup.DeveloperSettingsFragment;
import io.ourglass.bucanero.tv.SettingsAndSetup.SettingsFragment;
import io.ourglass.bucanero.tv.SettingsAndSetup.WelcomeFragment;
import io.ourglass.bucanero.tv.Support.Frame;
import io.ourglass.bucanero.tv.Support.OGAnimations;
import io.ourglass.bucanero.tv.Support.OGApp;
import io.ourglass.bucanero.tv.Support.Size;
import io.ourglass.bucanero.tv.VenuePairing.PairVenueFragment;
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

    Date startedAtDate;

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

    // Replacing Android Services;
    STBPollingWorker stbPoller;



    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        startedAtDate = new Date();

        Log.d(TAG, "OS Level: " + OGSystem.getOsVersion());
        Log.d(TAG, "Is demo H/W? " + (OGSystem.isTronsmart() ? "YES" : "NO"));
        Log.d(TAG, "Is real OG H/W? " + (OGSystem.isRealOG() ? "YES" : "NO"));
        Log.d(TAG, "Is emulated H/W? " + (OGSystem.isEmulator() ? "YES" : "NO"));
        Log.d(TAG, "Is Nexus 10?: "+ (OGSystem.isNexus()  ? "YES" : "NO" ));

        if (OGSystem.isTronsmart()) {
            setContentView(R.layout.activity_main_frame_tronsmart);
        } else if (OGSystem.isEmulator() || OGSystem.isNexus()) {
            setContentView(R.layout.activity_main_frame_emulator);

        } else if (OGSystem.isRealOG()) {

            setContentView(R.layout.activity_main_frame_zidoo_og);
            mHDMIView = (HDMIView)findViewById(R.id.home_hdmi_parent);

            mHDMIView.setDebugMode(OGSettings.getHDMIDebugOverlayMode());

            try {
                mHDMIView.prepareAuto(new HDMIView.HDMIViewListener() {
                    @Override
                    public void surfaceReady() {
                        Log.d(TAG, "HDMI Surface ready");
                    }

                    @Override
                    public void ready() {
                        Log.d(TAG, "HDMI Driver ready");
                    }

                    @Override
                    public void error(RtkHdmiWrapper.OGHdmiError error) {
                        Log.e(TAG, "HDMI Driver error: " + error.name());
                        if (error== RtkHdmiWrapper.OGHdmiError.HDMI_CANT_OPEN_DRIVER){
                            Log.wtf(TAG, "We're fucked, need to reboot. HDMI driver is locked.");
                            rebootIn(5000);

                        }

                    }

                    @Override
                    public void hdmiLOS() {
                        Log.d(TAG, "We've lost the HDMI input for more than one second.");
                    }

                    @Override
                    public void hdmiActive() {
                        Log.d(TAG, "HDMI has a signal.");
                    }

                });
            } catch (HDMIStateException e) {
                // TODO Handle this somehow
                e.printStackTrace();
                Log.wtf(TAG, "Unrecoverable error prepping HDMIView! Error: " + e.getMessage());
            }

        } else {
            Log.wtf(TAG, "Hmmm, this is not any recognized hardware. Exiting");
            finish();
        }

        // Register to receive messages
        ABApplication.ottobus.register(this);

        mPopupSystemMessageTV = (TextView) findViewById(R.id.textViewSystemMsg);
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

                    endBoot();

                }

            }
        });

        mBootBugImageView = (ImageView) findViewById(R.id.bootBugIV);



        //WAG at releasing HDMI when shit really gets fucked
        if (OGConstants.ENABLE_RESTART_ON_UNCAUGHT_EXCEPTIONS) {
            Log.d(TAG, "Should be catching all exceptions, commented out");
            rebootIn(100);
        }

        Log.d(TAG, "onCreate done");

    }

    private void rebootIn(int milliSecs){

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                OGShellE.execRoot("reboot", new OGShellE.OGShellEListener() {
                    @Override
                    public void stdout(ArrayList<String> results) {
                        Log.d(TAG, "Reboot responded with stdout: " + results);
                    }

                    @Override
                    public void stderr(ArrayList<String> errors) {
                        Log.d(TAG, "Reboot responded with stderr: " + errors);

                    }

                    @Override
                    public void fail(Exception e) {
                        Log.wtf(TAG, "Could not reboot");
                    }
                });
            }
        }, milliSecs);


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

        // TODO this is kind of gross, the webviewFrags should grab from a central place
        mHandler.postDelayed(new Runnable() {
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
        }, 5000);

//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//
//                if (mWidgetWebViewFrag.getWebView()==null || mCrawlerWebViewFrag.getWebView()==null){
//                    Log.e(TAG, "Trying to update app state and Activity not done booting!");
//                } else {
//
//                    for (int i = 0; i < runningApps.length(); i++) {
//                        try {
//                            JSONObject app = runningApps.getJSONObject(i);
//                            OGApp ogApp = new OGApp(app);
//                            OGWebViewFragment target = ogApp.appType.equalsIgnoreCase("widget") ?
//                                    mWidgetWebViewFrag : mCrawlerWebViewFrag;
//                            target.launchApp(ogApp.appId);
//                            target.setSizeAsPctOfScreen(ogApp.getSize());
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                }
//
//
//            }
//        });

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

        // Moved here for better process control: MAK 9-2017
        stbPoller = new STBPollingWorker();
        stbPoller.start();

        if (OGConstants.FFMPEG_START_AS_SERVICE){
            Log.d(TAG, "Loading FFmpegBinary as service.");
            Intent ffmpegBinaryIntent = new Intent(this, FFmpegBinaryService.class);
            startService(ffmpegBinaryIntent);
        } else {
            Log.d(TAG, "Loading FFmpegBinary by static methods.");
            FFmpegBinary.load();
        }


        mDebouncing = false;
        //showSystemToast("Starting up...");
        Log.d(TAG, "onResume done");
        enableHDMISurface();
        //mHDMIView.startDisplay();

        if (OGConstants.USE_LOCAL_DM_SERVER){
            Log.d(TAG, "Using local server, throwing up a message!");
            TextView tv = (TextView)findViewById(R.id.textViewWarning);
            tv.setVisibility(View.VISIBLE);
            tv.setText("USING LOCAL BELLINI-DM @ "+OGConstants.BELLINI_DM_LAN_LOCAL_ADDRESS);
        }


        if (OGConstants.AUTO_START_AUDIO_STREAMER){

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ABApplication.dbToast("Starting Audio Stream");
                    mHDMIView.streamAudio();
                }
            }, 5000);

        }

        mHDMIView.activityResume();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                MemInfo method1 = OGSystem.getOSMemory();
                MemInfo method2 = OGSystem.getOSMemory2();
                CharSequence time = DateUtils.getRelativeTimeSpanString(startedAtDate.getTime());

                StringBuilder sb = new StringBuilder();
                sb.append(time+ "\n\n");
                sb.append("Method 1\n");
                sb.append("Total: " + method1.totalMegs + "\n");
                sb.append("Free: " + method1.availableMegs + "\n");
                sb.append("% free: " + method1.getAvailablePctString()+ "\n\n");
                sb.append("Method 2\n");
                sb.append("Total: " + method2.totalMegs + "\n");
                sb.append("Free: " + method2.availableMegs + "\n");
                sb.append("% free: " + method2.getAvailablePctString()+ "\n");
                sb.append("Low Mem?: " + method2.lowMemory + "\n\n");

                ActivityManager.RunningAppProcessInfo rapi = new ActivityManager.RunningAppProcessInfo();
                ActivityManager.getMyMemoryState(rapi);

                sb.append("Last trim lvl: " + rapi.lastTrimLevel);

                mHDMIView.setGPMessage(sb.toString());

                mHandler.postDelayed(this, 5000);
            }
        }, 1000);


        // This sends a boradcast to Wort every 5000 ms if MFA is alive
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                kickWDT();
                mHandler.postDelayed(this, 5000);
            }
        }, 5000);

    }

    @Override
    public void onPause() {
        mHandler.removeCallbacksAndMessages(null);
        stbPoller.stop();
        mHDMIView.activityPause();

        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    //This needs to be here to prevent the dreaded illegal state exception
    @Override
    public void onResumeFragments(){
        super.onResumeFragments();
        // If the venueID is set, it can't be first time setup
        if (OGSystem.isFirstTimeSetup() && OGSystem.getVenueUUID().isEmpty()){
            mBootBugImageView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    launchWelcomeFragment();
                }
            }, 6000);
        }
    }

    @Override
    public void onTrimMemory(int level){
        mHDMIView.addDebugMessage("Mem Trim: "+level);
        Log.d(TAG, "Memory trim requested at level: " + level);
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

        if ( keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE && !OGConstants.AUTO_START_AUDIO_STREAMER) {
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

        if ((keyCode == 16) && OGConstants.CRASH_TEST_DUMMY) {
            //int zed = 1 / 0;
        }

//        if (keyCode == KeyEvent.KEYCODE_5) {
//            ABApplication.dbToast("Starting HDMI PLayback");
//            mHDMIView.resume();
//        }
//
//        if (keyCode == KeyEvent.KEYCODE_8) {
//            ABApplication.dbToast("Pausing HDMI PLayback");
//            mHDMIView.pause();
//        }
//
//        if (keyCode == KeyEvent.KEYCODE_6) {
//            ABApplication.dbToast("Restarting HDMI Playback");
//            mHDMIView.start(new HDMIView2.HDMIViewListener() {
//                @Override
//                public void ready() {
//                    Log.d(TAG, "HDMIView reports ready, starting it.");
//                    mHDMIView.resume();
//                }
//
//                @Override
//                public void error(OurglassHdmiDisplay2.OGHdmiError error) {
//                    Log.e(TAG, "Error initting HDMIView");
//                }
//
//            });
//        }
//
//        if (keyCode == KeyEvent.KEYCODE_9) {
//            ABApplication.dbToast("Killing HDMI Playback");
//            mHDMIView.destroy();
//        }

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

    private void showSystemToast(final String message) {
        // submessage not used right now
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPopupSystemMessageTV.setVisibility(View.VISIBLE);
                mPopupSystemMessageTV.setText(message);
                OGAnimations.animateAlphaTo(mPopupSystemMessageTV, 1.0f);
            }
        });

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

        BelliniDMAPI.appLaunchAck(launchMsg.appId, target.mLayoutSlot);

    }

    @Subscribe
    public void inboundSystemStatusMsg(SystemStatusMessage status) {

        switch ( status.status ){
            // Not used any more
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

            case SHOW_HDMI_DEBUG_LAYER:
                Log.d(TAG, "Received message to turn on HDMI debug layer.");
                mHDMIView.setDebugMode(true);
                break;

            case HIDE_HDMI_DEBUG_LAYER:
                Log.d(TAG, "Received message to turn off HDMI debug layer.");
                mHDMIView.setDebugMode(false);
                break;
        }

    }

    @Override
    public void dismissMe() {
        mDebouncing = false;
        //removeOverlayFragment();
        dismissOverlayFragment();
    }

    private void kickWDT(){
        Intent ackIntent = new Intent();
        ackIntent.setAction("io.ourglass.MF_WDT_KICK");
        sendBroadcast(ackIntent);
    }


}
