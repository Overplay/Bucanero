package io.ourglass.bucanero.tv.Support;

import android.animation.ObjectAnimator;
import android.view.View;

/**
 * Created by mkahn on 2/13/17.
 */

public class OGAnimations {

    public static void animateAlphaIn(View v, float finalAlpha) {

        ObjectAnimator anim = ObjectAnimator.ofFloat(v, "alpha", 0f, finalAlpha);
        anim.setDuration(500);
        anim.start();

    }

    public static void animateAlphaOut(View v) {

        ObjectAnimator anim = ObjectAnimator.ofFloat(v, "alpha", v.getAlpha(), 0);
        anim.setDuration(500);
        anim.start();

    }
}
