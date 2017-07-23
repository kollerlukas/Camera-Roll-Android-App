package us.koller.cameraroll.adapter.item.viewHolder;

import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.AlbumItem;
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

        final Drawable playOverlay
                = ContextCompat.getDrawable(itemView.getContext(),
                R.drawable.ic_play_circle_filled_white_24dp);

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
