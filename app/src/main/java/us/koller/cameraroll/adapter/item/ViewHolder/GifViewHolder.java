package us.koller.cameraroll.adapter.item.ViewHolder;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.util.ViewUtil;

public class GifViewHolder extends ViewHolder {

    public GifViewHolder(AlbumItem albumItem, int position) {
        super(albumItem, position);
    }

    @Override
    public View getView(ViewGroup container) {
        ViewGroup v = super.inflateView(container);
        v.removeView(v.findViewById(R.id.subsampling));
        View view = v.findViewById(R.id.image);
        if (albumItem.isSharedElement) {
            ViewUtil.bindTransitionView((ImageView) view, albumItem);
        } else {
            ViewUtil.bindGif(this, (ImageView) view, albumItem);
        }
        return v;
    }

    public void reloadGif() {
        View view = itemView.findViewById(R.id.image);
        ViewUtil.bindGif(this, (ImageView) view, albumItem);
    }
}
