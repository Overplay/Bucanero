package io.ourglass.bucanero.api;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import io.ourglass.bucanero.core.JSONCallback;
import io.ourglass.bucanero.core.OGConstants;
import io.ourglass.bucanero.core.OGSystem;
import io.ourglass.bucanero.objects.NetworkException;
import io.ourglass.bucanero.objects.SetTopBox;
import io.ourglass.bucanero.objects.TVShow;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * Created by mkahn on 3/13/17.
 */

public class BelliniDMAPI {

    public static final String TAG = "BelliniDMAPI";
    // This will only work until 138 is reset
    // TODO do a fetch and save or create an auto-reg endpoint for OG Office
    public static final String TEMP_OG_OFFICE_VUUID = "7dbfe156-5ffc-4513-99ee-ba5af5704390";
    public static final String LIMBO_VUUID = "limbo-limbo-limbo";
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    public static String fullUrlForApp(String appId){
        return OGConstants.BELLINI_DM_ADDRESS + "/blueline/opp/" + appId + "/app/tv";
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

    public static void associateDeviceWithVenueUUID(String venueUUID){

        JSONObject params = new JSONObject();

        try {
            params.put("deviceUDID", OGSystem.getUDID());
            params.put("venueUUID", venueUUID);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        HTTPTransaction.post(OGConstants.BELLINI_DM_ADDRESS + "/ogdevice/associateWithVenue", params, new JSONCallback() {
            @Override
            public void jsonCallback(JSONObject jsonData) {
                Log.d(TAG, "OGDevice successfully associated with Venue with Bellini, yay!");
            }

            @Override
            public void error(NetworkException e) {
                Log.e(TAG, "OGDevice FAILED associating venue with Bellini. CODE: "+e.statusCode);

            }
        });


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

}
