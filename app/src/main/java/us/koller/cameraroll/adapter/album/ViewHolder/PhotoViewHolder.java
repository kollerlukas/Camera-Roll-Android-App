package us.koller.cameraroll.adapter.album.ViewHolder;

import android.view.View;
import android.widget.ImageView;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.RAWImage;

public class PhotoViewHolder extends AlbumItemHolder {

    public PhotoViewHolder(View itemView) {
        super(itemView);
    }

    @Override
    public void loadImage(final ImageView imageView, final AlbumItem albumItem) {
        super.loadImage(imageView, albumItem);

        ImageView indicator = itemView.findViewById(R.id.indicator);
        if (albumItem instanceof RAWImage) {
            indicator.setImageResource(R.drawable.raw_indicator);
        } else {
            indicator.setImageDrawable(null);
        }
    }
}
