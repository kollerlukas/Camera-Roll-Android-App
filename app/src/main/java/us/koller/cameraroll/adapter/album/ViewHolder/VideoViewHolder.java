package us.koller.cameraroll.adapter.album.ViewHolder;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.Video;
import us.koller.cameraroll.util.Util;

public class VideoViewHolder extends AlbumItemHolder {

    public VideoViewHolder(View itemView) {
        super(itemView);
        ImageView indicator = (ImageView) itemView.findViewById(R.id.indicator);
        indicator.setImageResource(R.drawable.video_indicator);
    }

    @Override
    void loadImage(ImageView imageView, final AlbumItem albumItem) {
        Context context = imageView.getContext();

        int screenWidth = Util.getScreenWidth((Activity) context);
        int columnCount = Util.getAlbumActivityGridColumnCount(context);
        //square image
        int[] imageDimens = {
                (int) ((float) screenWidth / columnCount),
                (int) ((float) screenWidth / columnCount)};

        if (albumItem instanceof Video) {
            Glide.with(context)
                    .load(albumItem.getPath())
                    .asBitmap()
                    .thumbnail(0.1f)
                    .skipMemoryCache(true)
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
        }
    }
}
