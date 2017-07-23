package us.koller.cameraroll.adapter.album.viewHolder;

import android.view.View;
import android.widget.ImageView;

import us.koller.cameraroll.data.AlbumItem;

public class PhotoViewHolder extends AlbumItemHolder {

    public PhotoViewHolder(View itemView) {
        super(itemView);
    }

    @Override
    public void loadImage(final ImageView imageView, final AlbumItem albumItem) {
        super.loadImage(imageView, albumItem);
    }
}
