package us.koller.cameraroll.util;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

public class ColorFade {

    public static void fadeBackgroundColor(final View v, final int startColor, final int endColor) {
        ValueAnimator animator = getDefaultValueAnimator();
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                v.setBackgroundColor(getAnimatedColor(startColor, endColor, valueAnimator.getAnimatedFraction()));
            }
        });
        animator.start();
    }

    //performance issues
    public static void fadeTintColor(final Drawable d, final int startColor, final int endColor) {
        ValueAnimator animator = getDefaultValueAnimator();
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                d.setTint(getAnimatedColor(startColor, endColor, valueAnimator.getAnimatedFraction()));
            }
        });
        animator.start();
    }

    private static ValueAnimator getDefaultValueAnimator() {
        ValueAnimator animator = ValueAnimator.ofInt(0, 100);
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        return animator;
    }

    private static int getAnimatedColor(int startColor, int endColor, float animatedValue) {
        int alpha = getAnimatedValue(Color.alpha(startColor), Color.alpha(endColor), animatedValue);
        int red = getAnimatedValue(Color.red(startColor), Color.red(endColor), animatedValue);
        int green = getAnimatedValue(Color.green(startColor), Color.green(endColor), animatedValue);
        int blue = getAnimatedValue(Color.blue(startColor), Color.blue(endColor), animatedValue);
        return Color.argb(alpha, red, green, blue);
    }

    private static int getAnimatedValue(int start, int end, float animatedValue) {
        return (int) (start + (end - start) * animatedValue);
    }
}
