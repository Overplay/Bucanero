package io.ourglass.bucanero.services.SocketIO;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.ourglass.bucanero.core.OGConstants;
import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.Transport;

/**
 * Created by mkahn on 6/1/17.
 */

public class SailsSocketIO {

    private static final String TAG = "SailsSocketIO";

    public static Socket socket;
    private static String mCookie;

    public interface SailsSIOCallback {
        public void sioCallback(Object... args);
    }

    public static Socket getSailsSocket(String cookie) throws URISyntaxException {

        mCookie = cookie;

        IO.Options opts = new IO.Options();

        opts.forceNew = true;
        // This is required because sails does an idiotic version check
        opts.query = "__sails_io_sdk_version=0.11.0";
        opts.reconnection = true;
        opts.reconnectionDelay = 1000; // TODO is this right?
        //opts.timeout = 5000;
        socket = IO.socket(OGConstants.SOCKET_IO_ADDRESS, opts);

        socket.io().on(Manager.EVENT_TRANSPORT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {

                Transport transport = (Transport) args[0];
                transport.on(Transport.EVENT_REQUEST_HEADERS, new Emitter.Listener() {

                    @Override
                    public void call(Object... args) {

                        @SuppressWarnings("unchecked")
                        Map<String, List<String>> headers = (Map<String, List<String>>) args[0];
                        // modify request headers
                        List<String> cookies = new ArrayList<String>();
                        cookies.add(mCookie);
                        headers.put("Cookie", cookies);

                        List<String> auth = new ArrayList<String>();
                        auth.add(OGConstants.DEVICE_AUTH_HDR);
                        headers.put("Authorization", auth);
                    }
                });

                transport.on(Transport.EVENT_RESPONSE_HEADERS, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        //@SuppressWarnings("unchecked")
                        //Map<String, List<String>> headers = (Map<String, List<String>>)args[0];
                        // access response headers
                        //String cookie = headers.get("Set-Cookie").get(0);
                    }
                });
            }
        });

        return socket;

    }

    public static void post(String url, HashMap<String, String> params, final SailsSIOCallback cb){

        Log.d(TAG, "Sails socket.io post to: " + url );

        JSONObject data = new JSONObject();

        for (String key: params.keySet()){
            try {
                data.put(key, params.get(key));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        JSONObject packet = new JSONObject();
        try {
            packet.put("url", url);
            packet.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        doPost(packet, cb);

    }

    public static void post(String url, JSONObject paramsJson, final SailsSIOCallback cb) throws JSONException {
        JSONObject packet = new JSONObject();
        packet.put("url", url);
        packet.put("data", paramsJson);
        doPost(packet, cb);
    }

    private static void doPost(JSONObject jsonObject, final SailsSIOCallback cb){

        SailsSocketIO.socket.emit("post", jsonObject, new Ack() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "records: " + args[0].toString());
                if (cb!=null){
                    cb.sioCallback(args);
                }
            }
        });

    }
}
