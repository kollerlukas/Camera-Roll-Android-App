package us.koller.cameraroll.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.media.ExifInterface;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import us.koller.cameraroll.R;
import us.koller.cameraroll.themes.Theme;
import us.koller.cameraroll.data.Settings;

public class Util {

    private static Drawable selectorOverlay;

    public static int[] getImageDimensions(Context context, Uri uri) {
        int[] dimensions = new int[]{0, 0};

        try {
            InputStream is = context.getContentResolver().openInputStream(uri);

            //try exif
            String mimeType = MediaType.getMimeType(context, uri);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    && MediaType.doesSupportExifMimeType(mimeType)
                    && is != null) {
                ExifInterface exif = new ExifInterface(is);
                if (exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH) != null
                        && exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH) != null) {
                    int width = (int) ExifUtil.getCastValue(exif, ExifInterface.TAG_IMAGE_WIDTH);
                    int height = (int) ExifUtil.getCastValue(exif, ExifInterface.TAG_IMAGE_LENGTH);
                    if (width != 0 && height != 0) {
                        return new int[]{width, height};
                    }
                }
            }

            //exif didn't work
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, new Rect(0, 0, 0, 0), options);
            dimensions[0] = options.outWidth;
            dimensions[1] = options.outHeight;

            if (is != null) {
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return dimensions;
    }

    public static int[] getVideoDimensions(String path) throws FileNotFoundException {
        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException();
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            retriever.setDataSource(path);
            Bitmap bitmap = retriever.getFrameAtTime();

            int[] dimensions = new int[2];

            if (bitmap != null) {
                dimensions[0] = bitmap.getWidth() > 0 ? bitmap.getWidth() : 1;
                dimensions[1] = bitmap.getHeight() > 0 ? bitmap.getHeight() : 1;
            }
            return dimensions;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new int[]{1, 1};
    }

    @SuppressWarnings("inlineValue")
    public static TextView setToolbarTypeface(Toolbar toolbar) {
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View view = toolbar.getChildAt(i);
            if (view instanceof TextView) {
                TextView textView = (TextView) view;
                if (textView.getText().equals(toolbar.getTitle())) {
                    Typeface typeface = ResourcesCompat.getFont(toolbar.getContext(),
                            R.font.roboto_mono_regular);
                    textView.setTypeface(typeface);
                    return textView;
                }
            }
        }
        return null;
    }

    public static void setDarkStatusBarIcons(final View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            v.post(new Runnable() {
                @Override
                @RequiresApi(api = Build.VERSION_CODES.M)
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

    @SuppressWarnings("unused")
    public static boolean areStatusBarIconsDark(final View v) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && v.getSystemUiVisibility() != 0;
    }

    public static String getParentPath(String path) {
        return new File(path).getParent();
    }

    public static void showSnackbar(Snackbar snackbar) {
        snackbar.show();
        TextView textView = snackbar.getView()
                .findViewById(android.support.design.R.id.snackbar_text);
        textView.setTypeface(Typeface.create("sans-serif-monospace", Typeface.NORMAL));
    }

    public static Snackbar getPermissionDeniedSnackbar(final View rootView) {
        Snackbar snackbar = Snackbar.make(rootView,
                R.string.read_permission_denied,
                Snackbar.LENGTH_INDEFINITE);
        snackbar.getView().setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Toast.makeText(rootView.getContext(), R.string.read_permission_denied, Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        return snackbar;
    }

    public static void colorToolbarOverflowMenuIcon(Toolbar toolbar, int color) {
        //set Toolbar overflow icon color
        Drawable drawable = toolbar.getOverflowIcon();
        if (drawable != null) {
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable.mutate(), color);
            toolbar.setOverflowIcon(drawable);
        }
    }

    public static Drawable getAlbumItemSelectorOverlay(Context context) {
        if (selectorOverlay == null) {
            selectorOverlay = AppCompatResources.getDrawable(context,
                    R.drawable.album_item_selected_indicator);
        }

        Settings s = Settings.getInstance(context);
        Theme theme = s.getThemeInstance(context);

        int tintColor = theme.getAccentColor(context);
        selectorOverlay = DrawableCompat.wrap(selectorOverlay);
        DrawableCompat.setTint(selectorOverlay, tintColor);
        return selectorOverlay;
    }

    //int[left, top, right, bottom]
    public static int[] getScreenSize(Activity context) {
        Rect displayRect = new Rect();
        context.getWindow().getDecorView().getWindowVisibleDisplayFrame(displayRect);
        return new int[]{
                displayRect.left, displayRect.top,
                displayRect.right, displayRect.bottom};
    }

    public static float getAnimatorSpeed(Context context) {
        PowerManager powerManager = (PowerManager)
                context.getSystemService(Context.POWER_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && powerManager.isPowerSaveMode()) {
            // Animations are disabled in power save mode, so just show a toast instead.
            return 0.0f;
        }
        return android.provider.Settings.Global.getFloat(context.getContentResolver(),
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f);
    }

    public static boolean hasWifiConnection(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null &&
                    activeNetwork.isConnectedOrConnecting();
            return isConnected && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
        } catch (SecurityException e) {
            e.printStackTrace();
            return false;
        }
    }

    static Locale getLocale(Context context) {
        Locale locale;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            locale = context.getResources().getConfiguration().getLocales().get(0);
        } else {
            locale = context.getResources().getConfiguration().locale;
        }
        return locale;
    }

    public static int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}
