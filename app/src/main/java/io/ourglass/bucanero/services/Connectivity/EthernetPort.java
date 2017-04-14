package io.ourglass.bucanero.services.Connectivity;

import android.util.Log;

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
            public void results(String results) {
                Log.d(TAG, "UDHCPD>>>>> "+results);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });

        bringUpUdhcpd.exec("su -c /system/bin/busybox udhcpd /mnt/sdcard/wwwaqui/conf/udhcpd.conf");

    }

    public static void bringUpEthernetPort(){

        Log.d(TAG, "Bringing up ethernet interface.");

        Runnable bringUpRunnable = new Runnable() {
            @Override
            public void run() {

                Log.d(TAG+"/ethBringUp", "Bringup up eth0");
                ShellExecutor bringUpEth = new ShellExecutor(new ShellExecutor.ShellExecutorListener() {
                    @Override
                    public void results(String results) {
                        Log.d(TAG, "IFUP>>>>> "+results);
                        launchUDHCPd();
                    }
                });

                bringUpEth.exec("su -c /system/bin/busybox ifconfig eth0 10.21.200.1 netmask 255.255.255.0");
            }
        };

        Thread t = new Thread(bringUpRunnable);
        t.start();


    }




}
