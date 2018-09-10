package us.koller.cameraroll.adapter.main.viewHolder;

import android.view.View;
import android.widget.ImageView;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.models.Album;
import us.koller.cameraroll.ui.widget.ParallaxImageView;

public class SimpleAlbumHolder extends AlbumHolder {
    public SimpleAlbumHolder(View itemView) {
        super(itemView);
    }

    @Override
    public void setAlbum(Album a) {
        super.setAlbum(a);
        final ImageView image = itemView.findViewById(R.id.image);
        if (image instanceof ParallaxImageView) {
            ((ParallaxImageView) image).setParallaxTranslation();
        }
        loadImage(image);
    }
}
