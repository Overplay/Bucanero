package io.ourglass.bucanero.services.OGDP;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

import io.ourglass.bucanero.core.ABApplication;
import io.ourglass.bucanero.core.OGSystem;


/**
 * Created by ethan on 8/3/16.
 *
 * Modified a tad by MAK
 *
 * Renamed to OGDPService (Ourglass Discovery Protocol)
 *
 */

public class OGDPService extends Service {

    private String TAG = "OGDPService";
    public static final int OGDP_PORT = 9091;
    public static OGDPService sService;

    /**
     * Returns a direct reference to the running service. Could be null if service has not started!
     * @return
     */
    public static OGDPService getInstance(){
        return sService;
    }

    private String mMessage;
    private int mPort = OGDP_PORT;

    private DatagramSocket mSocket;
    private Thread udpListenThread;
    private boolean threadAlive;
    private Random mRandom = new Random();


    // Adds up to 500ms to a delayed response so all OGs aren't responding at the same time.
    private long randomDelayFrom(int start){
        return mRandom.nextInt(500) + start;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate(){

        Log.d(TAG, "onCreate");
        sService = this;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){

        ABApplication.dbToast(this, "starting udp listener");

        threadAlive = true;
        udpListenThread = new Thread(new Runnable(){
            public void run(){
                while(threadAlive){
                    try {
                        listenForUDPBroadcast();
                    } catch(IOException e){
                        Log.e(TAG, "listen errored: " + e.getMessage());
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Listen was interrupted. App is likely shutting down.");
                    }
                }
                Log.v(TAG, "Listen and Respond thread has been killed");
            }
        });

        udpListenThread.start();

        return Service.START_STICKY;
    }

    private void listenForUDPBroadcast() throws IOException, InterruptedException{

        byte[] buffer = new byte[512];
        if(mSocket == null || mSocket.isClosed()){
            mSocket = new DatagramSocket(mPort);
            mSocket.setBroadcast(true);
        }

        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
        Log.v( TAG, "UDP listener sitting on port: "+mPort);
        mSocket.receive(receivedPacket);

        String rxString = new String(receivedPacket.getData()).trim();

        Log.v(TAG, "received UDP packet from " + receivedPacket.getAddress() + " "
                + rxString);

        //TODO: add some parsing of the packet before issuing a response

        int responseDelay = 50; // Respond essentially right away by default

        try {
            JSONObject inbound = new JSONObject(rxString);
            int inboundDelay = inbound.optInt("delay", 50);
            responseDelay = inboundDelay; // You will only get here if you have valid JSON
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //MAK sending 2 more additional responses to help with shitty networks...
        respondToUDP(receivedPacket.getAddress(), responseDelay);
        respondToUDP(receivedPacket.getAddress(), responseDelay*2);
        respondToUDP(receivedPacket.getAddress(), responseDelay*3);

    }

    private void respondToUDP(InetAddress receivedFrom, int responseDelay){
        try {
            mMessage = OGSystem.getSystemInfo().toString();
            DatagramPacket packet = new DatagramPacket(mMessage.getBytes(), mMessage.length(), receivedFrom, mPort);
            Thread.sleep(randomDelayFrom(responseDelay));
            mSocket.send(packet);
        } catch (IOException e){
            Log.e(TAG, "Couldn't respond to " + receivedFrom + " - " + e.getMessage());
        } catch (InterruptedException e) {
            Log.d(TAG, "Interrupted sleeping before responding to incoming inquiry. Assuming app dying.");
            threadAlive = false; // Thread will die when I get back to the top.
        }
    }

    public void onDestroy(){
        Log.d(TAG, "onDestroy");
        threadAlive = false;
    }
}
