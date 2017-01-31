package us.koller.cameraroll.util;

import android.graphics.drawable.ColorDrawable;

public class SizedColorDrawable extends ColorDrawable {

    private int width = 0, height = 0;

    public SizedColorDrawable(int color, int width, int height) {
        super(color);
        this.width = width;
        this.height = height;
    }

    public SizedColorDrawable(int color, int[] dimens) {
        super(color);
        this.width = dimens[0];
        this.height = dimens[1];
    }

    @Override
    public int getIntrinsicWidth() {
        return width;
    }

    @Override
    public int getIntrinsicHeight() {
        return height;
    }
}
