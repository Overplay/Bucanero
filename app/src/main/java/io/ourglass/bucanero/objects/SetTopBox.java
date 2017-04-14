package io.ourglass.bucanero.objects;

import io.ourglass.bucanero.services.STB.STBPollStatus;

/**
 * Created by mkahn on 11/11/16.
 */

public abstract class SetTopBox {

    public String ipAddress = "";
    public STBConnectionType connectionType = STBConnectionType.IPGENERIC;
    public STBCarrier carrier = STBCarrier.DIRECTV;
    public String modelName = "";
    public TVShow nowPlaying;
    public String receiverId = "";
    public long lastUpdated = 0;
    public SetTopBoxListener mListener;

    public static final Object syncLock = new Object();

    // We'll probably use this somewhere down the line
    public enum STBConnectionType {
        WIFI, ETHERNET, MOCA, IPGENERIC, HARD_ETHERNET, HDMI
    }

    public enum STBCarrier {
        DIRECTV, XFINITY
    }

    public interface SetTopBoxListener {
        public void newShowCallback(TVShow newShow);
        //public void newGuideData(JSONObject newChannelData);
    }

    public SetTopBox(){
        this.ipAddress = ""; // here for Gson
    }

    public SetTopBox(SetTopBoxListener listener, String ipAddress, STBCarrier carrier, STBConnectionType connectionType, String modelName ){

        this.ipAddress = ipAddress;
        this.connectionType = connectionType;
        this.carrier = carrier;
        this.modelName = modelName;

    }

    // These will be box specific (DirecTV, Xfinity, etc.)
    public abstract void updateWhatsOn();
    public abstract TVShow updateWhatsOnSync();
    public abstract STBPollStatus updateAllSync();
    //public abstract void updateGuide();

    public abstract String toJsonString();

    public void updateInBackground(){
        // TODO: think about Synclock on these objects. Do we need it? Seems like if we lock here, we
        // should lock everywhere, no? MAK
        synchronized (syncLock){
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    updateAllSync();
                }
            });
            t.start();
        }
    }



}
