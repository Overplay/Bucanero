package io.ourglass.bucanero.realm;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.ourglass.bucanero.core.OGSystem;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.Required;

/**
 * Created by ethan on 8/23/16.
 */

public class OGLog extends RealmObject {

    @Required
    public String logType;

    @Required
    public String message; // JSON Data

    public Long loggedAt;
    public Long uploadedAt;

    public String logFile;


    public JSONObject getLogAsJSON(){
        try {
            JSONObject toReturn = new JSONObject();

            toReturn.put("logType", this.logType.toLowerCase());
            toReturn.put("message", new JSONObject(message));
            toReturn.put("loggedAt", loggedAt);
            toReturn.put("uploadedAt", uploadedAt);
            toReturn.put("deviceUDID", OGSystem.getDeviceId());
            return toReturn;

        } catch (JSONException e){
            return null;
        }
    }

    // For consistency
    public JSONObject toJson(){
        return getLogAsJSON();
    }

    public void setType(String type){
        //can add some checking on type string before setting
        this.logType = type;
    }

    public void setMessage(JSONObject obj){
        this.message = obj.toString();
    }

    public JSONObject getMessage(){
        try {
            return new JSONObject(message);
        } catch (JSONException e){
            return null;
        }
    }

    public void setUploaded(){
        if(this.uploadedAt != 0){
            Log.e("OGLog", "Try to reset uploaded time, error");
            throw new Error("Log already marked uploaded");
        }

        this.uploadedAt = System.currentTimeMillis();
    }

    public boolean isUploaded(){

        return this.uploadedAt != 0;
    }

    // Using default values for fields (like createdAt=System.time...) does not work right with realm.
    // So we use a factory method.

    /**
     * Factory method for new OGLog
     * @param realm
     * @return
     */
    public static OGLog getNewLogInstance(Realm realm){
        OGLog newLog = realm.createObject(OGLog.class);
        newLog.loggedAt = System.currentTimeMillis();
        newLog.uploadedAt = 0L;
        newLog.logType = null;
        newLog.message = "{}";
        return newLog;
    }

    public static JSONArray getAllAsJson(Realm realm){
        RealmResults<OGLog> logs = realm.where(OGLog.class).findAll();
        JSONArray rval = new JSONArray();
        for (OGLog log: logs){
            rval.put(log.toJson());
        }
        return rval;
    }

}