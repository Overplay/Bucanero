package io.ourglass.bucanero.core;


import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import io.ourglass.bucanero.api.BelliniDMAPI;
import io.ourglass.bucanero.api.JSONCallback;
import io.ourglass.bucanero.objects.NetworkException;
import io.ourglass.bucanero.objects.SetTopBox;
import io.ourglass.bucanero.objects.TVShow;
import io.ourglass.bucanero.services.Connectivity.NetworkingUtils;
import io.ourglass.bucanero.services.STB.DirecTV.DirecTVAPI;
import io.ourglass.bucanero.services.STB.DirecTV.DirecTVSetTopBox;
import io.ourglass.bucanero.tv.Support.Size;

import static io.ourglass.bucanero.services.Connectivity.NetworkingUtils.getWiFiMACAddress;

/**
 * Created by mkahn on 2/12/17.
 */

public class OGSystem {

    private static final String TAG = "OGSystem";

    public static int crawlerSlot = 0;
    public static int widgetSlot = 0;

    private static SetTopBox mPairedSTB;
    private static TVShow mCurrentTVShow;

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

    public static void putBoolToPrefs(String key, boolean bool) {

        mEditor.putBoolean(key, bool);
        mEditor.apply();
    }

    public static boolean getBoolFromPrefs(String key, boolean defaultValue) {
        return mPrefs.getBoolean(key, defaultValue);
    }

    /*
     * Verbose/Debug Mode
     */
    private static boolean getVerboseMode(){
        return getBoolFromPrefs("verboseMode", OGConstants.SHOW_DB_TOASTS);
    };

    private static void setVerboseMode(boolean verbose){
        putBoolToPrefs("verboseMode", verbose);
    };

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
     * OLD System UDID
     * See bottom of file for new SJM stuff
     */

    public static String getNonUniqueUDID() {
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
        return OGConstants.FORCE_EMULATOR || Build.FINGERPRINT.contains("generic");
    }


    /**
     *
     * @return boolean if we're running on Nexus 10
     */
    public static boolean isNexus() {
        return Build.HARDWARE.equalsIgnoreCase("manta");
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

    private static void setPairedSTBIpAddress(String ipAddr) {
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

    private static void setPairedSTBType(String stbType) {
        putStringToPrefs("pairedSTBType", stbType);
    }

    public static void setPairedSTB(SetTopBox stb) {
        setPairedSTBIpAddress(stb.ipAddress);
        ///setPairedSTBType(stb.carrier);
        putStringToPrefs("pairedSTB", stb.toJsonString());
        BelliniDMAPI.registerSTBPairing(stb);
    }

    public static void unpairSTB(){

        setPairedSTBIpAddress(null);
        setPairedSTBType(null);
        putStringToPrefs("pairedSTB", null);

    }

    /**
     * Returns the current STB or a blank SetTopBox that needs to have it's networking functions run to load current state
     *
     * @return
     */
    public static SetTopBox getPairedSTB() {

        if (!isPairedToSTB()) {
            return null;
        }

        if (mPairedSTB != null)
            return mPairedSTB;

        // need to dearchive
        String json = mPrefs.getString("pairedSTB", "");
        if (json.equalsIgnoreCase("")){
            Log.wtf(TAG, "In a weird pairing state where mPairedSTB is null but isPaired is true and in SharedPrefs");
            mPairedSTB = null;
            return null;
        }

        Gson gson = new Gson();

        // Can't dearchive to a abstract class, so any concrete will do for now
        DirecTVSetTopBox dtvstb = gson.fromJson(json, DirecTVSetTopBox.class);

        switch (dtvstb.carrier){
            case DIRECTV:
                Log.d(TAG, "Dearchived DirecTV STB from prefs");
                mPairedSTB = dtvstb;
                return mPairedSTB;

            case XFINITY:
            default:
                Log.wtf(TAG, "Got an unsupported Carrier from archived STB!!!");
                mPairedSTB = dtvstb;
                return mPairedSTB;
        }

    }

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

    // Fast boot mode just fires everything off at once and does not signal any messages
    // Slow boot is more dramatic and looks like "work is being done" :)
    public static boolean getFastBootMode(){
        return getBoolFromPrefs("fastBootMode", false);
    }

    public static void setFastBootMode(boolean isFast){
        putBoolToPrefs("fastBootMode", true);
    }

    public static void setOGCloudDBId(String dbId) {
        putStringToPrefs("OGCloudDBId", dbId);
    }

    public static String getOGCloudDBId() {
        return getStringFromPrefs("OGCloudDBId", null);
    }

    public static JSONObject getSystemInfo() {
        JSONObject deviceJSON = new JSONObject();
        try {
            deviceJSON.put("name", getSystemName());
            deviceJSON.put("locationWithinVenue", getSystemLocation());
            deviceJSON.put("randomFactoid", "Bunnies are cute");
            deviceJSON.put("codeRevName", OGConstants.CODE_REV_NAME);



            deviceJSON.put("wifiMacAddress", getWiFiMACAddress());
            //deviceJSON.put("settings", device.settings);
            //deviceJSON.put("apiToken", device.apiToken);
            //deviceJSON.put("uuid", device.uuid);
            String pairIp = getPairedSTBIpAddress();
            deviceJSON.put("isPairedToSTB", isPairedToSTB());
            deviceJSON.put("pairedSTBIP", pairIp);

            if (isPairedToSTB()){
                if (mCurrentTVShow!=null){
                    deviceJSON.put("nowShowing", new JSONObject(getCurrentTVShow().toJsonString()));
                } else {
                    deviceJSON.put("nowShowing", new JSONObject()); // usually a poll hasn't happened yet
                }
            }

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

            deviceJSON.put("ts", System.currentTimeMillis());

            deviceJSON.put("widgetSlot", OGSystem.widgetSlot);
            deviceJSON.put("crawlerSlot", OGSystem.crawlerSlot);

            //deviceJSON.put("lastGuideSync", AppSettings.getString(AJPGSPollingService.LAST_SYNC_SETTINGS_KEY, "none"));

        } catch (JSONException e) {
            Log.e("OGDevice.model", e.toString());
            return null;
        }

        return deviceJSON;
    }

    public static String getSystemInfoString(){
        StringBuilder sb = new StringBuilder();

        sb.append("System Name: " + getSystemName() + "\n");
        sb.append("System UUID: " + getUDID() + "\n");
        if (getVenueId().equalsIgnoreCase("")){
            sb.append("Venue ID: Not assigned to a venue"+"\n");
        } else {
            sb.append("Venue ID: " + getVenueId()+"\n");
        }
        sb.append("-----------\n");
        sb.append("Code Name Rev: " + OGConstants.CODE_REV_NAME+"\n");
        sb.append("Version: " + getABVersionName() + "\n");
        sb.append("OS Version: " + getOsVersion()  + "\n");
        sb.append("-----------\n");
        sb.append("WiFi IP Address: " + NetworkingUtils.getWiFiIPAddressString()+"\n");
        sb.append("Ethernet IP Address:" + NetworkingUtils.getEthernetIPAddressString() + "\n");
        sb.append("-----------\n");
        if ( isPairedToSTB() ){
            sb.append("System Paired to STB at: "+ getPairedSTBIpAddress()+ "\n");
            if (mCurrentTVShow!=null){
                sb.append("Showing: "+mCurrentTVShow.toString()+"\n");
            } else {
                sb.append("Showing: *** not yet available ***\n");
            }
        } else {
            sb.append("System is not paired to STB"+ "\n");
        }

        sb.append("Output Resolution: " + getTVResolution().toString()+ "\n");

        return sb.toString();

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

    public static boolean isFirstTimeSetup() {
        return getBoolFromPrefs("firstTimeSetup", true);
    }

    public static void setFirstTimeSetup(boolean isFirst){
        putBoolToPrefs("firstTimeSetup", isFirst);
    }

    public static void updateFromOGCloud(){

        BelliniDMAPI.getMe(new JSONCallback() {
            @Override
            public void jsonCallback(JSONObject jsonData) {
                // Assume OGCloud is canonical on the following and update local
                String venueUUID = jsonData.optString("atVenueUUID", null);
                if (venueUUID!=null){
                    Log.d(TAG, "Updating local venue ID from OG Cloud");
                    setVenueId(venueUUID);
                }

                String name = jsonData.optString("name", "No Name");
                if (name!=null){
                    Log.d(TAG, "Updating local system name from OG Cloud");
                    setSystemName(name);
                }
            }

            @Override
            public void error(NetworkException e) {
                Log.e(TAG, "There was a problem retrieving OGDevice from OG Cloud.");
            }
        });
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

        String pairedSTBAddr = getPairedSTBIpAddress();
        if (pairedSTBAddr == null) return false;
        return pairedSTBAddr.equalsIgnoreCase(OGConstants.ETHERNET_HARD_PAIR_IP_ADDRESS);

    }


    public static void setCurrentTVShow(TVShow currentlyOnTV) {

        // FIXME this must be a bug in the poll service!!
        if (currentlyOnTV == null){
            Log.wtf(TAG, "Who the fuck is sending null tv updates??? FIXME");
            return;
        }

        if (mCurrentTVShow==null || !mCurrentTVShow.equals(currentlyOnTV)){
            mCurrentTVShow = currentlyOnTV;
            ABApplication.ottobus.post(mCurrentTVShow);
            // Send it upstream
            new Thread(new Runnable() {
                @Override
                public void run() {
                    BelliniDMAPI.programChange(mCurrentTVShow);
                }
            }).start();
        } else {
            Log.d(TAG, "Got a TV show update, but it is the same as what I have, so ignoring");
        }
    }

    public static TVShow getCurrentTVShow() {
        return mCurrentTVShow;
    }

    public static void changeTVChannel(final int channel){

        if (isPairedToSTB()){

            switch (mPairedSTB.carrier){

                case DIRECTV:

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "Changing channel to: "+channel);
                            DirecTVAPI.changeChannel(getPairedSTBIpAddress(), channel);
                        }
                    }).start();

                    break;

                default:

                    Log.wtf(TAG, "Unsupported carrier for channel change");
            }
        }
    }
    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }


    // Crazy UDID nonsense!!

    // Had to comment this out because on Android 6 it was crashing
    protected static final String _mUDID = "this doesn't work on Android 6"; //_getUDID();

    public static String getUDID(){

        String udid = getStringFromPrefs("devUDID", null);
        if (udid == null){
            udid = UUID.randomUUID().toString();
            putStringToPrefs("devUDID", udid);
        }

        return udid;

    }

    public static String getUDIDScott() {
        return _mUDID;
    }

    protected static synchronized String _getUDID() {

        if (isEmulator()){
            return getNonUniqueUDID(); // for now, this won't fault out on emu
        }
        //return Settings.Secure.getString(ABApplication.sharedContext.getContentResolver(),
        //        Settings.Secure.ANDROID_ID);
        String m_szUniqueID = new String();

        //if (_mUDID == null) {
        // http://www.pocketmagic.net/android-unique-device-id/

            /*
                String tmDevice, tmSerial, tmPhone, androidId;
                tmDevice = "" + tm.getDeviceId();
                tmSerial = "" + tm.getSimSerialNumber();
                androidId = "" + android.provider.Settings.Secure.getString(ABApplication.sharedContext.getContentResolver(), Settings.Secure.ANDROID_ID);

                UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
                String deviceId = deviceUuid.toString();*/

        TelephonyManager TelephonyMgr = (TelephonyManager)ABApplication.sharedContext.getSystemService(Context.TELEPHONY_SERVICE);
        String m_szImei = TelephonyMgr.getDeviceId(); // Requires READ_PHONE_STATE

        String m_szDevIDShort = "35" + //we make this look like a valid IMEI
                Build.BOARD.length()%10+ Build.BRAND.length()%10 +
                Build.CPU_ABI.length()%10 + Build.DEVICE.length()%10 +
                Build.DISPLAY.length()%10 + Build.HOST.length()%10 +
                Build.ID.length()%10 + Build.MANUFACTURER.length()%10 +
                Build.MODEL.length()%10 + Build.PRODUCT.length()%10 +
                Build.TAGS.length()%10 + Build.TYPE.length()%10 +
                Build.USER.length()%10 ; //13 digits

        String m_szAndroidID = Settings.Secure.getString(ABApplication.sharedContext.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        WifiManager wm = (WifiManager)ABApplication.sharedContext.getSystemService(Context.WIFI_SERVICE);
        String m_szWLANMAC = wm.getConnectionInfo().getMacAddress();

        BluetoothAdapter m_BluetoothAdapter    = null; // Local Bluetooth adapter
        m_BluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        String m_szBTMAC = m_BluetoothAdapter.getAddress();

        String m_szLongID = m_szImei + m_szDevIDShort + m_szAndroidID+ m_szWLANMAC + m_szBTMAC;

        // compute md5
        MessageDigest m = null;
        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Log.e("MD5", e.toString());
            return null;
        }
        m.update(m_szLongID.getBytes(),0,m_szLongID.length());
        // get md5 bytes
        byte p_md5Data[] = m.digest();
        // create a hex string

        for (int i=0;i<p_md5Data.length;i++) {
            int b =  (0xFF & p_md5Data[i]);
            // if it is a single digit, make sure it have 0 in front (proper padding)
            if (b <= 0xF) m_szUniqueID+="0";
            // add number to string
            m_szUniqueID+=Integer.toHexString(b);
        }
        // hex string to uppercase
        m_szUniqueID = m_szUniqueID.toUpperCase();
        //}
        return m_szUniqueID;
    }

}


