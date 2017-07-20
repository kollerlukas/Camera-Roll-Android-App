package us.koller.cameraroll.muzei;

import android.os.Bundle;
import android.support.annotation.Nullable;

import us.koller.cameraroll.R;
import us.koller.cameraroll.ui.ThemeableActivity;

public class SettingsActivity extends ThemeableActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //TODO
    }

    @Override
    public int getDarkThemeRes() {
        return R.style.Theme_CameraRoll_MuzeiSettings;
    }

    @Override
    public int getLightThemeRes() {
        return R.style.Theme_CameraRoll_Light_MuzeiSettings;
    }
}
