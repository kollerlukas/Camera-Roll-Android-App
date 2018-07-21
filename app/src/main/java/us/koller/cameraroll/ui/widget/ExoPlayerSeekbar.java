package us.koller.cameraroll.ui.widget;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.widget.SeekBar;

import com.google.android.exoplayer2.ui.TimeBar;

public class ExoPlayerSeekbar extends AppCompatSeekBar implements TimeBar, SeekBar.OnSeekBarChangeListener {

    private OnScrubListener listener;

    public ExoPlayerSeekbar(Context context) {
        super(context);
    }

    public ExoPlayerSeekbar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExoPlayerSeekbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //@Override (doesn\'t override method from superclass)
    public void setListener(OnScrubListener listener) {
        setOnSeekBarChangeListener(this);
        this.listener = listener;
    }

    @Override
    public void addListener(OnScrubListener listener) {
    }

    @Override
    public void removeListener(OnScrubListener listener) {
    }

    @Override
    public void setKeyTimeIncrement(long time) {
    }

    @Override
    public void setKeyCountIncrement(int count) {
    }

    @Override
    public void setPosition(long position) {
        setProgress((int) position);
    }

    @Override
    public void setBufferedPosition(long bufferedPosition) {
        setSecondaryProgress((int) bufferedPosition);
    }

    @Override
    public void setDuration(long duration) {
        setMax((int) duration);
    }

    @Override
    public void setAdGroupTimesMs(@Nullable long[] adGroupTimesMs, @Nullable boolean[] playedAdGroups, int adGroupCount) {
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        if (listener != null) {
            listener.onScrubMove(this, i);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (listener != null) {
            listener.onScrubStart(this, getProgress());
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (listener != null) {
            listener.onScrubStop(this, getProgress(), false);
        }
    }
}
