package io.ourglass.bucanero.api;

import android.util.Log;

import org.jdeferred.FailCallback;

import io.ourglass.bucanero.messages.OGLogMessage;
import io.ourglass.bucanero.messages.SystemStatusMessage;

import static io.ourglass.bucanero.messages.SystemStatusMessage.SystemStatus.NETWORK_ISSUE;

/**
 * Created by mkahn on 7/21/17.
 */

public class BelliniNetworkFailureCallback implements FailCallback<Exception> {

    private static String TAG = "BelliniNetworkFail";

    public String description;
    public Integer code;

    public BelliniNetworkFailureCallback(String description, Integer code){
        this.description = description;
        this.code = code;
    }

    @Override
    public void onFail(Exception result) {
        Log.e(TAG, description);
        Log.e(TAG, result.toString());

        OGLogMessage.newOGLog("network_issue")
                .addFieldToMessage("description", description)
                .addFieldToMessage("exception", result.toString()  )
                .addFieldToMessage("issue_code", code)
                .post();

        SystemStatusMessage.sendStatusMessageWithException(NETWORK_ISSUE, result);
    }

}
