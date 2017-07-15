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
    public boolean darkStatusBarIconsInSelectorMode() {
        return true;
    }

    @Override
    int getBackgroundColorRes() {
        return R.color.dark_bg;
    }

    @Override
    int getToolbarColorRes() {
        return R.color.black_translucent2;
    }

    @Override
    int getTextColorPrimaryRes() {
        return R.color.white;
    }

    @Override
    int getTextColorSecondaryRes() {
        return R.color.white_translucent1;
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
        return R.style.Theme_CameraRoll_Dialog;
    }
}
