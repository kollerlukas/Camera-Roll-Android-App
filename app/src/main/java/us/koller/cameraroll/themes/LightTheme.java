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
    public boolean statusBarOverlay() {
        return true;
    }

    @Override
    public boolean darkStatusBarIconsInSelectorMode() {
        return false;
    }

    @Override
    public int getBackgroundColorRes() {
        return R.color.light_bg;
    }

    @Override
    public int getToolbarColorRes() {
        return R.color.colorPrimary_light;
    }

    @Override
    public int getTextColorPrimaryRes() {
        return R.color.grey_900;
    }

    @Override
    public int getTextColorSecondaryRes() {
        return R.color.grey_900_translucent;
    }

    @Override
    public int getAccentColorRes() {
        return R.color.colorAccent_light;
    }

    @Override
    public int getAccentColorLightRes() {
        return R.color.colorAccentLight_light;
    }

    @Override
    public int getAccentTextColorRes() {
        return R.color.colorAccent_text_light;
    }

    @Override
    public int getDialogThemeRes() {
        return R.style.CameraRoll_Theme_Light_Dialog;
    }
}
