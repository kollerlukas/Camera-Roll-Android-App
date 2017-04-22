package us.koller.cameraroll.adapter.main.ViewHolder;

import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.ui.widget.ParallaxImageView;

public class AlbumHolderParallax extends AlbumHolder {

    public AlbumHolderParallax(View itemView) {
        super(itemView);
    }

    @Override
    public void setAlbum(Album album) {
        super.setAlbum(album);

        ((TextView) itemView.findViewById(R.id.name)).setText(album.getName());

        if (!excluded) {
            //album not excluded
            String count = album.getAlbumItems().size()
                    + (album.getAlbumItems().size() > 1 ?
                    getContext().getString(R.string.items) :
                    getContext().getString(R.string.item));
            ((TextView) itemView.findViewById(R.id.count)).setText(Html.fromHtml(count));

            itemView.findViewById(R.id.hidden_folder_indicator)
                    .setVisibility(album.isHidden() ? View.VISIBLE : View.GONE);

            final ImageView image = (ImageView) itemView.findViewById(R.id.image);
            if (image instanceof ParallaxImageView) {
                ((ParallaxImageView) image).setParallaxTranslation();
            }
            loadImage(image);
        } else {
            //album excluded
        }
    }
}
