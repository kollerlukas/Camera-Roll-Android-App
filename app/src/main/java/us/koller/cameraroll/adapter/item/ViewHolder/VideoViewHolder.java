package us.koller.cameraroll.adapter.item.ViewHolder;

import android.app.Activity;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.devbrackets.android.exomedia.listener.OnCompletionListener;
import com.devbrackets.android.exomedia.listener.OnPreparedListener;
import com.devbrackets.android.exomedia.ui.widget.EMVideoView;

import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.Video;
import us.koller.cameraroll.ui.ItemActivity;
import us.koller.cameraroll.ui.widget.CustomVideoControls;
import us.koller.cameraroll.util.ItemViewUtil;

public class VideoViewHolder extends ViewHolder
        implements OnPreparedListener, OnCompletionListener {

    interface BottomInsetCallback {
        void setBottomInset(int[] bottomInsets);
    }

    //left, top, right, bottom
    private static int[] bottomInsets = new int[]{-1, -1, -1, -1};
    private static ArrayList<BottomInsetCallback> callbacks = new ArrayList<>();

    public static void onBottomInset(int[] bottomInsets) {
        VideoViewHolder.bottomInsets = bottomInsets;
        if (callbacks == null) {
            return;
        }
        for (int i = 0; i < callbacks.size(); i++) {
            callbacks.get(i).setBottomInset(bottomInsets);
        }
        callbacks = new ArrayList<>();
    }


    public VideoViewHolder(AlbumItem albumItem, int position) {
        super(albumItem, position);
    }

    @Override
    public View getView(ViewGroup container) {
        ViewGroup v = super.inflateVideoView(container);

        final EMVideoView emVideoView =
                (EMVideoView) itemView.findViewById(R.id.video_view);
        final View transitionView = itemView.findViewById(R.id.image);

        final CustomVideoControls customVideoControls
                = new CustomVideoControls(itemView.getContext());
        customVideoControls.addOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (view.getContext() instanceof ItemActivity) {
                            boolean b = ((ItemActivity) view.getContext()).imageOnClick();
                            customVideoControls.animateVisibility(b);
                        }
                    }
                });
        emVideoView.setControls(customVideoControls);
        emVideoView.setOnPreparedListener(this);

        emVideoView.setVideoURI(albumItem.getUri(itemView.getContext()));
        emVideoView.setOnCompletionListener(this);

        emVideoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean uiVisible = VideoViewHolder.super.imageOnClick(view);
                hideVideoControls(!uiVisible, null);
            }
        });

        hideVideoControls(true, null);
        handleVideoControlsBottomInset();

        ItemViewUtil.bindTransitionView((ImageView) transitionView, albumItem, null);
        /*if (albumItem.isSharedElement) {
            //emVideoView.setVisibility(View.INVISIBLE);
            emVideoView.setAlpha(0.0f);
        } else {
            transitionView.setVisibility(View.INVISIBLE);
        }*/
        emVideoView.setAlpha(0.0f);
        return v;
    }

    public void pauseVideo() {
        final EMVideoView emVideoView =
                (EMVideoView) itemView.findViewById(R.id.video_view);
        if (emVideoView != null && emVideoView.isPlaying()) {
            emVideoView.pause();
        }
    }

    public void swapView(boolean isReturning) {
        //swap views
        final EMVideoView emVideoView =
                (EMVideoView) itemView.findViewById(R.id.video_view);
        final View transitionView = itemView.findViewById(R.id.image);
        //not using visibility, because visibilty change of EMVideoView causes flickering
        //emVideoView.setVisibility(!isReturning ? View.VISIBLE : View.INVISIBLE);
        emVideoView.setAlpha(!isReturning ? 1.0f : 0.0f);
        transitionView.setVisibility(!isReturning ? View.INVISIBLE : View.VISIBLE);
    }

    private void handleVideoControlsBottomInset() {
        final EMVideoView emVideoView =
                (EMVideoView) itemView.findViewById(R.id.video_view);
        final View videoControls = emVideoView.getVideoControls();
        if (videoControls != null) {
            if (bottomInsets[0] != -1) {
                if (videoControls instanceof CustomVideoControls) {
                    ((CustomVideoControls) videoControls).setBottomInsets(bottomInsets);
                }
            } else {
                if (callbacks != null) {
                    callbacks.add(new BottomInsetCallback() {
                        @Override
                        public void setBottomInset(int[] bottomInsets) {
                            if (videoControls instanceof CustomVideoControls) {
                                ((CustomVideoControls) videoControls)
                                        .setBottomInsets(bottomInsets);
                            }
                        }
                    });
                }
            }
        }
    }

    private void hideVideoControls(boolean hide, final ItemActivity.Callback callback) {
        final EMVideoView emVideoView =
                (EMVideoView) itemView.findViewById(R.id.video_view);
        final View videoControls = emVideoView.getVideoControls();
        if (videoControls != null && videoControls instanceof CustomVideoControls) {
            ((CustomVideoControls) videoControls).animateVisibility(!hide);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (callback != null) {
                        callback.callback();
                    }
                }
            }, 300);
        } else {
            if (callback != null) {
                callback.callback();
            }
        }
    }

    public void savePlayedTime() {
        final EMVideoView emVideoView =
                (EMVideoView) itemView.findViewById(R.id.video_view);
        if (albumItem instanceof Video && emVideoView != null) {
            ((Video) albumItem).setPlayedTime(emVideoView.getCurrentPosition());
        }
    }

    @Override
    public void onSharedElementEnter() {
        swapView(false);
    }

    @Override
    public void onSharedElementExit(final ItemActivity.Callback callback) {
        hideVideoControls(true, new ItemActivity.Callback() {
            @Override
            public void callback() {
                swapView(true);
                callback.callback();
            }
        });
    }

    @Override
    public void onCompletion() {
    }

    @Override
    public void onPrepared() {
        if (!albumItem.isSharedElement) {
            swapView(false);
            if (albumItem instanceof Video) {
                final EMVideoView emVideoView =
                        (EMVideoView) itemView.findViewById(R.id.video_view);
                emVideoView.seekTo(((Video) albumItem).getPlayedTime());
                ((Video) albumItem).setPlayedTime(0);
            }
        }
    }
}
