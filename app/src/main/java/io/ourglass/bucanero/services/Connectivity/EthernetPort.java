package io.ourglass.bucanero.services.Connectivity;

import android.util.Log;

import java.util.ArrayList;

import io.ourglass.bucanero.Support.ShellExecutor;
import io.ourglass.bucanero.core.OGSystem;

/**
 * Created by mkahn on 3/31/17.
 */

/**
 *
 * Many of the methods in here require that busybox be installed on the system!
 *
 */
public class EthernetPort {

    private static final String TAG = "EthernetPort";

    public static void launchUDHCPd(){

        Log.d(TAG, "Firing up UDHCPD");

        ShellExecutor bringUpUdhcpd = new ShellExecutor(new ShellExecutor.ShellExecutorListener() {
            @Override
            public void results(ArrayList<String> results) {
                Log.d(TAG, "UDHCPD>>>>> "+results);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });

        if (OGSystem.isTronsmart()){
            bringUpUdhcpd.exec("su -c /system/bin/busybox udhcpd /mnt/sdcard/wwwaqui/conf/udhcpd.conf");
        } else if (OGSystem.isRealOG()){
            bringUpUdhcpd.exec("/system/xbin/busybox udhcpd /mnt/sdcard/udhcpd.conf");
        }


    }

    public static void bringUpEthernetPort(){

        Log.d(TAG, "Bringing up ethernet interface.");

        Runnable bringUpRunnable = new Runnable() {
            @Override
            public void run() {

                Log.d(TAG+"/ethBringUp", "Bringup up eth0");
                ShellExecutor bringUpEth = new ShellExecutor(new ShellExecutor.ShellExecutorListener() {
                    @Override
                    public void results(ArrayList<String> results) {
                        Log.d(TAG, "IFUP>>>>> "+results);
                        launchUDHCPd();
                    }
                });

                if (OGSystem.isTronsmart()){
                    bringUpEth.exec("su -c /system/bin/busybox ifconfig eth0 10.21.200.1 netmask 255.255.255.0");
                } else if (OGSystem.isRealOG()){
                    bringUpEth.exec("/system/xbin/busybox ifconfig eth0 10.21.200.1 netmask 255.255.255.0");
                }
            }
        };

        Thread t = new Thread(bringUpRunnable);
        t.start();


    }




}
