package us.koller.cameraroll.ui;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Settings;

public abstract class ThemeableActivity extends AppCompatActivity {

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

        setViewBgColors();

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

        accent_color_res = dark ? R.color.colorAccent
                : R.color.colorAccent_light;
    }

    public void setViewBgColors() {
        ViewGroup rootView = (ViewGroup) findViewById(R.id.root_view);
        if (rootView == null) {
            return;
        }

        //find views
        String TAG = this.getString(R.string.theme_bg_color);
        ArrayList<View> views = findViewsWithTag(TAG, rootView);

        int bg_color = ContextCompat.getColor(this, bg_color_res);
        for (int i = 0; i < views.size(); i++) {
            Log.d("ThemeableActivity", "setViewBgColor");
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

    public abstract void onThemeApplied(int theme);

    public int getDialogThemeRes() {
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
                R.mipmap.ic_launcher_round);
        setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.app_name),
                overviewIcon, color));
        overviewIcon.recycle();
    }
}

