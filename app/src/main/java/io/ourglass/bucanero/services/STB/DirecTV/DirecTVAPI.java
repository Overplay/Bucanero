package io.ourglass.bucanero.services.STB.DirecTV;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.ourglass.bucanero.core.ABApplication;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by mkahn on 11/11/16.
 */


public class DirecTVAPI {

    public static final String TAG = "DirecTVAPI";
    public static final String DIRECTV_CHANNEL_GET_ENDPOINT = "/tv/getTuned";
    public static final int DIRECTV_PORT = 8080;
    public static final int DIRECTV_API_CONNECTION_TIMEOUT = 15000;

    public static OkHttpClient mClient = ABApplication.okclient.newBuilder()
            .connectTimeout(DIRECTV_API_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(DIRECTV_API_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(DIRECTV_API_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
            .build();

    public static ArrayList<DirecTVSetTopBox> availableSystems = new ArrayList<>();


    public static JSONObject whatsOn(String ipAddress){

        JSONObject rval = null;

        try {

            Request req = new Request.Builder()
                    .url("http://"+ipAddress + ":" + DIRECTV_PORT + DIRECTV_CHANNEL_GET_ENDPOINT)
                    .build();
            Log.d(TAG, "checking channel info on "+ipAddress);
            Response response = mClient.newCall(req).execute();

            if(response.isSuccessful()) {
                rval = new JSONObject(response.body().string());
            }

        } catch (IOException e){
            Log.e(TAG, "IO Exception getting channel info");
        } catch (JSONException e){
            Log.e(TAG, "JSON Exception getting channel info");
        } catch (Exception e){
            Log.e(TAG, "Exception getting channel info");
        }

        return rval;
    }

    public static JSONObject stbInfo(String ipAddress){

        JSONObject rval = null;

        try {

            Request req = new Request.Builder()
                    .url("http://"+ipAddress + ":" + DIRECTV_PORT + "/info/getVersion")
                    .build();
            Log.d(TAG, "checking system info on "+ipAddress);
            Response response = mClient.newCall(req).execute();

            if(response.isSuccessful()) {
                rval = new JSONObject(response.body().string());
            }

        } catch (IOException e){
            Log.e(TAG, "IO Exception getting system info");
        } catch (JSONException e){
            Log.e(TAG, "JSON Exception getting system info");
        } catch (Exception e){
            Log.e(TAG, "Exception getting system info");
        }

        return rval;
    }

    public static String modelInfo(String url){

        String rval = null;

        // First thing we need to do is grab the SSDP more info URL from response

        try {

            Request req = new Request.Builder()
                    .url(url)
                    .build();
            Log.d(TAG, "checking model info on " + url);
            Response response = mClient.newCall(req).execute();

            if(response.isSuccessful()) {

                // We got some crappy XML, need to parse
                String xml = response.body().string();
                String md= "<modelDescription>";
                String mdSlash= "</modelDescription>";

                int modelStartIdx = xml.indexOf(md)+ md.length();
                int modelEndIdx = xml.indexOf(mdSlash);
                rval = xml.substring(modelStartIdx, modelEndIdx);

            }

        } catch (IOException e) {
            Log.e(TAG, "IO Exception getting model info");
        } catch (Exception e){
            Log.e(TAG, "Exception getting model info");
        }

        return rval;

    }

    public static String receiverId(String ipAddress){

        String rval = null;

        // First thing we need to do is grab the SSDP more info URL from response

        try {

            Request req = new Request.Builder()
                    .url("http://"+ipAddress + ":8080/info/getVersion")
                    .build();
            Log.d(TAG, "checking version info on " + ipAddress);
            Response response = mClient.newCall(req).execute();

            if(response.isSuccessful()) {

                JSONObject jobj = new JSONObject(response.body().string());
                rval = jobj.optString("receiverId", "???");

            }

        } catch (IOException e) {
            Log.e(TAG, "IO Exception getting system info");
        } catch (Exception e){
            Log.e(TAG, "Exception getting system info");
        }

        return rval;

    }

    public static JSONObject changeChannel(String ipAddress, int channelNumber){


        JSONObject jobj = null;

        try {

            Request req = new Request.Builder()
                    .url("http://"+ipAddress + ":8080/tv/tune?major="+channelNumber)
                    .build();
            Log.d(TAG, "changing channel on " + ipAddress);
            Response response = mClient.newCall(req).execute();

            if(response.isSuccessful()) {

                jobj = new JSONObject(response.body().string());

            }

        } catch (IOException e) {
            Log.e(TAG, "IO Exception changing channel");
        } catch (Exception e){
            Log.e(TAG, "Exception changing channel");
        }

        return jobj;

    }

}


/*  UPNP XML SALAD

<?xml version="1.0" ?>
<root
    xmlns="urn:schemas-upnp-org:device-1-0">
    <specVersion>
        <major>1</major>
        <minor>0</minor>
    </specVersion>
    <device>
        <deviceType>urn:schemas-upnp-org:device:MediaRenderer:1</deviceType>
        <friendlyName>DIRECTV Mediashare Renderer</friendlyName>
        <manufacturer>DIRECTV</manufacturer>
        <manufacturerURL>http://www.directv.com</manufacturerURL>
        <modelDescription>DIRECTV Plus HD DVR</modelDescription>
        <modelName>MediaRenderer</modelName>
        <modelNumber>1.0</modelNumber>
        <UDN>uuid:29bbe0e1-1a6e-47f6-8f8d-0003784ebf0c</UDN>
        <serviceList>
            <service>
                <serviceType>urn:schemas-upnp-org:service:RenderingControl:1</serviceType>
                <serviceId>urn:upnp-org:serviceId:RenderingControl</serviceId>
                <controlURL>/upnp/control/2/RenderingControl1</controlURL>
                <eventSubURL>/upnp/event/2/RenderingControl1</eventSubURL>
                <SCPDURL>http://10.1.10.118:49152/2/RenderingControlSCPD.xml</SCPDURL>
            </service>
            <service>
                <serviceType>urn:schemas-upnp-org:service:ConnectionManager:1</serviceType>
                <serviceId>urn:upnp-org:serviceId:ConnectionManager</serviceId>
                <controlURL>/upnp/control/2/ConnectionManager1</controlURL>
                <eventSubURL>/upnp/event/2/ConnectionManager1</eventSubURL>
                <SCPDURL>http://10.1.10.118:49152/2/ConnectionManagerSCPD.xml</SCPDURL>
            </service>
        </serviceList>
        <intel_nmpr:X_INTEL_NMPR
            xmlns:intel_nmpr="udn:schemas-intel-com:device-1-0">2.1
        </intel_nmpr:X_INTEL_NMPR>
        <dlna:X_DLNADOC
            xmlns:dlna="udn:schemas-dlna-org:device-1-0">DMP 1.00
        </dlna:X_DLNADOC>
        <directv-hmc></directv-hmc>
    </device>
</root>

 */