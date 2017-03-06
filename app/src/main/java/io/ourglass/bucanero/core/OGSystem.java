package io.ourglass.bucanero.core;


import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import io.ourglass.bucanero.tv.Support.Size;

/**
 * Created by mkahn on 2/12/17.
 */

public class OGSystem {

    private static final String TAG = "OGSystem";

    /*
     *
     * Shared Preferences Methods
     *
     *
     */

    private static SharedPreferences mPrefs = ABApplication.sharedContext.getSharedPreferences(
            "ourglass.buc", Context.MODE_PRIVATE);

    private static SharedPreferences.Editor mEditor = mPrefs.edit();

    public static void putStringToPrefs(String key, String string) {

        mEditor.putString(key, string);
        mEditor.apply();
    }

    public static String getStringFromPrefs(String key, String defValue) {

        return mPrefs.getString(key, defValue);

    }

    public static void putIntToPrefs(String key, int integer) {

        mEditor.putInt(key, integer);
        mEditor.apply();
    }

    public static int getIntFromPrefs(String key) {

        return mPrefs.getInt(key, 0);

    }

    /*
     * TV Resolution
     */

    private static Size mTVResolution;

    public static void setTVResolution(Size size) {
        mTVResolution = size;
        Log.d(TAG, "Screen size is: " + size.toString());
    }

    public static Size getTVResolution() {
        return mTVResolution;
    }

    /*
     * System UDID
     */

    public static String getUDID() {
        return Settings.Secure.getString(ABApplication.sharedContext.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    /**
     * OS Info
     */

    /**
     * @return os string like "4.4.2"
     */
    public static String getOsVersion() {
        return Build.VERSION.RELEASE;
    }

    /**
     * @return in value like 19
     */
    public static int getOsLevel() {
        return Build.VERSION.SDK_INT;
    }

    /**
     *
     * @return boolean if we're running in emulator
     */
    public static boolean isEmulator() {
        return Build.FINGERPRINT.contains("generic");
    }

    /**
     * Tells whether running on Tronsmart hardware
     *
     * @return boolean
     */
    public static boolean isTronsmart() {
        return (getOsLevel() == 19) && !isEmulator();
    }

    /**
     * Tells whether running on real Ourglass hardware
     *
     * @return boolean
     */
    public static boolean isRealOG() {
        return (getOsLevel() > 19) && !isEmulator();
    }


    public static boolean enableHDMI() {
        if (isRealOG()) {
            return false;
        } else if (isTronsmart()) {
            //enableTronsmartHDMI();
            return true;
        } else {
            // Emulator
            return false;
        }
    }

    public static void setSystemName(String name) {
        putStringToPrefs("systemName", name);
    }

    public static String getSystemName() {
        return getStringFromPrefs("systemName", "No Name");
    }

    public static void setSystemLocation(String location) {
        putStringToPrefs("systemLocation", location);
    }

    public static String getSystemLocation() {
        return getStringFromPrefs("systemLocation", "No Location");
    }

    // Set top pairing

    public static void setPairedSTBIpAddress(String ipAddr) {
        putStringToPrefs("pairedSTBIpAddress", ipAddr);
    }

    public static String getPairedSTBIpAddress() {
        return getStringFromPrefs("pairedSTBIpAddress", null);
    }

    public static boolean isPairedToSTB() {
        return getPairedSTBIpAddress() != null;
    }

    // Only valid type right now is "DIRECTV"
    public static String getPairedSTBType() {
        return getStringFromPrefs("pairedSTBType", null);
    }

    public static void setPairedSTBType(String stbType) {
        putStringToPrefs("pairedSTBType", stbType);
    }

//    // TODO: This should use Serializable interface and save the object directly
//    public static void setPairedSTB(DirecTVSetTopBox stb) {
//        setPairedSTBIpAddress(stb.ipAddress);
//        putStringToPrefs("ssdpResponse", stb.ssdpResponse);
//        setPairedSTBType("DIRECTV");
//        pairedSTB = stb;
//    }
//
//    /**
//     * Returns the current STB or a blank SetTopBox that needs to have it's networking functions run to load current state
//     *
//     * @return
//     */
//    public static DirecTVSetTopBox getPairedSTB() {
//
//        if (!isPairedToSTB()) {
//            return null;
//        }
//
//        if (pairedSTB != null)
//            return pairedSTB;
//
//        // need to dearchive
//        String ipAddr = getPairedSTBIpAddress();
//        String ssdpResponse = getStringFromPrefs("ssdpResponse", "");
//        pairedSTB = new DirecTVSetTopBox(null, ipAddr, SetTopBox.STBConnectionType.IPGENERIC, ssdpResponse);
//        return pairedSTB;
//
//    }

    // "ab" stands for AmstelBright which was the original codename of this project
    public static void setABVersionName(String vName) {
        putStringToPrefs("abVersionName", vName);
    }

    public static String getABVersionName() {
        return getStringFromPrefs("abVersionName", null);
    }

    public static void setABVersionCode(int vCode) {
        putIntToPrefs("abVersionCode", vCode);
    }

    public static int getABVersionCode() {
        return getIntFromPrefs("abVersionCode");
    }

    public static JSONObject getSystemInfo() {
        JSONObject deviceJSON = new JSONObject();
        try {
            deviceJSON.put("name", getSystemName());
            deviceJSON.put("locationWithinVenue", getSystemLocation());
            deviceJSON.put("randomFactoid", "Bunnies are cute");

            WifiManager manager = (WifiManager) ABApplication.sharedContext.getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = manager.getConnectionInfo();
            String macAddress = info.getMacAddress();

            if (macAddress == null) {
                macAddress = "undefined";
            }

            deviceJSON.put("wifiMacAddress", macAddress);
            //deviceJSON.put("settings", device.settings);
            //deviceJSON.put("apiToken", device.apiToken);
            //deviceJSON.put("uuid", device.uuid);
            String pairIp = getPairedSTBIpAddress();
            deviceJSON.put("isPairedToSTB", isPairedToSTB());
            deviceJSON.put("pairedSTBIP", pairIp);

//            if (isPairedToSTB() && OGCore.currentlyOnTV != null) {
//                deviceJSON.put("channel", OGCore.currentlyOnTV.networkName);
//                deviceJSON.put("title", OGCore.currentlyOnTV.title);
//            }

            deviceJSON.put("outputRes", getTVResolution().toJson());

            deviceJSON.put("abVersionName", getABVersionName());
            deviceJSON.put("abVersionCode", getABVersionCode());

            deviceJSON.put("osVersion", getOsVersion());
            deviceJSON.put("osApiLevel", getOsLevel());

            deviceJSON.put("venue", getVenueId());
            deviceJSON.put("udid", getUDID());

            //deviceJSON.put("lastGuideSync", AppSettings.getString(AJPGSPollingService.LAST_SYNC_SETTINGS_KEY, "none"));

        } catch (JSONException e) {
            Log.e("OGDevice.model", e.toString());
            return null;
        }

        return deviceJSON;
    }

    public static void setVenueId(String venueId) {
        putStringToPrefs("venueId", venueId);
    }

    public static String getVenueId() {
        return getStringFromPrefs("venueId", "");
    }

    public static void setDeviceId(String deviceId) {
        putStringToPrefs("deviceId", deviceId);
    }

    public static String getDeviceId() {
        return getStringFromPrefs("deviceId", "");
    }

    public static void setDeviceAPIToken(String venueId) {
        putStringToPrefs("deviceToken", venueId);
    }

    public static String getDeviceAPIToken() {
        return getStringFromPrefs("deviceToken", "");
    }


    /*******************************************************************************
     *
     * TRONSMART SPECIFIC CODE
     *
     *******************************************************************************/

    /***************************************
     * TRONSMART CODE
     ***************************************/

    private static boolean enableOGHDMI() {

        Log.wtf(TAG, "Yeah, that's not implemented yet. But nice try.");
        return false;

    }




    /***********************************
     * HARD PAIR CODE
     ***********************************/

    public static boolean isHardPaired() {

        if (OGConstants.SIMULATE_HARD_PAIR == true)
            return true;

        String pairedSTBAddr = getPairedSTBIpAddress();
        if (pairedSTBAddr == null) return false;
        return pairedSTBAddr.equalsIgnoreCase("10.21.200.2");
    }

//    public static void checkHardSTBConnection() {
//
//        Runnable checkHardRunnable = new Runnable() {
//            @Override
//            public void run() {
//                Log.d(TAG, "Checking hard pair thread running");
//                JSONObject stbJson = DirecTVAPI.stbInfo("10.21.200.2");
//                if (stbJson != null) {
//                    Log.d(TAG, "We are hard paired!");
//                    OGSystem.setPairedSTBIpAddress("10.21.200.2");
//                    DirecTVSetTopBox newSTB = new DirecTVSetTopBox(null, "10.21.200.2",
//                            SetTopBox.STBConnectionType.IPGENERIC, null);
//                    OGSystem.setPairedSTB(newSTB);
//                    //Toast.makeText(ABApplication.sharedContext, "Hard Paired!", Toast.LENGTH_LONG).show();
//
//
//                } else {
//                    Log.d(TAG, "Hard pair check failed!");
//                    //Toast.makeText(ABApplication.sharedContext, "Hard Pair FAILED!", Toast.LENGTH_LONG).show();
//
//                }
//            }
//        };
//
//        Thread cht = new Thread(checkHardRunnable);
//        cht.start();
//
//
//    }

    public static void bringUpEthernetPort() {


    }


}


