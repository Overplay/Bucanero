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

    public static final String CODE_REV_NAME = "Bucanero";
    public static final String DEVICE_AUTH_HDR = "x-ogdevice-1234";
    public static final boolean FORCE_EMULATOR = false;  // use this to run on a tablet for testing

    // Normally, you can't venue pair if a venue is set, this is for testing
    public static final boolean FORCE_VENUE_PAIR = false;

    public static final int BOOT_DELAY = 500; //ms between boot steps in Slow Boot Mode

    public static final String BELLINI_DM_PRODUCTION_ADDRESS = "https://cloud-dm.ourglass.tv";
    public static final String BELLINI_DM_DEV_ADDRESS = "http://138.68.230.239:2001";
    public static final String BELLINI_DM_EMU_LOCAL_ADDRESS = "http://10.0.2.2:2001";
    //when debugging with a local instance of Bellini running on a Mac. Set this to true and set IP
    //to your workstation IP address
    public static final Boolean USE_LOCAL_DM_SERVER = false;
    public static final String BELLINI_DM_LAN_LOCAL_ADDRESS = "http://192.168.1.172:2001";

    // Force automatic venue registration to the OG Office in Campbell
    public static final Boolean AUTO_REG_TO_OGOFFICE = false;

    // How far down from top is the widget limit (or up from bottom)
    public static final float WIDGET_Y_INSET = 0.12f;

    public static final boolean ENABLE_RESTART_ON_UNCAUGHT_EXCEPTIONS = false;

    public static final boolean USE_NATIVE_ANDROID_WIFI_SETTINGS = true;

    // TEST and DEBUG
    public static final boolean TEST_MODE = true;
    public static final boolean CRASH_TEST_DUMMY = false; // enable force crash on '5' button
    public static final boolean SHOW_DB_TOASTS = true;
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

    /**
     * AUDIO STREAMER STUFF
     */

    // FFMPEG can be started as a service or as a simple obj.
    // Set to true to go back to deprecated service.
    public static final boolean FFMPEG_START_AS_SERVICE = false;

    // nginx barfs with the port 80 explicit
    //public static final String BELLINI_AUDIO_SERVER_ADDRESS = "https://cloud-listen.ourglass.tv/as/";
    //public static final String BELLINI_AUDIO_SERVER_ADDRESS = "http://192.241.217.88:3000/as/";
    public static final String BELLINI_AUDIO_SERVER_ADDRESS = "http://192.241.217.88:4000/as/";
    public static final String BELLINI_AUDIO_SERVER_SECRET = "supersecret";

    /**
     * Audio loop constants
     */
    public static final int BUCANERO_AUDIO_BUFFERFILLTIME_MS = 100;
    public static final int BUCANERO_AUDIO_BUFFERSIZE_BYTES = 4096;

    /**
     * Audio rate constants
     */
    public static final int BUCANERO_AV_V_MAXWIDTH   = 1920;
    public static final int BUCANERO_AV_V_MAXHEIGHT  = 1080;
    public static final int BUCANERO_AV_V_BITRATE    = 20000000;
    public static final int BUCANERO_AV_A_BITRATE    = 64000;
    public static final int BUCANERO_AV_A_SAMPLERATE = 44100;
    public static final int BUCANERO_AV_A_CHANNELS   = 2;

    /**
     * Audio ffMPEG command
     *
     * For testing...
     *  ffmpeg
     *   -hide_banner -loglevel quiet -nostats
     *   -re -stream_loop -1
     *   -i <fn>
     *   -dn -sn -vn -bsf:v dump_extra
     *   -codec:a mp2 -b:a 128k -ar 44100 -ac 2 -muxdelay 0.001 -f mpegts http://localhost:3000/supersecret/abc
     */
    public static final String BUCANERO_FFMPEG_CMD_OPTS_SILENCE = new String(" -hide_banner -loglevel quiet -nostats");
    public static final String BUCANERO_FFMPEG_CMD_OPTS = new String(
            " -i -" +
                    " -dn -sn -vn" +
                    //" -codec:v mpeg1video" +
                    //" -b:v 1000k" +
                    //" -s 320x240" +
                    //
                    " -bsf:v dump_extra" +
                    " -codec:a mp2" +
                    " -b:a 128k" +
                    " -ar 44100" +
                    " -ac 2" +
                    //
                    //" -bf 16" +
                    //" -bsf:v dump_extra" +
                    //" -muxdelay 0.001" +
                    " -f mpegts" +
                    " " +
            "");

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
