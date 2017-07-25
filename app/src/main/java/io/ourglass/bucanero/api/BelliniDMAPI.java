package io.ourglass.bucanero.api;

import android.util.Log;

import org.jdeferred.DoneFilter;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import io.ourglass.bucanero.core.OGSettings;
import io.ourglass.bucanero.core.OGSystem;
import io.ourglass.bucanero.objects.NetworkException;
import io.ourglass.bucanero.objects.SetTopBox;
import io.ourglass.bucanero.objects.TVShow;
import okhttp3.MediaType;
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
        return OGSettings.getBelliniDMAddress() + "/blueline/opp/" + appId + "/app/tv";
    }

    /**
     * Convenience factory for initial params object
     * @return
     */
    private static JSONObject getParamsWithDeviceUDID(){
        JSONObject params = new JSONObject();
        try {
            params.put("deviceUDID", OGSystem.getUDID());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return params;
    }

    public static Promise<String, Exception, Void> authenticateDevice(){

        JSONObject params = new JSONObject();

        try {
            params.put("deviceUDID", OGSystem.getUDID());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        DeferredRequest dr = DeferredRequest.post(OGSettings.getBelliniDMAddress() + "/auth/device", params, Response.class);
        return dr.go()
                .then(new DoneFilter<Response, String>() {

                    @Override
                    public String filterDone(Response response) {

                        Log.v(TAG, "Device Login Request successful");
                        if (response.headers().get("set-cookie")!=null){
                            String cookie = response.headers().get("set-cookie");
                            SESSION_COOKIE = cookie.split(";")[0];
                        }

                        return SESSION_COOKIE;
                    }
                });

    }


    // Not used currently
    public static Promise<String, Exception, Integer> authenticateUser(String username, String password){

        JSONObject params = new JSONObject();

        try {
            params.put("email", username);
            params.put("password", password);
            params.put("type", "local");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        DeferredRequest dr = DeferredRequest.post(OGSettings.getBelliniDMAddress() + "/auth/login", params, Response.class);
        return dr.go()
                .then(new DoneFilter<Response, String>() {

                    @Override
                    public String filterDone(Response response) {

                        Log.v(TAG, "Login Request successful");
                        if (response.headers().get("set-cookie")!=null){
                            String cookie = response.headers().get("set-cookie");
                            SESSION_COOKIE = cookie.split(";")[0];
                        }

                        String sresp = null;

                        try {
                            sresp = response.body().string();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        return sresp;
                    }
                });

    }

    public static Promise<JSONObject, Exception, Void> registerDeviceWithBellini(){

        return DeferredRequest.post(OGSettings.getBelliniDMAddress() + "/ogdevice/register", getParamsWithDeviceUDID(), JSONObject.class).go();

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

    public static Promise<JSONObject, Exception, Void> associateDeviceWithVenueUUID(String venueUUID){

        return DeferredRequest.post(OGSettings.getBelliniDMAddress() + "/ogdevice/associateWithVenue", getParamsWithDeviceUDID(), JSONObject.class).go();

    }

//    public static void associateDeviceWithVenueUUIDOld(String venueUUID, JSONCallback cb){
//
//        if (cb==null)
//            cb = getFYICallback("OGDevice associate with Venue with Bellini");
//
//        JSONObject params = new JSONObject();
//
//        try {
//            params.put("deviceUDID", OGSystem.getUDID());
//            params.put("venueUUID", venueUUID);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        HTTPTransaction.post(OGSettings.getBelliniDMAddress() + "/ogdevice/associateWithVenue", params, cb);
//
//    }


    public static Promise<JSONObject, Exception, Void> getRegCode(){
        return DeferredRequest.post(OGSettings.getBelliniDMAddress() + "/ogdevice/regcode", getParamsWithDeviceUDID(), JSONObject.class).go();
    }

    public static Promise<JSONObject, Exception, Void> getAppStatusFromCloud(){
        String url = OGSettings.getBelliniDMAddress()+"/ogdevice/appstatus?deviceUDID="+OGSystem.getUDID();
        return DeferredRequest.get(url, JSONObject.class).go();
    }

    public static Promise<JSONObject, Exception, Void> pingCloud(){
        return DeferredRequest.get(OGSettings.getBelliniDMAddress()+"/ogdevice/pingcloud", JSONObject.class).go();
    }

    public static Promise<JSONObject, Exception, Void> appLaunchAck(String appId, int layoutSlot){

        JSONObject params = getParamsWithDeviceUDID();

        try {
            params.put("appId", appId);
            // Not actuall used
            // TODO we should record this for a cold boot
            params.put("layoutSlot", layoutSlot);
            params.put("command", "launch");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return DeferredRequest.post(OGSettings.getBelliniDMAddress()+"/ogdevice/commandack", params, JSONObject.class).go();

    }

    public static Promise<JSONObject, Exception, Void> appKillAck(String appId){

        JSONObject params = getParamsWithDeviceUDID();

        try {
            params.put("appId", appId);
            params.put("command", "kill");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return DeferredRequest.post(OGSettings.getBelliniDMAddress()+"/ogdevice/commandack", params, JSONObject.class).go();

    }

    public static Promise<JSONObject, Exception, Void> appMoveAck(String appId, int slot){

        JSONObject params = getParamsWithDeviceUDID();

        try {
            params.put("appId", appId);
            params.put("command", "move");
            params.put("slot", slot);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return DeferredRequest.post(OGSettings.getBelliniDMAddress()+"/ogdevice/commandack", params, JSONObject.class).go();

    }

    public static Promise<JSONObject, Exception, Void> programChange(TVShow newShow){

        if (newShow==null){
            Log.wtf(TAG, "Fix null newShow bullshit, mitch");
            return new DeferredObject<JSONObject, Exception, Void>().reject(new Exception("New show is null"));
        }

        JSONObject params = getParamsWithDeviceUDID();

        try {
            params.put("tvShow", newShow.toJsonString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return DeferredRequest.post(OGSettings.getBelliniDMAddress()+"/ogdevice/programchange", params, JSONObject.class).go();

    }

    public static Promise<JSONObject, Exception, Void> registerSTBPairing(SetTopBox setTopBox){

        String url = OGSettings.getBelliniDMAddress() +"/ogdevice/regstbpairing?deviceUDID="+OGSystem.getUDID();
        RequestBody body = RequestBody.create(JSON, setTopBox.toJsonString());
        return DeferredRequest.post(url, body, JSONObject.class).go();

    }

    public static Promise<JSONObject, Exception, Void> getMe(){

        String url = OGSettings.getBelliniDMAddress() +"/ogdevice/findByUDID?deviceUDID="+OGSystem.getUDID();
        return DeferredRequest.getJsonObject(url).go();
    }
}
