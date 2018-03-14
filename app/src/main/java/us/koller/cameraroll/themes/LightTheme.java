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
        return false;
    }

    @Override
    public boolean darkStatusBarIconsInSelectorMode() {
        return true;
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
        return R.color.grey_800;
    }

    @Override
    public int getTextColorSecondaryRes() {
        return R.color.grey_900_translucent1;
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
