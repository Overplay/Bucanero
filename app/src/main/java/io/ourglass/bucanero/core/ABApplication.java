package io.ourglass.bucanero.core;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import net.danlew.android.joda.JodaTimeAndroid;

import io.ourglass.bucanero.api.BelliniDMAPI;
import io.ourglass.bucanero.messages.MainThreadBus;
import io.ourglass.bucanero.messages.OnScreenNotificationMessage;
import io.ourglass.bucanero.messages.SystemStatusMessage;
import io.ourglass.bucanero.services.Connectivity.ConnectivityService;
import io.ourglass.bucanero.services.Connectivity.EthernetPort;
import io.ourglass.bucanero.services.Connectivity.NetworkingUtils;
import io.ourglass.bucanero.services.LogCat.LogCatRotationService;
import io.ourglass.bucanero.services.STB.STBPollingService;
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
        if (isExternalStorageWritable() && OGConstants.LOGCAT_TO_FILE) {
            Intent logCatServiceIntent = new Intent(this, LogCatRotationService.class);
            startService(logCatServiceIntent);
        }

        JodaTimeAndroid.init(this);

        boot();

    }

    public static void dbToast(Context context, String message) {
        if (OGConstants.SHOW_DB_TOASTS) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
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

        Intent connectivityIntent = new Intent(this, ConnectivityService.class);
        startService(connectivityIntent);

        Intent stbIntent = new Intent(this, STBPollingService.class);
        startService(stbIntent);

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

                    Thread.sleep(delay);
                    sendBootMessage("Contacting OG Cloud");


                    // System registers its existance if it hasn't already
                    if (OGSystem.getOGCloudDBId()==null)
                        BelliniDMAPI.registerDeviceWithBellini(null);

                    // This is a test mode that automatically associates a box/emu with the OG Office in Campbell
                    if (OGConstants.AUTO_REG_TO_OGOFFICE) {
                        BelliniDMAPI.associateDeviceWithVenueUUID(BelliniDMAPI.TEMP_OG_OFFICE_VUUID);
                    } else if (OGSystem.getVenueId().isEmpty()){
                        BelliniDMAPI.associateDeviceWithVenueUUID(BelliniDMAPI.LIMBO_VUUID);
                    }

                    Thread.sleep(delay);
                    NetworkingUtils.getDeviceIpAddresses();

                    sendBootMessage("Starting Services");
                    startServices();

                    Thread.sleep(delay);
                    sendBootMessage("All Done!");

                    // Longer delay if fast boot
                    sendBootComplete(delay == 0 ? 5000: 500);


                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }).start();


    }


    private void bootSyncWithCloud(){

        // 1. Run a registration pass. If this box is already registered, we get back the last settings
        // saved in the cloud which we sync into this box.


    }

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


}
