package io.ourglass.bucanero.tv.Fragments;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
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

import org.json.JSONObject;

import io.ourglass.bucanero.core.OGSystem;
import io.ourglass.bucanero.tv.Support.Frame;
import io.ourglass.bucanero.tv.Support.OGAnimations;


public class OGWebViewFragment extends WebViewFragment {

    private static final String TAG = "OGWebViewFragment";
    private WebView webView;
    private Context mContext;
    private Frame mFrame;
    private String mUrl;

    private OGWebViewListener mListener;

    public static OGWebViewFragment newInstance(String initialUrl, Frame initialFrame) {
        OGWebViewFragment fragment = new OGWebViewFragment();
        Bundle args = new Bundle();
        args.putSerializable("FRAME", initialFrame);
        args.putString("URL", initialUrl);
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
            mUrl = getArguments().getString("URL");
            mFrame = (Frame) getArguments().getSerializable("FRAME");

        }
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
        Log.d(TAG, "In onResume");
    }

    public void setFrame(Frame frame) {
        mFrame = frame;
        ViewGroup.LayoutParams params = getWebView().getLayoutParams();
        // Changes the height and width to the specified *pixels*
        params.height = frame.size.height;
        params.width = frame.size.width;
        getWebView().setLayoutParams(params);
        getWebView().setTranslationX(frame.location.x);
        getWebView().setTranslationY(frame.location.y);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "In onDetach");

    }

    @Override
    public void onStart() {
        //WebView is available here
        super.onStart();

        WebView wv = getWebView();

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
                OGAnimations.animateAlphaIn(getWebView(), 1f);
            }

        });

        WebSettings settings = wv.getSettings();
        settings.setLoadWithOverviewMode(false);

        if (mUrl != null) {
            loadUrl(mUrl);
        }

    }

    public void loadUrl(String url) {

        getWebView().setAlpha(0f);
        Log.wtf(TAG, "loadUrl called");
        getWebView().loadUrl(url);

        //injectSystemGlobals(OGSystem.getSystemInfo());

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


}
