package io.ourglass.bucanero.messages;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;

import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.core.OGSystem;

/**
 * Created by mkahn on 5/10/17.
 */

public class OGLogMessage {

    public String logType = "";
    public JSONObject message;
    public long loggedAt;
    public String logFile = "";

    public OGLogMessage(String type, JSONObject message, String file){
        this.logType = type;
        this.message = message;
        this.logFile = file;
        this.loggedAt = System.currentTimeMillis();
    }

    public OGLogMessage(String type, JSONObject message){
        this(type, message, null);
    }

    public static OGLogMessage newHeartbeatLog(){
        return new OGLogMessage("heartbeat", new JSONObject(), null);
    }

    public void post(){
        ABApplication.ottobus.post(this);
    }

    public String toJsonString(){

        JSONObject jobj = new JSONObject();
        try {
            jobj.put("logType", logType);
            jobj.put("message", message);

            DateTime dt = new DateTime(loggedAt);
            DateTimeFormatter fmt = ISODateTimeFormat.dateTime();

            jobj.put("loggedAt", fmt.print(dt));
            jobj.put("logFile", logFile);
            jobj.put("deviceUDID", OGSystem.getUDID());

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jobj.toString();
    }
}