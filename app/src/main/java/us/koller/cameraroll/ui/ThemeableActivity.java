package us.koller.cameraroll.ui;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Settings;

public abstract class ThemeableActivity extends BaseActivity {

    public static final int UNDEFINED = -1;
    public static final int DARK = 1;
    public static final int LIGHT = 2;

    public static int THEME = UNDEFINED;

    public static int bg_color_res;

    public static int toolbar_color_res;
    public static int text_color_res;
    public static int text_color_secondary_res;

    public static int accent_color_res;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (THEME == UNDEFINED) {
            readTheme(this);
            setColors();
        }

        setTheme(getThemeRes(THEME));
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        ViewGroup rootView = (ViewGroup) findViewById(R.id.root_view);

        checkTags(rootView);

        onThemeApplied(THEME);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setupTaskDescription();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (THEME == UNDEFINED) {
            this.recreate();
        }
    }

    private static void readTheme(Context context) {
        Settings s = Settings.getInstance(context);
        THEME = s.getTheme()
                .equals(context.getString(R.string.DARK_THEME_VALUE)) ?
                DARK : LIGHT;
    }

    public void setColors() {
        boolean dark = THEME == DARK;

        bg_color_res = dark ? R.color.dark_bg : R.color.light_bg;

        toolbar_color_res = dark ? R.color.black_translucent2
                : R.color.colorPrimary_light;

        text_color_res = dark ? R.color.white : R.color.grey_900_translucent;

        text_color_secondary_res = dark ? R.color.white_translucent1
                : R.color.grey_900_translucent;

        accent_color_res = R.color.colorAccent;
    }

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

        int textColorPrimary = ContextCompat.getColor(vg.getContext(), text_color_res);
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

        int textColorSecondary = ContextCompat.getColor(vg.getContext(), text_color_secondary_res);
        for (int i = 0; i < viewsSecondary.size(); i++) {
            View v = viewsSecondary.get(i);
            if (v instanceof TextView) {
                ((TextView) v).setTextColor(textColorSecondary);
            } else if (v instanceof ImageView) {
                ((ImageView) v).setColorFilter(textColorSecondary);
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

        int bg_color = ContextCompat.getColor(vg.getContext(), bg_color_res);
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

    public abstract int getThemeRes(int style);

    public void onThemeApplied(int theme) {

    }

    public static int getDialogThemeRes() {
        if (THEME == DARK) {
            return R.style.Theme_CameraRoll_Dialog;
        } else {
            return R.style.Theme_CameraRoll_Light_Dialog;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setupTaskDescription() {
        int colorRes = THEME == DARK ? R.color.colorPrimary : R.color.colorPrimary_light;
        int color = ContextCompat.getColor(this, colorRes);

        Bitmap overviewIcon = BitmapFactory.decodeResource(getResources(),
                R.mipmap.ic_launcher);
        setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.app_name),
                overviewIcon, color));
        overviewIcon.recycle();
    }

    public static int getStatusBarColor(Context context, int toolbarColor) {
        float darken = 0.9f;
        if (toolbarColor == -1) {
            toolbarColor = ContextCompat
                    .getColor(context, toolbar_color_res);
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
}

