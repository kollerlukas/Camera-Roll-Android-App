package us.koller.cameraroll.adapter.item.ViewHolder;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.ui.AlbumActivity;
import us.koller.cameraroll.ui.ItemActivity;
import us.koller.cameraroll.util.ItemViewUtil;

public class VideoViewHolder extends ViewHolder {

    public VideoViewHolder(AlbumItem albumItem, int position) {
        super(albumItem, position);
    }

    @Override
    public View getView(ViewGroup container) {
        ViewGroup v = super.inflateView(container);
        v.removeView(v.findViewById(R.id.subsampling));
        View view = v.findViewById(R.id.image);
        if (albumItem.isSharedElement) {
            ItemViewUtil.bindTransitionView((ImageView) view, albumItem);
        } else {
            itemView = v;
            bindView();
        }
        return v;
    }

    public void bindView() {
        View view = itemView.findViewById(R.id.image);
        ViewGroup v = ItemViewUtil.bindImageViewForVideo((ImageView) view, albumItem);
        View playButton = v.findViewWithTag(ItemViewUtil.VIDEO_PLAY_BUTTON_TAG);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlbumActivity.videoOnClick(view.getContext(), albumItem);
            }
        });
        playButton.setAlpha(0.0f);
        playButton.animate().alpha(0.54f).start();
    }

    @Override
    public void onSharedElement(final ItemActivity.Callback callback) {
        callback.callback();
    }
}
