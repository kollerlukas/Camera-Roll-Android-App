package us.koller.cameraroll.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;

import us.koller.cameraroll.R;

public class Util {
    public static int getAlbumActivityGridColumnCount(Context context) {
        boolean landscape = context.getResources().getBoolean(R.bool.landscape);
        return !landscape ? 3 : 4;
    }

    public static int getScreenWidth(Activity context) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        return displaymetrics.widthPixels;
    }

    static int[] getImageDimensions(String path) {
        int[] dimensions = new int[2];

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(path, options);
        dimensions[0] = options.outWidth > 0 ? options.outWidth : 1;
        dimensions[1] = options.outHeight > 0 ? options.outHeight : 1;
        return dimensions;
    }

    public static void setDarkStatusBarIcons(final View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            v.post(new Runnable() {
                @Override
                public void run() {
                    v.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
            });
        }
    }

    public static void setLightStatusBarIcons(final View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            v.post(new Runnable() {
                @Override
                public void run() {
                    v.setSystemUiVisibility(0);
                }
            });
        }
    }
}
