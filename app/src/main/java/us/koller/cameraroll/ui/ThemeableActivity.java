package us.koller.cameraroll.ui;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Settings;

public abstract class ThemeableActivity extends BaseActivity {

    public static final int UNDEFINED = -1;

    public static int THEME = UNDEFINED;
    /*true => dark, false => light*/
    private static boolean[] baseThemes;

    private static ColorManager colorManager;

    /*themeable colors*/
    public int backgroundColor,
            toolbarColor,
            textColor,
            textColorSec,
            accentColor,
            accentTextColor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (THEME == UNDEFINED) {
            readTheme(this);
            colorManager = new ColorManager(this, THEME);
        }

        setTheme(getThemeRes(THEME));

        //set color variables
        backgroundColor = getThemeColor(ColorManager.BG_COLOR);
        toolbarColor = getThemeColor(ColorManager.TOOLBAR_COLOR);
        textColor = getThemeColor(ColorManager.TEXT_COLOR);
        textColorSec = getThemeColor(ColorManager.TEXT_COLOR_SEC);
        accentColor = getThemeColor(ColorManager.ACCENT_COLOR);
        accentTextColor = getThemeColor(ColorManager.ACCENT_TEXT_COLOR);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        ViewGroup rootView = (ViewGroup) findViewById(R.id.root_view);

        checkTags(rootView);

        onThemeApplied(isLightBaseTheme(THEME));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setupTaskDescription();
        }
    }

    //systemUiFlags need to be reset to achieve transparent status- and NavigationBar
    void setSystemUiFlags() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    @Override
    protected void onRestart() {
        if (THEME == UNDEFINED) {
            this.recreate();
        }

        super.onRestart();
    }

    private static void readTheme(Context context) {
        Settings s = Settings.getInstance(context);
        THEME = s.getTheme(context);

        readBaseThemes(context);
    }

    private static void readBaseThemes(Context context) {
        Resources res = context.getResources();
        TypedArray baseThemes = res.obtainTypedArray(R.array.base_themes);
        ThemeableActivity.baseThemes = new boolean[baseThemes.length()];
        for (int i = 0; i < baseThemes.length(); i++) {
            ThemeableActivity.baseThemes[i] = baseThemes.getBoolean(i, true);
        }
        baseThemes.recycle();
        Log.d("ThemeableActivity", "readBaseThemes: " + Arrays.toString(ThemeableActivity.baseThemes));
    }

    public static ColorManager getColorManager() {
        return colorManager;
    }

    public int getThemeColor(int COLOR) {
        ColorManager colorManager = getColorManager();
        if (colorManager != null) {
            return colorManager.getColor(COLOR);
        }
        return -1;
    }

    //use dark statusBar icons over colorAccent
    public boolean colorAccentDarkIcons() {
        return getResources().getBoolean(R.bool.colorAccent_dark_icons);
    }


    //static Method to call, when adding a view dynamically in order to get Theme applied
    public static void checkTags(ViewGroup viewGroup) {
        setViewBgColors(viewGroup);

        setViewTextColors(viewGroup);
    }

    public static void setViewTextColors(ViewGroup vg) {
        if (vg == null) {
            return;
        }

        //find views
        String TAG_TEXT_PRIMARY = vg.getContext().getString(R.string.theme_text_color_primary);
        ArrayList<View> viewsPrimary = findViewsWithTag(TAG_TEXT_PRIMARY, vg);

        ColorManager colorManager = getColorManager();

        int textColorPrim = colorManager.getColor(ColorManager.TEXT_COLOR);
        for (int i = 0; i < viewsPrimary.size(); i++) {
            View v = viewsPrimary.get(i);
            if (v instanceof TextView) {
                ((TextView) v).setTextColor(textColorPrim);
            } else if (v instanceof ImageView) {
                ((ImageView) v).setColorFilter(textColorPrim);
            }
        }

        String TAG_TEXT_SECONDARY = vg.getContext().getString(R.string.theme_text_color_secondary);
        ArrayList<View> viewsSecondary = findViewsWithTag(TAG_TEXT_SECONDARY, vg);

        int textColorSec = colorManager.getColor(ColorManager.TEXT_COLOR_SEC);
        for (int i = 0; i < viewsSecondary.size(); i++) {
            View v = viewsSecondary.get(i);
            if (v instanceof TextView) {
                ((TextView) v).setTextColor(textColorSec);
            } else if (v instanceof ImageView) {
                ((ImageView) v).setColorFilter(textColorSec);
            }
        }
    }

    public static void setViewBgColors(ViewGroup vg) {
        if (vg == null) {
            return;
        }

        //find views
        String TAG = vg.getContext().getString(R.string.theme_bg_color);
        ArrayList<View> views = findViewsWithTag(TAG, vg);

        ColorManager colorManager = getColorManager();

        int bg_color = colorManager.getColor(ColorManager.BG_COLOR);
        for (int i = 0; i < views.size(); i++) {
            views.get(i).setBackgroundColor(bg_color);
        }
    }

    private static ArrayList<View> findViewsWithTag(String TAG, ViewGroup rootView) {
        return findViewsWithTag(TAG, rootView, new ArrayList<View>());
    }

    private static ArrayList<View> findViewsWithTag(String TAG, ViewGroup rootView,
                                                    ArrayList<View> views) {
        Object tag = rootView.getTag();
        if (tag != null && tag.equals(TAG)) {
            views.add(rootView);
        }

        for (int i = 0; i < rootView.getChildCount(); i++) {
            View v = rootView.getChildAt(i);
            tag = v.getTag();
            if (tag != null && tag.equals(TAG)) {
                views.add(v);
            }

            if (v instanceof ViewGroup) {
                findViewsWithTag(TAG, (ViewGroup) v, views);
            }
        }

        return views;
    }

    public boolean isLightBaseTheme(int style) {
        return !baseThemes[style];
    }

    public int getThemeRes(int style) {
        return baseThemes[style] ? getDarkThemeRes() : getLightThemeRes();
    }

    public abstract int getDarkThemeRes();

    public abstract int getLightThemeRes();

    public void onThemeApplied(boolean lightBaseTheme) {

    }

    public static int getDialogThemeRes() {
        if (baseThemes[THEME]) {
            return R.style.Theme_CameraRoll_Dialog;
        } else {
            return R.style.Theme_CameraRoll_Light_Dialog;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setupTaskDescription() {
        int colorRes = isLightBaseTheme(THEME) ? R.color.colorPrimary_light : R.color.colorPrimary;
        int color = ContextCompat.getColor(this, colorRes);

        Bitmap overviewIcon = BitmapFactory.decodeResource(getResources(),
                R.mipmap.ic_launcher);
        setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.app_name),
                overviewIcon, color));
        overviewIcon.recycle();
    }

    public static int getStatusBarColor(int toolbarColor) {
        float darken = 0.9f;
        if (toolbarColor == -1) {
            toolbarColor = getColorManager().getColor(ColorManager.TOOLBAR_COLOR);
        }
        return Color.argb(
                (int) (Color.alpha(toolbarColor) * darken),
                (int) (Color.red(toolbarColor) * darken),
                (int) (Color.green(toolbarColor) * darken),
                (int) (Color.blue(toolbarColor) * darken));
    }

    /*public void addStatusBarOverlay(final Toolbar toolbar,
                                    int toolbarColor,
                                    final int statusBarHeight) {
        int statusBarColor = getStatusBarColor(this, toolbarColor);

        //int statusBarColor = Color.argb( 54, 0, 0, 0);

        final Drawable statusBarOverlay
                = new ColorDrawable(statusBarColor);

        toolbar.post(new Runnable() {
            @Override
            public void run() {
                toolbar.getOverlay().clear();
                statusBarOverlay.setBounds(0, 0,
                        toolbar.getWidth(), statusBarHeight);
                toolbar.getOverlay().add(statusBarOverlay);
            }
        });
    }*/

    public static class ColorManager {

        public static final int BG_COLOR = 0;
        public static final int TOOLBAR_COLOR = 1;
        public static final int TEXT_COLOR = 2;
        public static final int TEXT_COLOR_SEC = 3;
        public static final int ACCENT_COLOR = 4;
        public static final int ACCENT_TEXT_COLOR = 5;

        private int[] colors;

        ColorManager(Context context, int theme) {
            Resources res = context.getResources();
            TypedArray themeColors = res.obtainTypedArray(R.array.themeColors);
            int themeColorRes = themeColors.getResourceId(theme, R.array.dark_theme_colors);
            themeColors.recycle();

            colors = res.getIntArray(themeColorRes);
        }

        public int getColor(int COLOR) {
            return colors[COLOR];
        }
    }
}

