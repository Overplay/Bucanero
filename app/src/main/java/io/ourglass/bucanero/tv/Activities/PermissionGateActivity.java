package io.ourglass.bucanero.tv.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TextView;

import io.ourglass.bucanero.R;
import io.ourglass.bucanero.core.ABApplication;


public class PermissionGateActivity extends BaseFullscreenActivity {

    public static final String TAG = "PermissionGate";

    TextView mMessageTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate in PermissionGate");
        setContentView(R.layout.activity_permission_gate);
        mMessageTV = (TextView) findViewById(R.id.permissionMessageTV);

        mMessageTV.setText("Initial Permission Settings");



    }

    @Override
    public void onResume(){
        super.onResume();
        Log.d(TAG, "onResume in PermissionGate");

        fishOrCutBait();

    }

    @Override
    public void onPause(){
        super.onPause();
        Log.d(TAG, "Pausing");
        finish();
    }

    @Override
    public void onNewIntent(Intent i){
        Log.d(TAG, "New inbound intent, hoss");
        ABApplication.dbToast(this, "PG new intent path");
        fishOrCutBait();
    }

    public void fishOrCutBait(){

        // Turns out the WiFi ones don't require a dialog, but added it because WiFi not working on Zidoo
        if (getAndOfAllPermissions()) {
            goMain();
        } else {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.CAMERA,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.READ_PHONE_STATE},
                    2727);

        }

    }

    public void goMain() {

        ((ABApplication)getApplication()).boot();
        startActivity(new Intent(this, MainFrameActivity.class));
        //psfinish();
    }

    public void die() {
        finish();
    }

    public void getWithTheProgram() {
        mMessageTV.setText("Wrong Answer. I Die Now!");
        mMessageTV.postDelayed(new Runnable() {
            @Override
            public void run() {
                die();
            }
        }, 2000);

    }

    public boolean getAndOfAllPermissions() {

        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&

                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&

                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED &&

                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED &&

                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&

                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&

                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 2727: {
                // If requestBuilder is cancelled, the result arrays are empty.
                // TODO this logic blows. It should be only
                if (getAndOfAllPermissions()) {
                    goMain();
                } else {
                    getWithTheProgram();
                }


            }

        }
    }


}
