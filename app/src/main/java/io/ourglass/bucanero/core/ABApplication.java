package io.ourglass.bucanero.core;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;

import net.danlew.android.joda.JodaTimeAndroid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.fabric.sdk.android.Fabric;
import io.ourglass.bucanero.api.OGHeaderInterceptor;
import io.ourglass.bucanero.messages.MainThreadBus;
import io.ourglass.bucanero.services.Connectivity.ConnectivityCenter;
import io.ourglass.bucanero.services.OGLog.OGLogWorker;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

//

/**
 * Created by mkahn on 5/18/16.
 */
public class ABApplication extends Application {

    public static Context sharedContext;
    public static final String TAG = "ABApplication";

    public static ABApplication thisApplication;
    public static ConnectivityCenter connectivityCenter;

    public static OGLogWorker ogLogWorker;

    // Shared by all!
    public static final MainThreadBus ottobus = new MainThreadBus();

    private static Handler mAppHandler = new Handler();

    public static final OkHttpClient okclient = new OkHttpClient.Builder()
            .addInterceptor(new OGHeaderInterceptor())
            .cookieJar(new CookieJar() {
                private final HashMap<HttpUrl, List<Cookie>> cookieStore = new HashMap<>();

                @Override
                public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                    cookieStore.put(url, cookies);
                }

                @Override
                public List<Cookie> loadForRequest(HttpUrl url) {
                    List<Cookie> cookies = cookieStore.get(url);
                    return cookies != null ? cookies : new ArrayList<Cookie>();
                }
            })
            .build();

    public static boolean bootComplete = false;

    @Override
    public void onCreate() {
        super.onCreate();
        // The realm file will be located in Context.getFilesDir() with name "default.realm"
        Log.d(TAG, "Loading AB application");

        thisApplication = this;
        sharedContext = getApplicationContext();

        Fabric.with(this, new Crashlytics());
        Fabric.with(this, new Answers());

        logUser();

        Log.d(TAG, "Package name is: " + getApplicationContext().getPackageName());

        Realm.init(getApplicationContext());

        RealmConfiguration config = new RealmConfiguration.Builder()
                .name("ab.realm")
                .schemaVersion(1)
                .build();

        Realm.setDefaultConfiguration(config);

        ogLogWorker = new OGLogWorker();

        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            int code = pInfo.versionCode;
            OGSystem.setABVersionName(version);
            OGSystem.setABVersionCode(code);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        JodaTimeAndroid.init(this);

        connectivityCenter = ConnectivityCenter.getInstance();
        connectivityCenter.initializeCloudComms();

    }

    public static void dbToast(String message) {
        if (OGConstants.SHOW_DB_TOASTS) {
            Toast.makeText(sharedContext, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void logUser() {
        // TODO: Use the current user's information
        // You can call any combination of these three methods
        Crashlytics.setUserIdentifier(OGSystem.getUDID());
        Crashlytics.setUserName("@VENUE: "+OGSystem.getVenueName());

    }


}
