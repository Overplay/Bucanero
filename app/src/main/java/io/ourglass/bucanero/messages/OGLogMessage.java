package io.ourglass.bucanero.messages;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;

import io.ourglass.bucanero.core.OGSystem;

/**
 * Created by mkahn on 5/10/17.
 */

public class OGLogMessage extends OttobusMainThreadMessage {

    public String logType = "";
    public JSONObject message;
    public long loggedAt;
    public String logFile = "";

    /**
     * Factory for a heartbeat OGLogMessage
     * @return OGLogMessage for a heartbeat
     */
    public static OGLogMessage newHeartbeatLog(){
        return new OGLogMessage("heartbeat", new JSONObject(), null);
    }

    /**
     * Factory for a bare OGLogMessage
     * @return OGLogMessage for a heartbeat
     */
    public static OGLogMessage newOGLog(String logType){
        return new OGLogMessage(logType, new JSONObject(), null);
    }

    /**
     * Full constructor
     * @param type type of Log message
     * @param message JSONObject to be uploaded
     * @param file Associated file contents, if any. Usually used for LogCats
     */
    public OGLogMessage(String type, JSONObject message, String file){
        this.logType = type;
        this.message = message;
        this.logFile = file;
        this.loggedAt = System.currentTimeMillis();
    }

    /**
     * Convenience constructor for messages with no file to upload
     * @param type
     * @param message
     */
    public OGLogMessage(String type, JSONObject message){
        this(type, message, null);
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

    /**
     * Adds a field of any JSONObject supported type to the message
     * @param key
     * @param value
     * @param <T>
     * @return
     */
    public <T> OGLogMessage addFieldToMessage(String key, T value){
        try {
            this.message.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }
}
