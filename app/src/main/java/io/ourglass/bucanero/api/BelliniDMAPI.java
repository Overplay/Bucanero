package io.ourglass.bucanero.api;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.core.JSONCallback;
import io.ourglass.bucanero.core.OGConstants;
import io.ourglass.bucanero.core.OGSystem;
import io.ourglass.bucanero.objects.TVShow;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by mkahn on 3/13/17.
 */

public class BelliniDMAPI {

    public static final String TAG = "BelliniDMAPI";
    public static final String TEMP_OG_OFFICE_VUUID = "7dbfe156-5ffc-4513-99ee-ba5af5704390";
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    public static String fullUrlForApp(String appId){
        return OGConstants.BELLINI_DM_ADDRESS + "/blueline/opp/" + appId + "/app/tv";
    }

    public static void registerDeviceWithBellini(){

        JSONObject params = new JSONObject();

        try {
            params.put("deviceUDID", OGSystem.getUDID());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(JSON, params.toString());

        Request request = new Request.Builder()
                .url(OGConstants.BELLINI_DM_ADDRESS+"/ogdevice/register")
                .post(body)
                .build();

        ABApplication.okclient.newCall(request).enqueue(new Callback() {

            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Log.wtf(TAG, "OGDevice failed to register with Bellini");
            }

            @Override public void onResponse(Call call, Response response) throws IOException {

                if (response.code() == 200){
                    Log.d(TAG, "OGDevice successfully registered with Bellini, yay!");
                } else {
                    Log.e(TAG, "OGDevice FAILED registering with Bellini. CODE: "+response.code());

                }
            }
        });
    }

    public static void associateDeviceWithVenueUUID(String venueUUID){

        JSONObject params = new JSONObject();

        try {
            params.put("deviceUDID", OGSystem.getUDID());
            params.put("venueUUID", venueUUID);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(JSON, params.toString());

        Request request = new Request.Builder()
                .url(OGConstants.BELLINI_DM_ADDRESS+"/ogdevice/associateWithVenue")
                .post(body)
                .build();

        ABApplication.okclient.newCall(request).enqueue(new Callback() {

            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Log.wtf(TAG, "OGDevice failed to associate with Venue with Bellini");
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200){
                    Log.d(TAG, "OGDevice successfully associated with Venue with Bellini, yay!");
                } else {
                    Log.e(TAG, "OGDevice FAILED associating venue with Bellini. CODE: "+response.code());

                }
            }
        });
    }

    public static void getAppStatusFromCloud(final JSONCallback callback){

        Request request = new Request.Builder()
                .url(OGConstants.BELLINI_DM_ADDRESS+"/ogdevice/appstatus?deviceUDID="+OGSystem.getUDID())
                .build();

        ABApplication.okclient.newCall(request).enqueue(new Callback() {

            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Log.wtf(TAG, "Failed trying to get app status from Bellini");
            }

            @Override public void onResponse(Call call, Response response) throws IOException {

                String respString = response.body().string();

                if (response.isSuccessful()) {
                    try {
                        JSONObject rval = new JSONObject(respString);
                        callback.jsonCallback(rval);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callback.error(new Error("Malformed return JSON"));
                    }
                } else {
                    callback.error(new Error("Something bad happened"));
                }


            }
        });

    }

    public static void pingCloud(final JSONCallback callback){

        Request request = new Request.Builder()
                .url(OGConstants.BELLINI_DM_ADDRESS+"/ogdevice/pingcloud")
                .build();

        ABApplication.okclient.newCall(request).enqueue(new Callback() {

            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Log.wtf(TAG, "Failed trying to ping Bellini");
            }

            @Override public void onResponse(Call call, Response response) throws IOException {

                String respString = response.body().string();

                if (response.isSuccessful()) {
                    try {
                        JSONObject rval = new JSONObject(respString);
                        callback.jsonCallback(rval);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callback.error(new Error("Malformed return JSON"));
                    }
                }


            }
        });

    }

    public static void appLaunchAck(String appId, int layoutSlot){

        JSONObject params = new JSONObject();

        try {
            params.put("deviceUDID", OGSystem.getUDID());
            params.put("appId", appId);
            // Not actuall used
            // TODO we should record this for a cold boot
            params.put("layoutSlot", layoutSlot);
            params.put("command", "launch");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(JSON, params.toString());

        Request request = new Request.Builder()
                .url(OGConstants.BELLINI_DM_ADDRESS+"/ogdevice/commandack")
                .post(body)
                .build();

        execRequest(request);
    }

    public static void appKillAck(String appId){

        JSONObject params = new JSONObject();

        try {
            params.put("deviceUDID", OGSystem.getUDID());
            params.put("appId", appId);
            params.put("command", "kill");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(JSON, params.toString());

        Request request = new Request.Builder()
                .url(OGConstants.BELLINI_DM_ADDRESS+"/ogdevice/commandack")
                .post(body)
                .build();

        execRequest(request);
    }

    public static void appMoveAck(String appId, int slot){

        JSONObject params = new JSONObject();

        try {
            params.put("deviceUDID", OGSystem.getUDID());
            params.put("appId", appId);
            params.put("command", "move");
            params.put("slot", slot);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(JSON, params.toString());

        Request request = new Request.Builder()
                .url(OGConstants.BELLINI_DM_ADDRESS+"/ogdevice/commandack")
                .post(body)
                .build();

        execRequest(request);
    }

    // TODO should we do something other than throw this on the floor if it fails?
    public static void programChange(TVShow newShow){

        if (newShow==null){
            Log.wtf(TAG, "Fix null newShow bullshit, mitch");
            return;
        }

        JSONObject params = new JSONObject();

        try {
            params.put("deviceUDID", OGSystem.getUDID());
            params.put("tvShow", newShow.toJsonString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(JSON, params.toString());

        Request request = new Request.Builder()
                .url(OGConstants.BELLINI_DM_ADDRESS+"/ogdevice/programchange")
                .post(body)
                .build();

        execRequest(request);
    }

    private static void execRequest(Request request){
        ABApplication.okclient.newCall(request).enqueue(new Callback() {

            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Log.wtf(TAG, "Was not able to execute request (1)");
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200){
                    Log.d(TAG, "Request successful with Bellini, yay!");
                } else {
                    Log.d(TAG, "Was not able to execute request (2). Code: "+response.code());

                }
            }
        });
    }
}
