package io.ourglass.bucanero.services.SSDP;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by mkahn on 11/13/16.
 */

public class SSDPHandlerThread extends HandlerThread {

    public static final String TAG = "OGDiscoHandlerThread";
    public String[] mDevices;
    public SSDPListener mListener;
    public Context mContext;
    private Handler mWorkerHandler;
    HashSet<String> addresses = new HashSet<>();
    HashMap<String, String> devices = new HashMap<>();

    public interface SSDPListener {
        public void foundDevices(HashMap<String, String> devices, HashSet<String> addresses);
        public void encounteredError(String errString);
    }

    public SSDPHandlerThread(String name) {
        super(name);
    }

    public void start(Context context, SSDPListener listener){

        super.start();
        mListener = listener;
        mContext = context;
        mWorkerHandler = new Handler(getLooper());

    }

    public void discover(){
        mWorkerHandler.post(discoveryRunnable);
    }

    Runnable discoveryRunnable = new Runnable() {
        @Override
        public void run() {

            WifiManager wifi = (WifiManager) mContext.getSystemService(mContext.getApplicationContext().WIFI_SERVICE);

            if (wifi != null) {

                WifiManager.MulticastLock lock = wifi.createMulticastLock("The Lock");
                lock.acquire();

                DatagramSocket socket = null;
                addresses.clear();
                devices.clear();

                try {

                    InetAddress group = InetAddress.getByName("239.255.255.250");
                    int port = 1900;

                    String query =
                            "M-SEARCH * HTTP/1.1\r\n" +
                                    "HOST: 239.255.255.250:1900\r\n" +
                                    "MAN: \"ssdp:discover\"\r\n" +
                                    "MX: 2\r\n" +   // do not respond after 2 seconds, plenty of time
                                    "ST: ssdp:all\r\n" +
                                    "\r\n";

                    socket = new DatagramSocket(null);
                    socket.setReuseAddress(true);
                    socket.setBroadcast(true);
                    socket.bind(new InetSocketAddress(port));

                    DatagramPacket dgram = new DatagramPacket(query.getBytes(), query.length(),
                            group, port);
                    socket.send(dgram);

                    // 6 seconds of inactivity and we're out
                    socket.setSoTimeout(4000);

                    // Let's consider all the responses we can get in 1 second
                    while ( true ) {
                        DatagramPacket p = new DatagramPacket(new byte[1200], 1200);
                        socket.receive(p);

                        String s = new String(p.getData(), 0, p.getLength());
                        Log.d( TAG, "Got response from: "+p.getAddress().getHostAddress());

                        if (s.startsWith("HTTP/1.1 200")){
                            addresses.add(p.getAddress().getHostAddress());
                            devices.put(p.getAddress().getHostAddress(), s);
                        }

                    }

                } catch (SocketTimeoutException e){
                    Log.d( TAG, "Socket timed out waiting for more data, we're done searching");
                    mListener.foundDevices(devices, addresses);
                } catch (SocketException e) {
                    e.printStackTrace();
                    mListener.encounteredError(e.toString());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    mListener.encounteredError(e.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                    mListener.encounteredError(e.toString());
                } finally {
                    if (socket!=null){
                        socket.close();
                    }
                }
                lock.release();
            }
        }
    };


}
