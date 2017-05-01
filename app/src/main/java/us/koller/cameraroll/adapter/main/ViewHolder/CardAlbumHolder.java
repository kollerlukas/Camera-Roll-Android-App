package us.koller.cameraroll.adapter.main.ViewHolder;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.AlbumItem;

public class CardAlbumHolder extends AlbumHolder {

    public CardAlbumHolder(View itemView) {
        super(itemView);
    }

    @Override
    public void setAlbum(Album album) {
        super.setAlbum(album);

        final ImageView image = (ImageView) itemView.findViewById(R.id.image);

        if (!excluded) {
            //album not excluded
            String count = album.getAlbumItems().size()
                    + (album.getAlbumItems().size() > 1 ?
                    getContext().getString(R.string.items) :
                    getContext().getString(R.string.item));
            ((TextView) itemView.findViewById(R.id.count)).setText(Html.fromHtml(count));
        } else {
            //album excluded

            //set image saturation to 0
            //prevent image from fading saturation
            final AlbumItem coverImage = album.getAlbumItems().get(0);
            coverImage.hasFadedIn = true;

            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
            image.setColorFilter(filter);
        }

        loadImage(image);
    }
}
