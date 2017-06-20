package us.koller.cameraroll.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;

import us.koller.cameraroll.R;
import us.koller.cameraroll.ui.ThemeableActivity;

public class Util {

    public static int[] getImageDimensions(Context context, Uri uri) {
        int[] dimensions = new int[2];

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        try {
            BitmapFactory.decodeStream(context
                            .getContentResolver().openInputStream(uri),
                    new Rect(0, 0, 0, 0), options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        dimensions[0] = options.outWidth > 0 ? options.outWidth : 1;
        dimensions[1] = options.outHeight > 0 ? options.outHeight : 1;
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

    /*@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void animateToolbarElevation(Toolbar toolbar, float scrollY) {
        float maxToolbarElevation = toolbar.getContext().getResources()
                .getDimension(R.dimen.toolbar_elevation);
        float toolbarElevation = scrollY;
        if (toolbarElevation > maxToolbarElevation) {
            toolbarElevation = maxToolbarElevation;
        } else if (toolbarElevation < 0) {
            toolbarElevation = 0;
        }
        toolbar.setElevation(toolbarElevation);
    }*/

    private static Drawable selectorOverlay;

    public static Drawable getAlbumItemSelectorOverlay(Context context) {
        if (selectorOverlay == null) {
            selectorOverlay = ContextCompat.getDrawable(context,
                    R.drawable.albumitem_selected_indicator);
        }
        ThemeableActivity.ColorManager colorManager = ThemeableActivity.getColorManager();
        int tintColor = colorManager.getColor(ThemeableActivity.ColorManager.ACCENT_COLOR);
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
        return Settings.Global.getFloat(context.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f);
    }

    //stolen from: https://gist.github.com/joecks/4559331#file-showflags-intent-intent
    public static void printIntentFlags(Intent intent) {
        Field[] declaredFields = Intent.class.getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.getName().startsWith("FLAG_")) {

                try {
                    int flag = field.getInt(null);

                    if ((intent.getFlags() & flag) != 0) {
                        Log.d("IntentReceiver", field.getName());
                    }

                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }

            }
        }
    }
}
