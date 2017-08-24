package io.ourglass.bucanero.services.SocketIO.SIOActions;

import android.util.Log;

import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by mkahn on 8/16/17.
 */

public class SIOActionDispatcher {

    private static final String TAG = "SIOActionDispatcher";

    HashMap<String, SIOAction> mActionMap = new HashMap<>();

    public void registerAction(SIOAction action){
        mActionMap.put(action.command, action);
        Log.d(TAG, "Action:  " + action.command + " has been registered. Action count: "+mActionMap.size());

    }

    public boolean processAction(String action, JSONObject inboundObject){
        Log.d(TAG, "Processing inbound action: "+action);
        SIOAction todo = mActionMap.get(action);
        if ( todo == null ){
            return false;
        }
        todo.process(inboundObject);
        return true;
    }

}
