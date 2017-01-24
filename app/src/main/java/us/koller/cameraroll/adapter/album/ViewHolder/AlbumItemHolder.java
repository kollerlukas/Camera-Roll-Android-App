package us.koller.cameraroll.adapter.album.ViewHolder;

import android.app.Activity;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.Gif;
import us.koller.cameraroll.data.Video;
import us.koller.cameraroll.util.ColorFade;
import us.koller.cameraroll.util.Util;

public class AlbumItemHolder extends RecyclerView.ViewHolder {
    public AlbumItem albumItem;

    public AlbumItemHolder(View itemView) {
        super(itemView);
    }

    public void setAlbumItem(AlbumItem albumItem) {
        this.albumItem = albumItem;
        ImageView imageView = (ImageView) itemView.findViewById(R.id.image);
        loadImage(imageView, albumItem);
    }

    private void loadImage(final ImageView imageView, final AlbumItem albumItem) {
        int screenWidth = Util.getScreenWidth((Activity) imageView.getContext());
        int columnCount = Util.getAlbumActivityGridColumnCount(imageView.getContext());
        //square image
        int[] imageDimens = {
                (int) ((float) screenWidth / columnCount),
                (int) ((float) screenWidth / columnCount)};

        if (albumItem instanceof Video) {
            Glide.with(imageView.getContext())
                    .load(albumItem.getPath())
                    .asBitmap()
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
        } else if (albumItem instanceof Gif) {
            Glide.with(imageView.getContext())
                    .load(albumItem.getPath())
                    .asGif()
                    .override(imageDimens[0], imageDimens[1])
                    .listener(new RequestListener<String, GifDrawable>() {
                        @Override
                        public boolean onException(Exception e, String model,
                                                   Target<GifDrawable> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GifDrawable resource, String model,
                                                       Target<GifDrawable> target, boolean isFromMemoryCache,
                                                       boolean isFirstResource) {
                            if (!albumItem.hasFadedIn) {
                                fadeIn();
                            }
                            return false;
                        }
                    })
                    .error(R.drawable.error_placeholder)
                    .into(imageView);
        } else {
            Glide.with(imageView.getContext())
                    .load(albumItem.getPath())
                    .asBitmap()
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

    private void fadeIn() {
        albumItem.hasFadedIn = true;
        ColorFade.fadeSaturation((ImageView) itemView.findViewById(R.id.image));
    }
}
