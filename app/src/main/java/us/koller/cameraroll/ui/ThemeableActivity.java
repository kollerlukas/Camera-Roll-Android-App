package us.koller.cameraroll.ui;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.themes.Theme;
import us.koller.cameraroll.data.Settings;

public abstract class ThemeableActivity extends BaseActivity {

    Theme theme = null;

    public int backgroundColor;
    public int toolbarColor;
    public int textColorPrimary;
    public int textColorSecondary;
    public int accentColor;
    public int accentTextColor;

    private ColorDrawable statusBarOverlay;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (theme == null) {
            readTheme(this);
        }

        setTheme(getThemeRes(theme));
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        ViewGroup rootView = findViewById(R.id.root_view);

        checkTags(rootView, theme);

        onThemeApplied(theme);

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

    private void readTheme(Context context) {
        Settings s = Settings.getInstance(context);
        theme = s.getThemeInstance(this);

        backgroundColor = theme.getBackgroundColor(this);
        toolbarColor = theme.getToolbarColor(this);
        textColorPrimary = theme.getTextColorPrimary(this);
        textColorSecondary = theme.getTextColorSecondary(this);
        accentColor = theme.getAccentColor(this);
        accentTextColor = theme.getAccentTextColor(this);
    }

    //static Method to call, when adding a view dynamically in order to get Theme applied
    public static void checkTags(ViewGroup viewGroup, Theme theme) {
        setViewBgColors(viewGroup, theme);

        setViewTextColors(viewGroup, theme);
    }

    private static void setViewTextColors(ViewGroup vg, Theme theme) {
        if (vg == null) {
            return;
        }

        //find views
        String TAG_TEXT_PRIMARY = vg.getContext().getString(R.string.theme_text_color_primary);
        ArrayList<View> viewsPrimary = findViewsWithTag(TAG_TEXT_PRIMARY, vg);

        int textColorPrimary = theme.getTextColorPrimary(vg.getContext());
        for (int i = 0; i < viewsPrimary.size(); i++) {
            View v = viewsPrimary.get(i);
            if (v instanceof TextView) {
                ((TextView) v).setTextColor(textColorPrimary);
            } else if (v instanceof ImageView) {
                ((ImageView) v).setColorFilter(textColorPrimary);
            }
        }

        String TAG_TEXT_SECONDARY = vg.getContext().getString(R.string.theme_text_color_secondary);
        ArrayList<View> viewsSecondary = findViewsWithTag(TAG_TEXT_SECONDARY, vg);

        int textColorSecondary = theme.getTextColorSecondary(vg.getContext());
        for (int i = 0; i < viewsSecondary.size(); i++) {
            View v = viewsSecondary.get(i);
            if (v instanceof TextView) {
                ((TextView) v).setTextColor(textColorSecondary);
            } else if (v instanceof ImageView) {
                ((ImageView) v).setColorFilter(textColorSecondary);
            }
        }
    }

    private static void setViewBgColors(ViewGroup vg, Theme theme) {
        if (vg == null) {
            return;
        }

        //find views
        String TAG = vg.getContext().getString(R.string.theme_bg_color);
        ArrayList<View> views = findViewsWithTag(TAG, vg);

        int backgroundColor = theme.getBackgroundColor(vg.getContext());
        for (int i = 0; i < views.size(); i++) {
            views.get(i).setBackgroundColor(backgroundColor);
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

    public int getThemeRes(Theme theme) {
        return theme.isBaseLight() ? getLightThemeRes() : getDarkThemeRes();
    }

    public abstract int getDarkThemeRes();

    public abstract int getLightThemeRes();

    public void onThemeApplied(Theme theme) {

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setupTaskDescription() {
        int colorRes = theme.isBaseLight() ? R.color.colorPrimary_light : R.color.colorPrimary;
        int color = ContextCompat.getColor(this, colorRes);

        //getting the app icon as a bitmap
        Drawable icon = getApplicationInfo().loadIcon(getPackageManager());
        Bitmap overviewIcon = Bitmap.createBitmap(icon.getIntrinsicWidth(),
                icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(overviewIcon);
        icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        icon.draw(canvas);

        setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.app_name),
                overviewIcon, color));
        overviewIcon.recycle();
    }

    public int getStatusBarColor() {
        float darken = 0.9f;
        return Color.argb(
                (int) (Color.alpha(toolbarColor) * darken),
                (int) (Color.red(toolbarColor) * darken),
                (int) (Color.green(toolbarColor) * darken),
                (int) (Color.blue(toolbarColor) * darken));
    }

    public void addStatusBarOverlay(final Toolbar toolbar) {
        int statusBarColor = getStatusBarColor();
        statusBarOverlay = new ColorDrawable(statusBarColor);
        toolbar.post(new Runnable() {
            @Override
            public void run() {
                statusBarOverlay.setBounds(new Rect(0, 0,
                        toolbar.getWidth(), toolbar.getPaddingTop()));
                toolbar.getOverlay().add(statusBarOverlay);
            }
        });
    }

    public ColorDrawable getStatusBarOverlay() {
        return statusBarOverlay;
    }
}