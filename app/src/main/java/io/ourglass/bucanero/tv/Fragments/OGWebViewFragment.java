package io.ourglass.bucanero.tv.Fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebViewFragment;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.squareup.otto.Subscribe;

import org.json.JSONObject;

import io.ourglass.bucanero.api.BelliniDMAPI;
import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.core.AppMapEntry;
import io.ourglass.bucanero.core.OGConstants;
import io.ourglass.bucanero.core.OGSystem;
import io.ourglass.bucanero.messages.BestPositionMessage;
import io.ourglass.bucanero.messages.KillAppMessage;
import io.ourglass.bucanero.messages.MoveAppMessage;
import io.ourglass.bucanero.messages.MoveWebViewMessage;
import io.ourglass.bucanero.tv.Support.Frame;
import io.ourglass.bucanero.tv.Support.OGAnimations;
import io.ourglass.bucanero.tv.Support.Size;


public class OGWebViewFragment extends WebViewFragment {

    private static final String TAG = "OGWebViewFragment";
    private static final float INITIAL_ALPHA = 0.5f; //this would normally be zero
    private static final boolean ANIMATE_MOTION = true;

    private WebView webView;
    private String mCurrentUrl = "";
    private Context mContext;
    private Frame mFrame;
    private String mUrl;
    public int mLayoutSlot = 0;
    public String appType;

    private String mAppId;

    // Here for nudging to be added later
    private int mXInset = 0;
    private int mYInset = 0;

    private OGWebViewListener mListener;

    // TODO Not sure all the factory params are needed
    public static OGWebViewFragment newInstance(String appType, int layoutSlot, Frame initialFrame) {
        OGWebViewFragment fragment = new OGWebViewFragment();
        Bundle args = new Bundle();
        args.putSerializable("FRAME", initialFrame);
        args.putString("APPTYPE", appType);
        args.putInt("SLOT", layoutSlot);
        fragment.setArguments(args);
        return fragment;
    }

    public interface OGWebViewListener {
        public void ready();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "In onCreate");
        if (getArguments() != null) {
            appType = getArguments().getString("APPTYPE");
            mFrame = (Frame) getArguments().getSerializable("FRAME");
            mLayoutSlot = getArguments().getInt("SLOT");
        }

        // Register to receive messages
        ABApplication.ottobus.register(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //Runs first Android 5+
        Log.d(TAG, "In onAttach(Context)");
        mContext = context;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        //Runs first Android 4.4
        Log.d(TAG, "In onAttach(activity)");
        mContext = activity.getBaseContext();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mCurrentUrl.isEmpty()){
            fadeIn();
        }
        Log.d(TAG, "In onResume");
    }

    public void setType(String appType) {
        this.appType = appType;
    }

    private Point calculateLocationForSlot() {

        float newX = 0, newY = 0;

        // TODO this is gross logic...maybe use some enums to do the calcs
        switch (appType) {
            case "widget":
                if (mLayoutSlot == 0 || mLayoutSlot == 3) {
                    // Left edge, so only zero plus the nudge inset
                    newX = mXInset;
                } else {
                    // Right edge
                    newX = OGSystem.getTVResolution().width - mFrame.size.width - mXInset;
                }

                if (mLayoutSlot < 2) {
                    // At the top
                    newY = OGSystem.getTVResolution().height * OGConstants.WIDGET_Y_INSET + mYInset;
                } else {
                    // At the bottom
                    newY = OGSystem.getTVResolution().height - OGSystem.getTVResolution().height * OGConstants.WIDGET_Y_INSET -
                            mYInset - mFrame.size.height;
                }
                break;

            case "crawler":

                newX = 0;
                if (mLayoutSlot == 0) {
                    newY = mYInset;
                } else {
                    newY = OGSystem.getTVResolution().height - mFrame.size.height - mYInset;
                }
                break;
        }

        return new Point((int) newX, (int) newY);

    }

    public void moveToNextLayoutSlot() {
        int numSlots = appType.equalsIgnoreCase("widget") ? 4 : 2;
        mLayoutSlot = (mLayoutSlot + 1) % numSlots;
        setFrameForSlot(mLayoutSlot);
    }

    public void setFrameForSlot(int layoutSlot) {
        mLayoutSlot = layoutSlot;
        mFrame.location = calculateLocationForSlot();
        if (appType.equalsIgnoreCase("widget")){
            OGSystem.widgetSlot = mLayoutSlot;
        } else {
            OGSystem.crawlerSlot = mLayoutSlot;
        }
        updateFrame();
    }

    public void setFrame(Frame frame) {
        mFrame = frame;
        updateFrame();
    }

    public void setSize(Size size) {
        mFrame.size = size;
        updateFrame();
    }

    public void setSizeAsPctOfScreen(Size pctSize) {
        Size tvRes = OGSystem.getTVResolution();
        float width = appType.equalsIgnoreCase("crawler") ? tvRes.width : tvRes.width * pctSize.width / 100;
        Size appSize = new Size((int) width, tvRes.height * pctSize.height / 100);
        setSize(appSize);
        updateFrame();
    }

    private void animateFrameChanges(Frame destinationFrame) {

    }

    private void updateFrame() {

        Point wvPosition = calculateLocationForSlot();
        mFrame.location = wvPosition;
        ViewGroup.LayoutParams params = getWebView().getLayoutParams();
        // Changes the height and width to the specified *pixels*
        params.height = mFrame.size.height;
        params.width = mFrame.size.width;
        getWebView().setLayoutParams(params);

        if (ANIMATE_MOTION) {
            OGAnimations.moveView(getWebView(), mFrame.location, OGAnimations.MoveAnimation.SLIDE);

        } else {
            getWebView().setTranslationX(mFrame.location.x);
            getWebView().setTranslationY(mFrame.location.y);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "In onDetach");

    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onStart() {
        //WebView is available here
        super.onStart();

        setFrameForSlot(mLayoutSlot);
        WebView wv = getWebView();

        wv.setAlpha(INITIAL_ALPHA);

        if (mFrame != null)
            setFrame(mFrame);

        wv.getSettings().setJavaScriptEnabled(true);
        wv.getSettings().setDomStorageEnabled(true);
        wv.setBackgroundColor(Color.TRANSPARENT);
        wv.setFocusable(false);
        wv.addJavascriptInterface(new SystemJsonObject(), "OGSystem");

        wv.setWebChromeClient(new WebChromeClient() {

            public void onProgressChanged(WebView view, int progress) {
                // Activities and WebViews measure progress with different scales.
                // The progress meter will automatically disappear when we reach 100%
                Log.d(TAG, "Progress is: " + progress);
            }

//            public boolean onConsoleMessage(ConsoleMessage cmsg){
//
//                Log.d(TAG, "JS CONSOLE: ["+cmsg.lineNumber()+"] "+cmsg.message());
//                return true;
//            }
        });

        wv.setWebViewClient(new WebViewClient() {

            public void onReceivedError(WebView view, int errorCode, WebResourceRequest req, WebResourceError err) {
                Log.e(TAG, "Error!");
            }

            public void onPageFinished(WebView view, String url) {
                Log.e(TAG, "Page done loading!");
                OGAnimations.animateAlphaTo(getWebView(), 1f);
            }

        });

        WebSettings settings = wv.getSettings();
        settings.setLoadWithOverviewMode(false);

        if (mUrl != null) {
            loadUrl(mUrl);
        }

    }

    public void halfFade() {
        //Not called as a part if the opacity bug
        OGAnimations.animateAlphaTo(getView(), 0.35f);
    }

    public void fadeOut() {
        OGAnimations.animateAlphaTo(getView(), 0f);
    }

    public void fadeIn() {
        OGAnimations.animateAlphaTo(getView(), 1f);
    }

    public void hide() {
        getWebView().setAlpha(0);
    }

    public void show() {
        getWebView().setAlpha(1);
    }

    public void loadUrl(String url) {
        mCurrentUrl = url;
        getWebView().setAlpha(0f);
        Log.wtf(TAG, "loadUrl called");
        getWebView().loadUrl(url);
    }

    public void launchApp(String appId) {
        mAppId = appId;
        loadUrl(BelliniDMAPI.fullUrlForApp(appId));
        Answers.getInstance().logCustom(new CustomEvent("App Launch")
                .putCustomAttribute("appId", appId));
    }

    class SystemJsonObject {
        @JavascriptInterface
        public String getSystemInfo() {
            return OGSystem.getSystemInfo().toString();
        }
    }

    // This is *not* working, but the SystemJsonObject is working.
    public void injectSystemGlobals(JSONObject jsonObject) {
        getWebView().loadUrl("javascript:SET_SYSTEM_GLOBALS_JSON(" + jsonObject.toString() + ")");
    }

    @Subscribe
    public void inboundKill(KillAppMessage killMsg) {
        // TODO: React to the event somehow!
        Log.d(TAG, "Got a launch kill, yo!");
        String appToDie = killMsg.appId;

        if (appToDie.equalsIgnoreCase(mAppId)) {
            fadeOut();
            getWebView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Kill the view
                    loadUrl("");
                }
            }, 1000);
            BelliniDMAPI.appKillAck(mAppId);
            Answers.getInstance().logCustom(new CustomEvent("App Kill")
                .putCustomAttribute("appId", killMsg.appId));

        }
    }

    @Subscribe
    public void inboundMove(MoveAppMessage moveMsg) {
        // TODO: React to the event somehow!
        Log.d(TAG, "Got a move message, yo!");
        if (moveMsg.appId.equalsIgnoreCase(mAppId)) {
            moveToNextLayoutSlot();
            OGSystem.screenMap.put(appType, new AppMapEntry(mAppId, mLayoutSlot, AppMapEntry.mapFromString(appType)));
            BelliniDMAPI.appMoveAck(mAppId, mLayoutSlot);
            Answers.getInstance().logCustom(new CustomEvent("App Move"));
        }
    }

    @Subscribe
    public void inboundExplicitMove(MoveWebViewMessage moveMsg) {
        Log.d(TAG, "Got an explicit slot move message, yo!");
        if (appType.equalsIgnoreCase(moveMsg.type)){
            setFrameForSlot(moveMsg.slot);
        }
    }

    @Subscribe
    public void inboundBestPosition(BestPositionMessage bpMsg) {
        Log.d(TAG, "Got a BestPosition message, yo!");

        int slot = mLayoutSlot;

        switch (appType){

            case "crawler":
                slot = bpMsg.getPreferredCrawlerSlot(mLayoutSlot);
                break;

            case "widget":
                slot = bpMsg.getPreferredWidgetSlot(mLayoutSlot);
                break;

        }

        if (slot != mLayoutSlot){
            Log.d(TAG, "Moving " + appType + " app in response to BestPosition message!");
            Answers.getInstance().logCustom(new CustomEvent("Best Position Move"));
            setFrameForSlot(slot);
        }
    }

}
