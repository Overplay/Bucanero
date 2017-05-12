package io.ourglass.bucanero.tv.WiFi;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import io.ourglass.bucanero.R;
import io.ourglass.bucanero.core.OGUi;
import io.ourglass.bucanero.services.ConnectivityMonitor;

/**
 * Created by mkahn on 5/5/17.
 */

public class WiFiAdapterPro extends ArrayAdapter<ScanResult> {

    WifiManager mWifiManager;

    public WiFiAdapterPro(Context context, ArrayList<ScanResult> nets, WifiManager wm){
        super(context, 0, nets);
        mWifiManager = wm;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){

        ScanResult net = getItem(position);

        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.wifi_list_elem_layout, parent, false);
            convertView.setPadding(40,10,20,10);
        }

        TextView ssid = (TextView) convertView.findViewById(R.id.wifi_list_elem_ssid);
//        TextView securityDescription = (TextView) convertView.findViewById(R.id.wifi_list_elem_security_desc);
        ImageView wifiLevel = (ImageView) convertView.findViewById(R.id.image_wifi_level_unlocked);

        ssid.setTypeface(OGUi.getRegularFont());
//        securityDescription.setTypeface(poppins);

        wifiLevel.setImageResource(ConnectivityMonitor.isOpenNetwork(net)? R.drawable.wifi_level_unlocked : R.drawable.wifi_level_locked);
        wifiLevel.setImageLevel(mWifiManager.calculateSignalLevel(net.level, 5));

        ssid.setText(net.SSID);

        boolean connected = net.SSID.equalsIgnoreCase(mWifiManager.getConnectionInfo().getSSID().replaceAll("^\"|\"$", ""));

        ((ImageView) convertView.findViewById(R.id.image_wifi_connected)).setVisibility(connected?View.VISIBLE:View.INVISIBLE);

        String securityString = "";
//        if(wifiNetworkInfo.WPA && wifiNetworkInfo.WPA2){
//            securityString = "Secured with WPA/WPA2";
//        }
//        else if(wifiNetworkInfo.WPA){
//            securityString = "Secured with WPA";
//        }
//        else if(wifiNetworkInfo.WPA2){
//            securityString = "Secured with WPA2";
//        }

//        if(WifiManageActivity.CURRENT_CONNECTION_STRING.length() != 0 && wifiNetworkInfo.SSID.equals(WifiManageActivity.CURRENT_CONNECTION_STRING)){
//            securityString = "Connected";
//        }
//        securityDescription.setText(securityString);

        return convertView;
    }


}
