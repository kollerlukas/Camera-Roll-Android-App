package us.koller.cameraroll.ui.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.devbrackets.android.exomedia.ui.widget.VideoControlsMobile;

import us.koller.cameraroll.R;

public class CustomVideoControls extends VideoControlsMobile {

    public CustomVideoControls(Context context) {
        super(context);
    }

    public CustomVideoControls(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomVideoControls(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomVideoControls(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.video_controls;
    }

    @Override
    protected void retrieveViews() {
        // replacing root View with regular RelativeLayout
        // because windowInsets are handle differently
        ViewGroup newRoot = (ViewGroup) findViewById(R.id.root);
        ViewGroup videoControls = (ViewGroup) LayoutInflater.from(getContext())
                .inflate(super.getLayoutResource(), newRoot, false);

        for (int i = videoControls.getChildCount() - 1; i >= 0; i--) {
            View v = videoControls.getChildAt(i);
            videoControls.removeView(v);
            newRoot.addView(v);
        }

        //calling super
        super.retrieveViews();

        //customize VideoControls
        controlsContainer.setBackground(
                ContextCompat.getDrawable(getContext(),
                        R.drawable.transparent_to_dark_gradient));
        currentTime.setTypeface(Typeface.create("sans-serif-monospace", Typeface.NORMAL));
        endTime.setTypeface(Typeface.create("sans-serif-monospace", Typeface.NORMAL));
    }

    @Override
    public void updatePlayPauseImage(boolean isPlaying) {
        //animating play pause change
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            super.updatePlayPauseImage(isPlaying);
        } else {
            Drawable d = playPauseButton.getDrawable();
            if (d instanceof Animatable
                    && ((Animatable) d).isRunning()) {
                ((Animatable) d).stop();
            }

            if (isPlaying) {
                d = ContextCompat.getDrawable(getContext(),
                        R.drawable.play_to_pause_avd);
            } else {
                d = ContextCompat.getDrawable(getContext(),
                        R.drawable.pause_to_play_avd);
            }
            playPauseButton.setImageDrawable(d);

            d = playPauseButton.getDrawable();
            if (d instanceof Animatable) {
                ((Animatable) d).start();
            }
        }
    }

    public void animateVisibility(boolean toVisible) {
        super.animateVisibility(toVisible);
    }

    public void setBottomInsets(int[] bottomInsets) {
        if (controlsContainer != null) {
            controlsContainer.setPadding(getPaddingLeft() + bottomInsets[0],
                    controlsContainer.getPaddingTop()/* + bottomInsets[1]*/,
                    controlsContainer.getPaddingRight() + bottomInsets[2],
                    controlsContainer.getPaddingBottom() + bottomInsets[3]);
        }
    }
}
