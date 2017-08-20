package us.koller.cameraroll.themes;

import us.koller.cameraroll.R;

public class DarkTheme extends Theme {

    @Override
    public int getBaseTheme() {
        return BASE_DARK;
    }

    @Override
    public boolean darkStatusBarIcons() {
        return false;
    }

    @Override
    public boolean elevatedToolbar() {
        return false;
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
        return R.color.dark_bg;
    }

    @Override
    public int getToolbarColorRes() {
        return R.color.black_translucent2;
    }

    @Override
    public int getTextColorPrimaryRes() {
        return R.color.white;
    }

    @Override
    public int getTextColorSecondaryRes() {
        return R.color.white_translucent1;
    }

    @Override
    public int getAccentColorRes() {
        return R.color.colorAccent;
    }

    @Override
    public int getAccentColorLightRes() {
        return R.color.colorAccentLight;
    }

    @Override
    public int getAccentTextColorRes() {
        return R.color.colorAccent_text;
    }

    @Override
    public int getDialogThemeRes() {
        return R.style.CameraRoll_Theme_Dialog;
    }
}
