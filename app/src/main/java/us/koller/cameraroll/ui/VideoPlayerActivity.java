package us.koller.cameraroll.ui;

import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.ImageButton;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import us.koller.cameraroll.R;

public class VideoPlayerActivity extends AppCompatActivity {

    public static final String POSITION = "POSITION";
    public static final String PLAY_WHEN_READY = "PLAY_WHEN_READY";

    private SimpleExoPlayer player;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        Intent intent = getIntent();
        Uri videoUri = intent.getData();

        if (videoUri == null) {
            return;
        }

        // 1. Create a default TrackSelector
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(null);
        TrackSelector trackSelector =
                new DefaultTrackSelector(videoTrackSelectionFactory);

        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, getString(R.string.app_name)), null);
        // Produces Extractor instances for parsing the media data.
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        // This is the MediaSource representing the media to be played.
        MediaSource videoSource = new ExtractorMediaSource(videoUri,
                dataSourceFactory, extractorsFactory, null, null);

        // Loops the video indefinitely.
        LoopingMediaSource loopingSource = new LoopingMediaSource(videoSource);

        // 2. Create a default LoadControl
        LoadControl loadControl = new DefaultLoadControl();

        // 3. Create the player
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);

        // Bind the player to the view.
        SimpleExoPlayerView simpleExoPlayerView
                = (SimpleExoPlayerView) findViewById(R.id.simpleExoPlayerView);
        simpleExoPlayerView.setPlayer(player);

        // Prepare the player with the source.
        player.prepare(loopingSource);

        if (savedInstanceState != null) {
            player.seekTo(savedInstanceState.getLong(POSITION));
            player.setPlayWhenReady(savedInstanceState.getBoolean(PLAY_WHEN_READY));
        } else {
            player.setPlayWhenReady(true);
        }

        //needed to achieve transparent navBar
        showOrHideSystemUi(true);

        //init Play pause button
        final View bottomControls = findViewById(R.id.controls);
        final ImageButton playPause = (ImageButton) findViewById(R.id.play_pause);
        playPause.setImageResource(R.drawable.pause_to_play_avd);
        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                player.setPlayWhenReady(!player.getPlayWhenReady());

            }
        });

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(null);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final ViewGroup rootView = (ViewGroup) findViewById(R.id.root_view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            rootView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
                public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                    toolbar.setPadding(toolbar.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                            toolbar.getPaddingTop() + insets.getSystemWindowInsetTop(),
                            toolbar.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                            toolbar.getPaddingBottom());

                    /*ViewGroup.MarginLayoutParams toolbarParams
                         = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
                    toolbarParams.leftMargin += insets.getSystemWindowInsetLeft();
                    toolbarParams.rightMargin += insets.getSystemWindowInsetRight();
                    toolbar.setLayoutParams(toolbarParams);*/

                    bottomControls.setPadding(bottomControls.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                            bottomControls.getPaddingTop(),
                            bottomControls.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                            bottomControls.getPaddingBottom() + insets.getSystemWindowInsetBottom());

                    /*ViewGroup.MarginLayoutParams bottomControlsParams
                            = (ViewGroup.MarginLayoutParams) bottomControls.getLayoutParams();
                    bottomControlsParams.leftMargin += insets.getSystemWindowInsetLeft();
                    bottomControlsParams.rightMargin += insets.getSystemWindowInsetRight();
                    bottomControls.setLayoutParams(bottomControlsParams);*/

                    // clear this listener so insets aren't re-applied
                    rootView.setOnApplyWindowInsetsListener(null);
                    return insets.consumeSystemWindowInsets();
                }
            });
        } else {
            rootView.getViewTreeObserver()
                    .addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    //hacky way of getting window insets on pre-Lollipop
                                    int[] screenSize = us.koller.cameraroll.util.Util
                                            .getScreenSize(VideoPlayerActivity.this);

                                    int[] windowInsets = new int[]{
                                            Math.abs(screenSize[0] - rootView.getLeft()),
                                            Math.abs(screenSize[1] - rootView.getTop()),
                                            Math.abs(screenSize[2] - rootView.getRight()),
                                            Math.abs(screenSize[3] - rootView.getBottom())};

                                    toolbar.setPadding(toolbar.getPaddingStart() + windowInsets[0],
                                            toolbar.getPaddingTop() + windowInsets[1],
                                            toolbar.getPaddingEnd() + windowInsets[2],
                                            toolbar.getPaddingBottom());

                                    bottomControls.setPadding(bottomControls.getPaddingStart() + windowInsets[0],
                                            bottomControls.getPaddingTop(),
                                            bottomControls.getPaddingEnd() + windowInsets[2],
                                            bottomControls.getPaddingBottom() + windowInsets[3]);

                                    rootView.getViewTreeObserver()
                                            .removeOnGlobalLayoutListener(this);
                                }
                            });
        }

        player.addListener(new ExoPlayer.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, Object o) {
            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroupArray, TrackSelectionArray trackSelectionArray) {
            }

            @Override
            public void onLoadingChanged(boolean b) {
            }

            @Override
            public void onPlayerStateChanged(boolean b, int i) {
                Log.d("VideoPlayerActivity", "onPlayerStateChanged() called with: b = [" + b + "], i = [" + i + "]");
                if (player.getPlayWhenReady()) {
                    playPause.setImageResource(R.drawable.play_to_pause_avd);
                } else {
                    playPause.setImageResource(R.drawable.pause_to_play_avd);
                }

                Drawable d = playPause.getDrawable();
                if (d instanceof Animatable) {
                    ((Animatable) d).start();
                }
            }

            @Override
            public void onPlayerError(ExoPlaybackException e) {
            }

            @Override
            public void onPositionDiscontinuity() {
            }
        });

        PlaybackControlView playbackControlView = (PlaybackControlView)
                findViewById(R.id.playback_control_view).getParent();
        playbackControlView.setVisibilityListener(
                new PlaybackControlView.VisibilityListener() {
                    @Override
                    public void onVisibilityChange(int i) {
                        showOrHideSystemUi(i == View.VISIBLE);
                    }
                });
    }

    public void showOrHideSystemUi(boolean show) {
        if (show) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                break;
        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(POSITION, player.getCurrentPosition());
        outState.putBoolean(PLAY_WHEN_READY, player.getPlayWhenReady());
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (player != null) {
            player.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (player != null) {
            player.release();
        }
    }
}
