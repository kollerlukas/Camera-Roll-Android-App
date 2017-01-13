package us.koller.cameraroll.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

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

    public static int[] getImageDimensions(String path) {
        int[] dimensions = new int[2];

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(path, options);
        dimensions[0] = options.outWidth > 0 ? options.outWidth : 1;
        dimensions[1] = options.outHeight > 0 ? options.outHeight : 1;
        return dimensions;
    }
}
