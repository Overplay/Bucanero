package io.ourglass.bucanero.services.Connectivity;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;

import io.ourglass.bucanero.core.ABApplication;

/**
 * Created by mkahn on 4/13/17.
 */

public class NetworkingUtils {

    private static final String TAG = "NetworkingUtils";

    public static String getWiFiMACAddress() {
        WifiManager manager = (WifiManager) ABApplication.sharedContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        String macAddress = info.getMacAddress();

        if (macAddress == null) {
            macAddress = "undefined";
        }

        return macAddress;
    }

    public static String getWiFiIPAddressString() {
        WifiManager manager = (WifiManager) ABApplication.sharedContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        int ipAddress = info.getIpAddress();
        // TODO is there a better way to get IP address string? This is deprecated and no one has a good alternative
        @SuppressWarnings("deprecation")
        String ip = Formatter.formatIpAddress(ipAddress);

        return ip;
    }


    /**
     * Returns a hash map with the wlan0 and eth0 ipv4 and ipv6 addresses
     * @return
     */
    public static HashMap<String, String> getDeviceIpAddresses() {
        HashMap<String, String> rval = new HashMap<String, String>();
        Enumeration<NetworkInterface> en = null;
        try {
            en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()){
                NetworkInterface ni = en.nextElement();
                String ifname = ni.getDisplayName();
                if ( ifname.equalsIgnoreCase("wlan0") || ifname.equalsIgnoreCase("eth0")){
                    Enumeration<InetAddress> ips = ni.getInetAddresses();
                    while (ips.hasMoreElements()){
                        InetAddress ina = ips.nextElement();
                        String addr = ina.getHostAddress();
                        if (addr.indexOf(":")>0){
                            rval.put(ifname+"-ipv6", addr );
                        } else {
                            rval.put(ifname, addr);
                        }

                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        return rval;
    }

    public static String getEthernetIPAddressString(){
        return getDeviceIpAddresses().get("eth0");
    }

}



