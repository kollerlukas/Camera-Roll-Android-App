package us.koller.cameraroll.adapter.item.ViewHolder;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Album;

public class GifViewHolder extends ViewHolder {

    public GifViewHolder(Album.AlbumItem albumItem, int position) {
        super(albumItem, position);
    }

    @Override
    public View getView(ViewGroup container) {
        ViewGroup v = super.inflateView(container);
        v.removeView(v.findViewById(R.id.subsampling));
        View view = v.findViewById(R.id.image);
        super.setOnClickListener(view);
        ViewHolder.ViewUtil.bindTransitionView((ImageView) view, albumItem);
        return v;
    }

    public void reloadGif() {
        View view = itemView.findViewById(R.id.image);
        ViewHolder.ViewUtil.bindTransitionView((ImageView) view, albumItem);
        super.setOnClickListener(view);
    }
}
