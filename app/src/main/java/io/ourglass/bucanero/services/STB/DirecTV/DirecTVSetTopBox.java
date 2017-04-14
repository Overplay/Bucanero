package io.ourglass.bucanero.services.STB.DirecTV;

import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONObject;

import io.ourglass.bucanero.objects.SetTopBox;
import io.ourglass.bucanero.objects.TVShow;
import io.ourglass.bucanero.services.STB.STBPollStatus;

/**
 * Created by mkahn on 11/11/16.
 */

public class DirecTVSetTopBox extends SetTopBox {

    private final static String TAG = "DirecTVSetTopBox";

    public String ssdpResponse = "";
    public String upnpInfoUrl = "";


    public DirecTVSetTopBox(SetTopBoxListener listener, String ipAddress, STBConnectionType connectionType, String ssdpResponse ){
        super(listener, ipAddress, STBCarrier.DIRECTV, connectionType, "");
        this.ssdpResponse = ssdpResponse;
        extractUpnpUrl();
    }

    private void extractUpnpUrl(){

        //TODO: This extraction should be done with REGEX
        try {
            int locIdx = ssdpResponse.toLowerCase().indexOf("location:");
            String s1 = ssdpResponse.substring(locIdx+"location:".length());
            int eol = s1.indexOf("\n");
            String url1 = s1.substring(0, eol);
            String url = url1.trim();
            this.upnpInfoUrl = url;

        } catch (Exception e){
            Log.d(TAG, "There was a problem parsing the SSDP payload info, probably pairing sequence issue, not a biggie.");
            this.upnpInfoUrl = null;
        }


    }

    @Override
    public void updateWhatsOn() {

        final String ipAddr = this.ipAddress;

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                whatsOn();
            }
        });

        t.start();

    }


    @Override
    public TVShow updateWhatsOnSync(){

        return whatsOn();

    }

    private TVShow whatsOn(){

        JSONObject onobj = DirecTVAPI.whatsOn(this.ipAddress);

        if (onobj==null){
            // error situation
            return null;
        }

        TVShow show = new TVShow();
        String fallback = "???";

        show.networkName = onobj.optString("callsign", fallback);
        show.title = onobj.optString("title", fallback);
        show.episodeTitle = onobj.optString("episodeTitle", fallback);
        show.channelNumber = onobj.optString("major", fallback);
        //show.uniqueId = onobj.getString("uniqueId");
        show.programId = onobj.optString("programId", fallback);
        // Assign to parent
        nowPlaying = show;
        lastUpdated = System.currentTimeMillis();

        return show;
    }

    private boolean getModelInfo(){

        this.modelName = DirecTVAPI.modelInfo(upnpInfoUrl);
        this.receiverId = DirecTVAPI.receiverId(this.ipAddress);
        lastUpdated = System.currentTimeMillis();

        return (this.modelName!=null) && (this.receiverId!=null);
    }

    @Override
    public STBPollStatus updateAllSync(){

        STBPollStatus status = new STBPollStatus();

        if (whatsOn()==null){
            status.cleanPoll = false;
            status.lostConnection = true;
        };

        if (!getModelInfo()){
            status.cleanPoll = false;
            status.otherError = true;
            status.message = "Could not poll model name and/or receiver id";
        }

        lastUpdated = System.currentTimeMillis();
        return status;
    }

    @Override
    public String toJsonString(){
        Gson gson = new Gson();
        String json = gson.toJson(this);
        return json;
    }

}

/*

HTTP/1.1 200 OK
Cache-Control: max-age=1800
EXT:
Location: http://10.1.10.118:49152/2/description.xml
Server: Linux/2.6.18.5, UPnP/1.0 DIRECTV JHUPnP/1.0
ST: upnp:rootdevice
USN: uuid:29bbe0e1-1a6e-47f6-8f8d-0003784ebf0c::upnp:rootdevice


 */