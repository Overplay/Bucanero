package io.ourglass.bucanero.core;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import net.danlew.android.joda.JodaTimeAndroid;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import io.ourglass.bucanero.api.BelliniDMAPI;
import io.ourglass.bucanero.messages.MainThreadBus;
import io.ourglass.bucanero.services.LogCat.LogCatRotationService;
import io.ourglass.bucanero.services.SocketIO.SocketIOManager;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import okhttp3.OkHttpClient;

/**
 * Created by mkahn on 5/18/16.
 */
public class ABApplication extends Application {

    public static Context sharedContext;
    public static final String TAG = "ABApplication";

    // Shared by all!
    public static final OkHttpClient okclient = new OkHttpClient();
    public static final MainThreadBus ottobus = new MainThreadBus();
    public static final SocketIOManager siomanager = SocketIOManager.getInstance();

    @Override
    public void onCreate() {
        super.onCreate();
        // The realm file will be located in Context.getFilesDir() with name "default.realm"
        Log.d(TAG, "Loading AB application");

        sharedContext = getApplicationContext();

        bringUpEthernet();

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

        // Logcat messages go to a file...
        if ( isExternalStorageWritable() && OGConstants.LOGCAT_TO_FILE ) {
            Intent logCatServiceIntent = new Intent(this, LogCatRotationService.class);
            startService(logCatServiceIntent);
        }

        JodaTimeAndroid.init(this);

        BelliniDMAPI.registerDeviceWithBellini();

        if (OGConstants.AUTO_REG_TO_OGOFFICE){
            BelliniDMAPI.associateDeviceWithVenueUUID(BelliniDMAPI.TEMP_OG_OFFICE_VUUID);
        }

    }

    public static void dbToast(Context context, String message){
        if (OGConstants.SHOW_DB_TOASTS){
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ) {
            return true;
        }
        return false;
    }

//    public void launchUDHCPd(){
//
//        Log.d(TAG, "Firing up UDHCPD");
//
//        ShellExecutor bringUpUdhcpd = new ShellExecutor(new ShellExecutor.ShellExecutorListener() {
//            @Override
//            public void results(String results) {
//                Log.d(TAG, "UDHCPD>>>>> "+results);
//                try {
//                    Thread.sleep(5000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//                OGSystem.checkHardSTBConnection();
//
//                try {
//                    Thread.sleep(5000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//                OGSystem.checkHardSTBConnection();
//
//                try {
//                    Thread.sleep(15000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//                OGSystem.checkHardSTBConnection();
//
//                try {
//                    Thread.sleep(60000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//                OGSystem.checkHardSTBConnection();
//
//                try {
//                    Thread.sleep(120000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//                OGSystem.checkHardSTBConnection();
//
//
//            }
//        });
//
//        bringUpUdhcpd.exec("su -c /system/bin/busybox udhcpd /mnt/sdcard/wwwaqui/conf/udhcpd.conf");
//
//    }

    public void bringUpEthernet(){

        Log.d(TAG, "Bringing up ethernet interface.");

//        ShellExecutor bringUpEth = new ShellExecutor(new ShellExecutor.ShellExecutorListener() {
//            @Override
//            public void results(String results) {
//                Log.d(TAG, "IFUP>>>>> "+results);
//                launchUDHCPd();
//            }
//        });
//
//        bringUpEth.exec("su -c /system/bin/busybox ifconfig eth0 10.21.200.1 netmask 255.255.255.0");
    }

}
