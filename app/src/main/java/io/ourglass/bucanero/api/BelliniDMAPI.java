package io.ourglass.bucanero.api;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.core.OGConstants;
import io.ourglass.bucanero.core.OGSystem;
import io.ourglass.bucanero.objects.NetworkException;
import io.ourglass.bucanero.objects.SetTopBox;
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
    // This will only work until 138 is reset
    // TODO do a fetch and save or create an auto-reg endpoint for OG Office
    public static final String TEMP_OG_OFFICE_VUUID = "7dbfe156-5ffc-4513-99ee-ba5af5704390";
    public static final String BULLPEN_VUUID = "bullpen-hey-battabatta";
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    public static String SESSION_COOKIE;

    public static String fullUrlForApp(String appId){
        return OGConstants.BELLINI_DM_ADDRESS + "/blueline/opp/" + appId + "/app/tv";
    }

    public static void authenticate( String username, String password, final StringCallback cb ){

        JSONObject params = new JSONObject();

        try {
            params.put("email", username);
            params.put("password", password);
            params.put("type", "local");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(JSON, params.toString());

        Request request = new Request.Builder()
                .url(OGConstants.BELLINI_DM_ADDRESS + "/auth/login")
                .post(body)
                .build();

        ABApplication.okclient.newCall(request).enqueue(new Callback() {

            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Log.wtf(TAG, "Was not able to login");
                if (cb!=null){
                    cb.error(new NetworkException(e.getMessage(), 999));
                }

            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200){
                    Log.v(TAG, "Login Request successful");
                    if (response.headers().get("set-cookie")!=null){
                        String cookie = response.headers().get("set-cookie");
                        SESSION_COOKIE = cookie.split(";")[0];
                        if (cb!=null){
                            cb.stringCallback(SESSION_COOKIE);
                        }
                    }
                    String sresp = response.body().string();

                } else {
                    Log.v(TAG, "Was not able to execute login (2). Code: "+response.code());
                    if (cb!=null){
                        cb.error(new NetworkException("Received error code from server", response.code()));
                    }
                }
            }
        });

    }

    public static void registerDeviceWithBellini(final JSONCallback cb){

        JSONObject params = new JSONObject();

        try {
            params.put("deviceUDID", OGSystem.getUDID());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        HTTPTransaction.post(OGConstants.BELLINI_DM_ADDRESS+"/ogdevice/register", params, cb);

    }

    private static JSONCallback getFYICallback(final String message) {
        return new JSONCallback() {
            @Override
            public void jsonCallback(JSONObject jsonData) {
                Log.d(TAG, "FYI: " + message);
            }

            @Override
            public void error(NetworkException e) {
                Log.e(TAG, "FYI FAILED: " + message + " CODE: " + e.statusCode);

            }
        };
    }

    public static void associateDeviceWithVenueUUID(String venueUUID, JSONCallback cb){

        if (cb==null)
            cb = getFYICallback("OGDevice associate with Venue with Bellini");

        JSONObject params = new JSONObject();

        try {
            params.put("deviceUDID", OGSystem.getUDID());
            params.put("venueUUID", venueUUID);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        HTTPTransaction.post(OGConstants.BELLINI_DM_ADDRESS + "/ogdevice/associateWithVenue", params, cb);

    }

    public static void getRegCode(JSONCallback cb){

        if (cb==null)
            cb = getFYICallback("OGDevice wants a code without a callback, this is dumb!");

        JSONObject params = new JSONObject();

        try {
            params.put("deviceUDID", OGSystem.getUDID());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        HTTPTransaction.post(OGConstants.BELLINI_DM_ADDRESS + "/ogdevice/regcode", params, cb);

    }


    public static void getAppStatusFromCloud(final JSONCallback callback){
        String url = OGConstants.BELLINI_DM_ADDRESS+"/ogdevice/appstatus?deviceUDID="+OGSystem.getUDID();
        HTTPTransaction.get(url, callback);
    }

    public static void pingCloud(final JSONCallback callback){
        HTTPTransaction.get(OGConstants.BELLINI_DM_ADDRESS+"/ogdevice/pingcloud", callback);
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

        HTTPTransaction.post(OGConstants.BELLINI_DM_ADDRESS+"/ogdevice/commandack", params, null);

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

        HTTPTransaction.post(OGConstants.BELLINI_DM_ADDRESS+"/ogdevice/commandack", params, null);

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

        HTTPTransaction.post(OGConstants.BELLINI_DM_ADDRESS+"/ogdevice/commandack", params, null);

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

        HTTPTransaction.post(OGConstants.BELLINI_DM_ADDRESS+"/ogdevice/programchange", params, null);

    }

    public static void registerSTBPairing(SetTopBox setTopBox){
        String url = OGConstants.BELLINI_DM_ADDRESS+"/ogdevice/regstbpairing?deviceUDID="+OGSystem.getUDID();
        RequestBody body = RequestBody.create(JSON, setTopBox.toJsonString());
        HTTPTransaction.post(url, body, null);
    }

    public static void getMe(JSONCallback callback){
        String url = OGConstants.BELLINI_DM_ADDRESS+"/ogdevice/findByUDID?deviceUDID="+OGSystem.getUDID();
        HTTPTransaction.get(url, callback);
    }
}
