package us.koller.cameraroll.adapter.main.ViewHolder;

import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.Provider.MediaProvider;
import us.koller.cameraroll.ui.widget.ParallaxImageView;
import us.koller.cameraroll.util.ColorFade;

public class AlbumHolder extends RecyclerView.ViewHolder {

    private Album album;

    public AlbumHolder(View itemView) {
        super(itemView);
    }

    public void setAlbum(Album album) {
        this.album = album;

        if (album == null) {
            //Error album
            album = MediaProvider.getErrorAlbum();
        }

        ((TextView) itemView.findViewById(R.id.name)).setText(album.getName());
        String count = album.getAlbumItems().size()
                + (album.getAlbumItems().size() > 1 ?
                itemView.getContext().getString(R.string.items) :
                itemView.getContext().getString(R.string.item));
        ((TextView) itemView.findViewById(R.id.count)).setText(count);

        itemView.findViewById(R.id.hidden_folder_indicator)
                .setVisibility(album.isHidden() ? View.VISIBLE : View.INVISIBLE);

        loadImage();
    }

    private void loadImage() {
        final AlbumItem coverImage = album.getAlbumItems().get(0);

        final ImageView image = (ImageView) itemView.findViewById(R.id.image);
        if (image instanceof ParallaxImageView) {
            ((ParallaxImageView) image).setParallaxTranslation();
        }

        Glide.with(itemView.getContext())
                .load(coverImage.getPath())
                .asBitmap()
                .error(R.drawable.error_placeholder_tinted)
                .listener(new RequestListener<String, Bitmap>() {
                    @Override
                    public boolean onException(Exception e, String model,
                                               Target<Bitmap> target, boolean isFirstResource) {
                        coverImage.error = true;

                        if (image instanceof ParallaxImageView) {
                            ((ParallaxImageView) image).setParallaxTranslation();
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, String model,
                                                   Target<Bitmap> target, boolean isFromMemoryCache,
                                                   boolean isFirstResource) {
                        if (!coverImage.hasFadedIn) {
                            coverImage.hasFadedIn = true;
                            ColorFade.fadeSaturation(image);
                        }

                        if (image instanceof ParallaxImageView) {
                            ((ParallaxImageView) image).setParallaxTranslation();
                        }
                        return false;
                    }

                })
                .into(image);
    }
}
