package io.ourglass.bucanero.core;

import com.mstar.android.tv.TvCommonManager;
import com.mstar.android.tvapi.common.TvManager;
import com.mstar.android.tvapi.common.exception.TvCommonException;
import com.mstar.android.tvapi.common.vo.TvOsType;

/**
 * Created by mkahn on 3/14/17.
 */

public class OGHardware {

    /*******************************************************************************
     *
     * TRONSMART SPECIFIC CODE
     *
     *******************************************************************************/

    /***************************************
     * TRONSMART CODE
     ***************************************/

    public static boolean enableTronsmartHDMI() {
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

    private static void changeTronsmartInputSource(TvOsType.EnumInputSource eis) {

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

}
