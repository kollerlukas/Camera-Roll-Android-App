package us.koller.cameraroll.util;

import android.app.Activity;
import android.util.DisplayMetrics;

public class Util {
    public static int getScreenWidth(Activity context) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        return displaymetrics.widthPixels;
    }
}
