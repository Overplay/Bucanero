package io.ourglass.bucanero.core;

import java.util.regex.Pattern;

/**
 * Created by mkahn on 5/17/16.
 */
public class OGConstants {

    // Bucanero stuff

    /**
     * SocketIO constants
     */
    public static final Boolean USE_LOCAL_SERVER = false;
    public static final String CODE_REV_NAME = "Bucanero";

    public static final boolean FORCE_EMULATOR = false;  // use this to run on a tablet for testing

    // Normally, you can't venue pair if a venue is set, this is for testing
    public static final boolean FORCE_VENUE_PAIR = true;

    public static final int BOOT_DELAY = 500; //ms between boot steps in Slow Boot Mode

    public static String BELLINI_DM_ADDRESS = USE_LOCAL_SERVER ? "http://10.0.2.2:2001" : "https://cloud-dm.ourglass.tv";
    //public static String BELLINI_DM_ADDRESS = USE_LOCAL_SERVER ? "http://10.0.2.2:2001" : "http://138.68.230.239:2001";
    //public static String BELLINI_DM_ADDRESS = USE_LOCAL_SERVER ? "http://10.0.2.2:2001" : "http://192.168.1.172:2001";

    public static String SOCKET_IO_ADDRESS = BELLINI_DM_ADDRESS; //alias

    // Force automatic venue registration to the OG Office in Campbell
    public static final Boolean AUTO_REG_TO_OGOFFICE = true;

    // How far down from top is the widget limit (or up from bottom)
    public static final float WIDGET_Y_INSET = 0.12f;

    public static final boolean ENABLE_RESTART_ON_UNCAUGHT_EXCEPTIONS = false;

    public static final boolean TEST_MODE = true;
    public static final boolean CRASH_TEST_DUMMY = true; // enable force crash on '5' button
    public static final boolean SHOW_DB_TOASTS = false;
    public static final boolean LOGCAT_TO_FILE = true; // Off until I am sure it works [mak]




    /**
     * PROGRAM GUIDE CONSTANTS FOR ALPHA
     */

    public static final String AJPGS_BASE = "http://107.170.209.248:1338";
    public static final int AJPGS_DIRECTV_PROVIDER_ID = 195;

    /**
     * Networking constants
     */
    public static final int TV_POLL_INTERVAL = 5000;
    public static final int TV_DISCOVER_INTERVAL = 1000 * 60;
    public static  final int LOG_UPLOAD_INTERVAL = 1000 * 60 * 2; // 2 minutes

    public static final int HEARTBEAT_TIMER_INTERVAL = 1000 * 60 * 5; //5 minutes

    public static final int STB_SERVICE_CHANNEL_POLL_INTERVAL = 60 * 1000; //every two seconds upon completion

    public static Pattern LOC_PATTERN = Pattern.compile("location[^\n]*", Pattern.CASE_INSENSITIVE);

    /* UPNP Discovery Packet
    M-SEARCH * HTTP/1.1
    HOST: 239.255.255.250:1900
    MAN: ssdp:discover
    MX: 10
    ST: ssdp:all
     */


    public static final String BELLINI_ADDRESS = "http://138.68.230.239:2001";

    public static final String BELLINI_API_PREFIX = "/api/v1/";
    public static final String ASAHI_ACCEPTED_AD_ENDPOINT = "/ad/getAccepted";
    public static final String ASAHI_VENUE_AD_ENDPOINT = "/ad/forVenue/";
    public static final String BELLINI_MEDIA_ENDPOINT = "/media/download/";

//    public static final String INTERNAL_PATH_TO_MEDIA = "/data/data/io.ourglass.amstelbright2/media/";
//    public static final String EXTERNAL_PATH_TO_MEDIA = "/api/admedia/";

    // When using git, use the one below
    //public static final String PATH_TO_ABWL = "/Android/data/me.sheimi.sgit/files/repo/AmstelBrightLimeWWW";


    // When manually pushing
    // Keep separate from DEMO release so we can run both on the same H/W
    public static final String PATH_TO_ABWL = "/wwwaqui";



    public static enum BootState {
        ABS_START(0),
        UDP_START(1),
        UPGRADE_START(2),
        HTTP_START(3);

        private final int mState;

        private BootState(int state){
            mState = state;
        }

        public int getValue(){
            return mState;
        }
    }

    //normally this is FALSE, set to test effects of hard pairing
    public static final boolean SIMULATE_HARD_PAIR = false;
    public static final String ETHERNET_HARD_PAIR_IP_ADDRESS = "10.21.200.2";

    public static final String[] WIFI_STRENGTH_LEVELS = new String[]{"None", "Poor", "Fair", "Good", "Excellent"};

    public static final int BUTTON_CLICK_DEBOUNCE = 2000;

    // HARD CODED for DEMO
    public static final int STB_PORT = 8080;
    public static final String STB_TUNED_ENDPOINT = "/tv/getTuned";

    /**
     * constants for JWT
     */

    public static boolean USE_JWT = true;
    public static final int JWT_LIFESPAN = 1 /*day*/ * 24 /*hours*/ * 60 /*minutes*/ * 60 /*seconds*/ * 1000 /*milliseconds*/;
    public enum AUTH_LEVEL {PATRON, OWNER, OG};

    public static final String TEST_DIRECT_TV_INFO =
                 "{\n" +
                "  \"callsign\": \"ESPNHD\",\n" +
                "  \"date\": \"20160603\",\n" +
                "  \"duration\": 14400,\n" +
                "  \"isOffAir\": false,\n" +
                "  \"isPclocked\": 3,\n" +
                "  \"isPpv\": false,\n" +
                "  \"isRecording\": false,\n" +
                "  \"isVod\": false,\n" +
                "  \"major\": 206,\n" +
                "  \"minor\": 65535,\n" +
                "  \"offset\": 652,\n" +
                "  \"programId\": \"36417953\",\n" +
                "  \"rating\": \"No Rating\",\n" +
                "  \"startTime\": 1464994800,\n" +
                "  \"stationId\": 2220255,\n" +
                "  \"status\": {\n" +
                "    \"code\": 200,\n" +
                "    \"commandResult\": 0,\n" +
                "    \"msg\": \"OK.\",\n" +
                "    \"query\": \"/tv/getTuned\"\n" +
                "  },\n" +
                "  \"title\": \"X Games\"\n" +
                "}";


}
