package io.ourglass.bucanero.core;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import net.danlew.android.joda.JodaTimeAndroid;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import io.ourglass.bucanero.api.JSONCallback;
import io.ourglass.bucanero.api.OGHeaderInterceptor;
import io.ourglass.bucanero.messages.MainThreadBus;
import io.ourglass.bucanero.messages.OnScreenNotificationMessage;
import io.ourglass.bucanero.messages.SystemStatusMessage;
import io.ourglass.bucanero.objects.NetworkException;
import io.ourglass.bucanero.services.Connectivity.ConnectivityCenter;
import io.ourglass.bucanero.services.Connectivity.EthernetPort;
import io.ourglass.bucanero.services.FFmpeg.FFmpegBinaryService;
import io.ourglass.bucanero.services.OGLog.OGLogService;
import io.ourglass.bucanero.services.STB.STBPollingService;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import okhttp3.OkHttpClient;

import static android.R.attr.delay;
import static io.ourglass.bucanero.core.OGSystem.isExternalStorageWritable;

/**
 * Created by mkahn on 5/18/16.
 */
public class ABApplication extends Application {

    public static Context sharedContext;
    public static final String TAG = "ABApplication";

    public static ABApplication thisApplication;
    public static ConnectivityCenter connectivityCenter;

    // Shared by all!
    public static final MainThreadBus ottobus = new MainThreadBus();

    private static Handler mAppHandler = new Handler();

    public static final OkHttpClient okclient = new OkHttpClient.Builder()
            .addInterceptor(new OGHeaderInterceptor())
            .build();

    @Override
    public void onCreate() {
        super.onCreate();
        // The realm file will be located in Context.getFilesDir() with name "default.realm"
        Log.d(TAG, "Loading AB application");

        thisApplication = this;
        sharedContext = getApplicationContext();

        RealmConfiguration config = new RealmConfiguration.Builder(this)
                .name("ab.realm")
                .schemaVersion(1)
                .build();

        Realm.setDefaultConfiguration(config);

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
        connectivityCenter.initializeCloudComms(null);
        //LogCat.takeLogcatSnapshotAndPost();
        //boot();

    }

    public static void dbToast(Context context, String message) {
        if (OGConstants.SHOW_DB_TOASTS) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }


    public void boot() {
        if (OGSystem.getFastBootMode()) {
            Log.d(TAG, "Fast booting...");
            bootWithDelay(0);
        } else {
            Log.d(TAG, "Slow booting...");
            bootWithDelay(OGConstants.BOOT_DELAY);
        }
    }

    private void startServices() {

        Intent logIntent = new Intent(this, OGLogService.class);
        startService(logIntent);

        Intent stbIntent = new Intent(this, STBPollingService.class);
        startService(stbIntent);


        Intent ffmpegBinaryIntent = new Intent(this, FFmpegBinaryService.class);
        //startService(ffmpegBinaryIntent);

        // Logcat messages go to a file...
//        if (OGSystem.isExternalStorageWritable() && OGConstants.LOGCAT_TO_FILE) {
//            Intent logCatServiceIntent = new Intent(this, LogCatRotationService.class);
//            startService(logCatServiceIntent);
//        }
    }

    private void sendBootMessage(String message){
        if (OGSystem.getFastBootMode()) return; //skip messages in FB mode
        new OnScreenNotificationMessage(message).post();
    }


    private void bootWithDelay(final int delay) {


        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(delay);

                    sendBootMessage("Setting up Networking");
                    EthernetPort.bringUpEthernetPort();


                    // TODO: Why is this here? The return value is not even used and there are no
                    // side effects. Gonna take it out.
                    //Thread.sleep(delay);
                    //NetworkingUtils.getDeviceIpAddresses();

                    Thread.sleep(delay);

                    sendBootMessage("Starting Services");
                    startServices();

                    Thread.sleep(delay);

                    sendBootComplete(1000);


                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }).start();


    }


    private JSONCallback regCallback = new JSONCallback() {
        @Override
        public void jsonCallback(JSONObject jsonData) {
            // 3. Update canonical settings
            OGSystem.updateFromOGCloud();
            sendBootMessage("All Done!");
            // Longer delay if fast boot
            sendBootComplete(delay == 0 ? 5000: 500);
        }

        @Override
        public void error(NetworkException e) {
            Log.e(TAG, "What the fuck just happened?");
            sendBootComplete(delay == 0 ? 5000: 500);
        }

    };


    private void sendBootComplete(final int afterMs) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(afterMs);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                new SystemStatusMessage(SystemStatusMessage.SystemStatus.BOOT_COMPLETE).post();
            }
        }).start();
    }


    // This is here for trying to figure out HTF logcat actually works!
    public void logcat(){

        if ( isExternalStorageWritable() && OGConstants.LOGCAT_TO_FILE ) {

            File appDirectory = new File( Environment.getExternalStorageDirectory() + "/Bucanero" );
            File logDirectory = new File( appDirectory + "/logs" );

            // create app folder
            if ( !appDirectory.exists() ) {
                appDirectory.mkdir();
            }

            // create log folder
            if ( !logDirectory.exists() ) {
                logDirectory.mkdir();
            }

            Calendar now = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yy-HH:mm:ss");
            String time = sdf.format(now.getTime());

            String fileName = "AxLogCat-" + time +
                    "-" + System.currentTimeMillis() + ".log";

            //File logFile = new File( logDirectory, fileName );

            File logFile = new File( logDirectory, "EFUCKING.log" );

            // clear the previous logcat and then write the new one to the file
            try {
                Process process = Runtime.getRuntime().exec( "logcat -c");
                Thread.sleep(1000);
                process = Runtime.getRuntime().exec( "logcat  -r 256 -v long -f " + logFile + "");
            } catch ( IOException e ) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }



}
