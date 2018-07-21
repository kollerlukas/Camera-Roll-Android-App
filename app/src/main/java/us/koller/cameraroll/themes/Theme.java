package us.koller.cameraroll.themes;

import android.content.Context;
import android.support.v4.content.ContextCompat;

public abstract class Theme {
    static final int BASE_DARK = 0;
    static final int BASE_LIGHT = 1;
    public abstract int getBaseTheme();

    private static int getColor(Context c, int res) {
        return ContextCompat.getColor(c, res);
    }

    /*flags*/
    public abstract boolean darkStatusBarIcons();
    public abstract boolean elevatedToolbar();
    public abstract boolean statusBarOverlay();
    public abstract boolean darkStatusBarIconsInSelectorMode();

    /*colors*/
    public abstract int getBackgroundColorRes();
    public abstract int getToolbarColorRes();
    public abstract int getTextColorPrimaryRes();
    public abstract int getTextColorSecondaryRes();
    public abstract int getAccentColorRes();
    public abstract int getAccentColorLightRes();
    public abstract int getAccentTextColorRes();

    public boolean isBaseLight() {
        return getBaseTheme() == BASE_LIGHT;
    }

    public int getBackgroundColor(Context c) {
        return getColor(c, getBackgroundColorRes());
    }

    public int getToolbarColor(Context c) {
        return getColor(c, getToolbarColorRes());
    }

    public int getTextColorPrimary(Context c) {
        return getColor(c, getTextColorPrimaryRes());
    }

    public int getTextColorSecondary(Context c) {
        return getColor(c, getTextColorSecondaryRes());
    }

    public int getAccentColor(Context c) {
        return getColor(c, getAccentColorRes());
    }

    public int getAccentColorLight(Context c) {
        return getColor(c, getAccentColorLightRes());
    }

    public int getAccentTextColor(Context c) {
        return getColor(c, getAccentTextColorRes());
    }

    /*Dialog theme*/
    public abstract int getDialogThemeRes();

    @Override
    public boolean equals(Object o) {
        Class c = this.getClass();
        return c.isInstance(o);
    }
}
