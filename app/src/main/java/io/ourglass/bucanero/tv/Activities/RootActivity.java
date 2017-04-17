package io.ourglass.bucanero.tv.Activities;


import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.mstar.android.tv.TvCommonManager;
import com.mstar.android.tvapi.common.TvManager;
import com.mstar.android.tvapi.common.exception.TvCommonException;
import com.mstar.android.tvapi.common.vo.TvOsType;

import io.ourglass.bucanero.R;


/**
 *
 * Why is this here? This shim Activity is here simply to make sure the TV Surface is always
 * running even if Mainframe crashes. All it does is set up the TV surface ans start AB Service.
 *
 */
public class RootActivity extends Activity {

    private static final String TAG = "RootActivity";

    private SurfaceView surfaceView;

    private RelativeLayout mMainLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.root_layout);

        enableTronsmartHDMI();

        Log.d(TAG, "onCreate done");


    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "onResume done");
        //Intent mainIntent = new Intent(this, MainframeActivity.class);
        //startActivity(mainIntent);
        Toast.makeText(this, "Hanging Root", Toast.LENGTH_LONG).show();

    }

    /*******************************************************************************
     *
     * TRONSMART SPECIFIC CODE
     *
     *******************************************************************************/

    /***************************************
     * TRONSMART CODE
     ***************************************/

    private static boolean enableTronsmartHDMI() {
        boolean bRet = false;
        try {
            changeTronsmartInputSource(TvOsType.EnumInputSource.E_INPUT_SOURCE_STORAGE);
            changeTronsmartInputSource(TvOsType.EnumInputSource.E_INPUT_SOURCE_HDMI);
            bRet = TvManager.getInstance().getPlayerManager().isSignalStable();
        } catch (TvCommonException e) {
            e.printStackTrace();
        }
        return bRet;
    }

    public static void changeTronsmartInputSource(TvOsType.EnumInputSource eis) {

        TvCommonManager commonService = TvCommonManager.getInstance();

        if (commonService != null) {
            TvOsType.EnumInputSource currentSource = commonService.getCurrentInputSource();
            if (currentSource != null) {
                if (currentSource.equals(eis)) {
                    return;
                }

                commonService.setInputSource(eis);
            }
        }
    }


    /*****************************************
     * TOASTS and ALERTS
     *****************************************/

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    }



}