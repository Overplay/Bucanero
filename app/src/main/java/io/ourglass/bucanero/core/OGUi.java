package io.ourglass.bucanero.core;

import android.content.res.AssetManager;
import android.graphics.Typeface;

import java.util.Locale;

/**
 * Created by mkahn on 4/10/17.
 */

public class OGUi {

    private static final String BOLD_FONT_NAME = "Poppins-Bold.ttf";
    private static final String REG_FONT_NAME = "Poppins-Regular.ttf";

    public static Typeface getBoldFont(){
        AssetManager am = ABApplication.sharedContext.getAssets();
        return Typeface.createFromAsset(am, String.format(Locale.US, "fonts/%s", BOLD_FONT_NAME));
    }

    public static Typeface getRegularFont(){
        AssetManager am = ABApplication.sharedContext.getAssets();
        return Typeface.createFromAsset(am, String.format(Locale.US, "fonts/%s", REG_FONT_NAME));
    }

}
