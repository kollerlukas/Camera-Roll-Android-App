package us.koller.cameraroll.adapter.item.viewHolder;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.models.AlbumItem;
import us.koller.cameraroll.ui.ItemActivity;
import us.koller.cameraroll.util.ItemViewUtil;

public class VideoViewHolder extends ViewHolder {

    public VideoViewHolder(AlbumItem albumItem, int position) {
        super(albumItem, position);
    }

    @Override
    public View inflateView(ViewGroup container) {
        ViewGroup v = super.inflateVideoView(container);
        final View view = itemView.findViewById(R.id.image);

        ItemViewUtil.bindTransitionView((ImageView) view, albumItem);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ItemActivity.videoOnClick(view.getContext(), albumItem);
            }
        });
        return v;
    }

    @Override
    public void onSharedElementEnter() {
        final View view = itemView.findViewById(R.id.image);

        Resources res = itemView.getContext().getResources();
        final Drawable playOverlay = VectorDrawableCompat.create(res,
                R.drawable.play_indicator, itemView.getContext().getTheme());

        if (playOverlay == null) {
            return;
        }

        view.post(new Runnable() {
            @Override
            public void run() {
                int dimen = (int) view.getContext().getResources()
                        .getDimension(R.dimen.twenty_four_dp) * 2;

                int left = view.getWidth() / 2 - dimen / 2;
                int top = view.getHeight() / 2 - dimen / 2;

                playOverlay.setBounds(left, top, left + dimen, top + dimen);
                view.getOverlay().add(playOverlay);
            }
        });
    }

    @Override
    public void onSharedElementExit(final ItemActivity.Callback callback) {
        final View view = itemView.findViewById(R.id.image);
        view.post(new Runnable() {
            @Override
            public void run() {
                view.getOverlay().clear();
            }
        });
        callback.done();
    }
}
