package us.koller.cameraroll.ui.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

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
    public void updatePlayPauseImage(boolean isPlaying) {
        //animating play pause change
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            super.updatePlayPauseImage(isPlaying);
        } else {
            Drawable d = playPauseButton.getDrawable();
            if (d instanceof Animatable && ((Animatable) d).isRunning()) {
                ((Animatable) d).stop();
            }
            if (isPlaying) {
                playPauseButton.setImageDrawable(AnimatedVectorDrawableCompat
                        .create(getContext(), R.drawable.play_to_pause_animatable));
            } else {
                playPauseButton.setImageDrawable(AnimatedVectorDrawableCompat
                        .create(getContext(), R.drawable.pause_to_play_animatable));
            }
            d = playPauseButton.getDrawable();
            if (d instanceof Animatable) {
                ((Animatable) d).start();
            }
        }
    }

    @Override
    protected void retrieveViews() {
        super.retrieveViews();
        //customize VideoControls
        controlsContainer.setBackground(
                ContextCompat.getDrawable(getContext(),
                        R.drawable.transparent_to_dark_gradient));
        currentTime.setTypeface(Typeface.create("sans-serif-monospace", Typeface.NORMAL));
        endTime.setTypeface(Typeface.create("sans-serif-monospace", Typeface.NORMAL));
        //set animated-vector
        updatePlayPauseImage(false);
    }

    public void animateVisibility(boolean toVisible) {
        super.animateVisibility(toVisible);
    }

    public void setBottomInsets(int[] bottomInsets) {
        View videoControlsContainer = findViewById(R.id.exomedia_controls_interactive_container);
        if (videoControlsContainer != null) {
            videoControlsContainer.setPadding(getPaddingLeft() + bottomInsets[0],
                    videoControlsContainer.getPaddingTop()/* + bottomInsets[1]*/,
                    videoControlsContainer.getPaddingRight() + bottomInsets[2],
                    videoControlsContainer.getPaddingBottom() + bottomInsets[3]);
        }
    }
}
