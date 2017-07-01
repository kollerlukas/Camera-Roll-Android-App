package us.koller.cameraroll.adapter.album.ViewHolder;

import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.RAWImage;

public class PhotoViewHolder extends AlbumItemHolder {

    public PhotoViewHolder(View itemView) {
        super(itemView);
    }

    public PhotoViewHolder(View itemView, RequestManager glideRequestManager) {
        super(itemView, glideRequestManager);
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

        getGlideRequestManager()
                .load(albumItem.getPath())
                .asBitmap()
                .thumbnail(0.1f)
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
                            if (isFirstResource) {
                                //set thumbnail saturation to 0
                                ColorMatrix matrix = new ColorMatrix();
                                matrix.setSaturation(0);
                                imageView.setColorFilter(new ColorMatrixColorFilter(matrix));
                            } else {
                                fadeIn();
                            }
                        } else {
                            imageView.clearColorFilter();
                        }
                        return false;
                    }
                })
                .error(R.drawable.error_placeholder_tinted)
                .into(imageView);
    }
}
