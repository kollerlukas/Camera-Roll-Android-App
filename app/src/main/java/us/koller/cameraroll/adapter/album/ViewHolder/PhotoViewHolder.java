package us.koller.cameraroll.adapter.album.ViewHolder;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.util.SizedColorDrawable;
import us.koller.cameraroll.util.Util;

public class PhotoViewHolder extends AlbumItemHolder {

    public PhotoViewHolder(View itemView) {
        super(itemView);
    }

    @Override
    public int[] loadImage(final ImageView imageView, final AlbumItem albumItem) {
        final int[] imageDimens = super.loadImage(imageView, albumItem);

        final Context context = imageView.getContext();

        Glide.with(context)
                .load(albumItem.getPath())
                .asBitmap()
                .skipMemoryCache(true)
                .placeholder(new SizedColorDrawable(ContextCompat.getColor(context, R.color.white_translucent2), imageDimens))
                .override(imageDimens[0], imageDimens[1])
                .listener(new RequestListener<String, Bitmap>() {
                    @Override
                    public boolean onException(Exception e, String model,
                                               Target<Bitmap> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, String model,
                                                   Target<Bitmap> target, boolean isFromMemoryCache,
                                                   boolean isFirstResource) {
                        if (!albumItem.hasFadedIn) {
                            fadeIn();
                        }
                        return false;
                    }
                })
                .error(R.drawable.error_placeholder)
                .into(imageView);

        return imageDimens;
    }
}
