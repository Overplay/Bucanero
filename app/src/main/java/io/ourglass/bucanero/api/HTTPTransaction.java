package io.ourglass.bucanero.api;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.objects.NetworkException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static io.ourglass.bucanero.api.BelliniDMAPI.JSON;

/**
 * Created by mkahn on 4/19/17.
 */

public class HTTPTransaction {

    public static final String TAG = "HTTPTransaction";
    public static final int TRANSACTION_FAILURE = 100;
    public static final int MALFORMED_JSON_RESPONSE = 101;

    public static void post(String url, JSONObject params, JSONCallback cb){
        RequestBody body = RequestBody.create(JSON, params.toString());
        post(url, body, cb);
    }

    public static void post(String url, RequestBody body, JSONCallback cb){

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        execRequest(request, cb);

    }

    public static void get(String url, JSONCallback cb){

        Request request = new Request.Builder()
                .url(url)
                .build();

        execRequest(request, cb);

    }


    private static void execRequest(Request request, final JSONCallback cb){

        ABApplication.okclient.newCall(request).enqueue(new Callback() {

            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Log.wtf(TAG, "Was not able to execute request (1)");
                if (cb!=null){
                    cb.error(new NetworkException(e.getMessage(), TRANSACTION_FAILURE));
                }
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200){
                    Log.v(TAG, "HTTP Request successful, converting to JSON");
                    if (cb!=null){
                        try {
                            cb.jsonCallback(new JSONObject(response.body().string()));
                        } catch (JSONException e) {
                            cb.error(new NetworkException(e.getMessage(), MALFORMED_JSON_RESPONSE));
                        }
                    }

                } else {
                    Log.v(TAG, "Was not able to execute request (2). Code: "+response.code());
                    if (cb!=null){
                        cb.error(new NetworkException("Received error code from server", response.code()));
                    }
                }
            }
        });
    }
}
