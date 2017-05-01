package us.koller.cameraroll.adapter.main.ViewHolder;

import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.ui.widget.ParallaxImageView;

public class ParallaxAlbumHolder extends AlbumHolder {

    public ParallaxAlbumHolder(View itemView) {
        super(itemView);
    }

    @Override
    public void setAlbum(Album album) {
        super.setAlbum(album);
        //album not excluded
        String count = album.getAlbumItems().size()
                + (album.getAlbumItems().size() > 1 ?
                getContext().getString(R.string.items) :
                getContext().getString(R.string.item));
        ((TextView) itemView.findViewById(R.id.count)).setText(Html.fromHtml(count));

        final ImageView image = (ImageView) itemView.findViewById(R.id.image);
        if (image instanceof ParallaxImageView) {
            ((ParallaxImageView) image).setParallaxTranslation();
        }
        loadImage(image);
    }
}
