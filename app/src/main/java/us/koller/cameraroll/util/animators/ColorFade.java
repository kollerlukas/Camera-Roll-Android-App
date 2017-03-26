package us.koller.cameraroll.util.animators;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

public class ColorFade {

    public static void fadeBackgroundColor(final View v, final int startColor, final int endColor) {
        ValueAnimator animator = getDefaultValueAnimator();
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                v.setBackgroundColor(getAnimatedColor(startColor, endColor,
                        valueAnimator.getAnimatedFraction()));
            }
        });
        animator.start();
    }

    private static ValueAnimator getDefaultValueAnimator() {
        ValueAnimator animator = ValueAnimator.ofInt(0, 100);
        animator.setDuration(500);
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

    // imageView saturation fade
    public static void fadeSaturation(final ImageView imageView) {
        // see: https://github.com/nickbutcher/plaid/blob/master/app/src/main/java/io/plaidapp/ui/FeedAdapter.java
        imageView.setHasTransientState(true);
        final AnimUtils.ObservableColorMatrix matrix = new AnimUtils.ObservableColorMatrix();
        final ObjectAnimator saturation = ObjectAnimator.ofFloat(
                matrix, AnimUtils.ObservableColorMatrix.SATURATION, 0f, 1f);
        saturation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener
                () {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                // just animating the color matrix does not invalidate the
                // drawable so need this update listener.  Also have to create a
                // new CMCF as the matrix is immutable :(
                imageView.setColorFilter(new ColorMatrixColorFilter(matrix));
            }
        });
        saturation.setDuration(2000L);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            saturation.setInterpolator(AnimUtils.getFastOutSlowInInterpolator(imageView.getContext()));
        }
        saturation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                imageView.clearColorFilter();
                imageView.setHasTransientState(false);
            }
        });
        saturation.start();
    }

    public interface ToolbarTitleFadeCallback {
        void setTitle(Toolbar toolbar);
    }

    private static AnimatorSet toolbarTitleAnimSet;

    //fade Toolbar title text change
    public static void fadeToolbarTitleColor(final Toolbar toolbar, final int color,
                                             final ToolbarTitleFadeCallback callback, boolean fadeTitleOut) {

        if (toolbarTitleAnimSet != null) {
            toolbarTitleAnimSet.cancel();
        }

        final int transparent = ContextCompat.getColor(toolbar.getContext(), android.R.color.transparent);

        ValueAnimator fadeOut = null;
        if (fadeTitleOut) {
            fadeOut = getDefaultValueAnimator();
            fadeOut.setDuration(250);
            fadeOut.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    toolbar.setTitleTextColor(getAnimatedColor(color, transparent,
                            valueAnimator.getAnimatedFraction()));
                }
            });
            fadeOut.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    callback.setTitle(toolbar);
                }
            });
        } else {
            toolbar.setTitleTextColor(transparent);
            callback.setTitle(toolbar);
        }

        ValueAnimator fadeIn = getDefaultValueAnimator();
        fadeIn.setDuration(fadeTitleOut ? 250 : 500);
        fadeIn.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                toolbar.setTitleTextColor(getAnimatedColor(transparent, color,
                        valueAnimator.getAnimatedFraction()));
            }
        });

        toolbarTitleAnimSet = new AnimatorSet();
        if (fadeOut != null) {
            toolbarTitleAnimSet.playSequentially(fadeOut, fadeIn);
        } else {
            toolbarTitleAnimSet.playSequentially(fadeIn);
        }
        toolbarTitleAnimSet.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        toolbarTitleAnimSet = null;
                    }
                });
        toolbarTitleAnimSet.start();
    }

    public static void fadeIconColor(final Drawable d, final int startColor,
                                     final int endColor) {
        DrawableCompat.wrap(d);

        ValueAnimator animator = getDefaultValueAnimator();
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int animatedColor = getAnimatedColor(startColor, endColor,
                        valueAnimator.getAnimatedFraction());
                DrawableCompat.setTint(d, animatedColor);

            }
        });
        AnimatorSet set = new AnimatorSet();
        set.play(animator);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                DrawableCompat.unwrap(d);
            }
        });
        set.start();
    }
}
