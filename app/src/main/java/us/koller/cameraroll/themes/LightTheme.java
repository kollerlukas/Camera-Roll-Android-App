package us.koller.cameraroll.themes;

import us.koller.cameraroll.R;

public class LightTheme extends Theme {

    @Override
    public int getBaseTheme() {
        return BASE_LIGHT;
    }

    @Override
    public boolean darkStatusBarIcons() {
        return true;
    }

    @Override
    public boolean elevatedToolbar() {
        return true;
    }

    @Override
    public boolean darkStatusBarIconsInSelectorMode() {
        return true;
    }

    @Override
    int getBackgroundColorRes() {
        return R.color.light_bg;
    }

    @Override
    int getToolbarColorRes() {
        return R.color.colorPrimary_light;
    }

    @Override
    int getTextColorPrimaryRes() {
        return R.color.grey_900_translucent;
    }

    @Override
    int getTextColorSecondaryRes() {
        return R.color.grey_900_translucent;
    }

    @Override
    int getAccentColorRes() {
        return R.color.colorAccent;
    }

    @Override
    int getAccentTextColorRes() {
        return R.color.colorAccent_text;
    }

    @Override
    public int getDialogThemeRes() {
        return R.style.Theme_CameraRoll_Light_Dialog;
    }
}
