package us.koller.cameraroll.util;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.BitmapRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.ImageViewState;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.item.ViewHolder.GifViewHolder;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.Photo;

public class ItemViewUtil {

    public static ViewGroup inflatePhotoView(ViewGroup container) {
        return (ViewGroup) LayoutInflater.from(container.getContext())
                .inflate(R.layout.photo_view, container, false);
    }

    public static ViewGroup inflateVideoView(ViewGroup container) {
        return (ViewGroup) LayoutInflater.from(container.getContext())
                .inflate(R.layout.video_view, container, false);
    }

    public static void bindSubsamplingImageView(SubsamplingScaleImageView imageView,
                                                Photo photo,
                                                SubsamplingScaleImageView.DefaultOnImageEventListener onImageEventListener) {
        ImageViewState imageViewState = null;
        if (photo.getImageViewSavedState() != null) {
            imageViewState = (ImageViewState) photo.getImageViewSavedState();
            photo.putImageViewSavedState(null);
        }

        imageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF);
        imageView.setMinimumDpi(80);

        imageView.setImage(ImageSource.uri(photo.getPath()), imageViewState);

        if (onImageEventListener != null) {
            imageView.setOnImageEventListener(onImageEventListener);
        }
    }

    public static void bindTransitionView(final ImageView imageView,
                                          final AlbumItem albumItem) {
        /*int[] imageDimens = albumItem.getImageDimens(imageView.getContext());
        if (imageView.getWidth() > 0) {
            //trying to scale image to half of the imageView with --> lower res faster animations
            int imageViewWidth = imageView.getWidth();
            float scale = ((float) imageViewWidth/2) / (float) imageDimens[0];
            scale = scale > 1.0f ? 1.0f : scale == 0.0f ? 1.0f : scale;
            imageDimens[0] =
                    (int) (imageDimens[0] * scale * 0.5f) > 0
                            ? (int) (imageDimens[0] * scale * 0.5f) : 1;
            imageDimens[1] =
                    (int) (imageDimens[1] * scale * 0.5f) > 0
                            ? (int) (imageDimens[1] * scale * 0.5f) : 1;
        } else {
            imageDimens[0] = imageDimens[0] / 2;
            imageDimens[1] = imageDimens[1] / 2;
        }

        if (imageDimens[0] <= 1 || imageDimens[1] <= 1) {
            return;
        }*/

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            imageView.setTransitionName(albumItem.getPath());
        }

        //handle timeOut
        if (albumItem.isSharedElement
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            new Handler().postDelayed(new Runnable() {
                @Override
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                public void run() {
                    albumItem.isSharedElement = false;
                    ((AppCompatActivity) imageView.getContext())
                            .startPostponedEnterTransition();
                }
            }, 300);
        }

        BitmapRequestBuilder<String, Bitmap> requestBuilder = Glide.with(imageView.getContext())
                .load(albumItem.getPath())
                .asBitmap();
        /*if (!(albumItem instanceof Gif)) {
            //not overriding image size for a gif --> OOM
            requestBuilder = requestBuilder.override(imageDimens[0], imageDimens[1]);
        }*/
        requestBuilder.error(R.drawable.error_placeholder_tinted)
                .listener(new RequestListener<String, Bitmap>() {
                    @Override
                    public boolean onException(Exception e, String model,
                                               Target<Bitmap> target,
                                               boolean isFirstResource) {
                        if (albumItem.isSharedElement
                                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            albumItem.isSharedElement = false;
                            ((AppCompatActivity) imageView.getContext())
                                    .startPostponedEnterTransition();
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, String model,
                                                   Target<Bitmap> target, boolean isFromMemoryCache,
                                                   boolean isFirstResource) {
                        if (albumItem.isSharedElement
                                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            albumItem.isSharedElement = false;
                            ((AppCompatActivity) imageView.getContext())
                                    .startPostponedEnterTransition();
                        }

                        return false;
                    }
                })
                .into(imageView);
    }

    public static void bindGif(final GifViewHolder gifViewHolder,
                               final ImageView imageView,
                               final AlbumItem albumItem) {
        Glide.with(imageView.getContext())
                .load(albumItem.getPath())
                .asGif()
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .error(R.drawable.error_placeholder_tinted)
                .listener(new RequestListener<String, GifDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model,
                                               Target<GifDrawable> target,
                                               boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GifDrawable resource, String model,
                                                   Target<GifDrawable> target, boolean isFromMemoryCache,
                                                   boolean isFirstResource) {
                        resource.start();
                        gifViewHolder.setAttacher(imageView);
                        return false;
                    }
                })
                .into(imageView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            imageView.setTransitionName(albumItem.getPath());
        }
    }
}
