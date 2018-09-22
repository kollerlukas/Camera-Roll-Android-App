package us.koller.cameraroll.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.ImageViewState;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.item.viewHolder.GifViewHolder;
import us.koller.cameraroll.data.models.AlbumItem;
import us.koller.cameraroll.data.models.Photo;

public class ItemViewUtil {

    public static ViewGroup inflatePhotoView(ViewGroup container) {
        return (ViewGroup) LayoutInflater.from(container.getContext())
                .inflate(R.layout.photo_view, container, false);
    }

    public static ViewGroup inflateVideoView(ViewGroup container) {
        return (ViewGroup) LayoutInflater.from(container.getContext())
                .inflate(R.layout.video_view, container, false);
    }

    public static void bindSubsamplingImageView(SubsamplingScaleImageView imageView, Photo photo,
                                                SubsamplingScaleImageView.DefaultOnImageEventListener onImageEventListener) {
        imageView.recycle();

        ImageViewState imageViewState = null;
        if (photo.getImageViewSavedState() != null) {
            imageViewState = (ImageViewState) photo.getImageViewSavedState();
            photo.putImageViewSavedState(null);
        }

        imageView.setImage(
                ImageSource.uri(photo.getUri(imageView.getContext())),
                imageViewState);

        if (onImageEventListener != null) {
            imageView.setOnImageEventListener(onImageEventListener);
        }
    }

    public static void bindTransitionView(final ImageView imageView, final AlbumItem albumItem) {
        //handle timeOut
        if (albumItem.isSharedElement && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            new Handler().postDelayed(() -> {
                albumItem.isSharedElement = false;
                ((AppCompatActivity) imageView.getContext()).startPostponedEnterTransition();
            }, 100);
        }
        ViewCompat.setTransitionName(imageView, albumItem.getPath());
        Context context = imageView.getContext();
        Glide.with(context)
                .asBitmap()
                .load(albumItem.getUri(context))
                .listener(new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Bitmap> target, boolean isFirstResource) {
                        albumItem.error = true;
                        if (albumItem.isSharedElement && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            albumItem.isSharedElement = false;
                            ((AppCompatActivity) imageView.getContext())
                                    .startPostponedEnterTransition();
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target,
                                                   DataSource dataSource, boolean isFirstResource) {
                        if (albumItem.isSharedElement && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            albumItem.isSharedElement = false;
                            ((AppCompatActivity) imageView.getContext())
                                    .startPostponedEnterTransition();
                        }
                        return false;
                    }
                })
                .apply(albumItem.getGlideRequestOptions(imageView.getContext()))
                .into(imageView);
    }

    public static void bindGif(final GifViewHolder gifViewHolder,
                               final ImageView imageView,
                               final AlbumItem albumItem) {
        ViewCompat.setTransitionName(imageView, albumItem.getPath());
        Context context = imageView.getContext();
        Glide.with(context)
                .asGif()
                .load(albumItem.getUri(context))
                .listener(new RequestListener<GifDrawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<GifDrawable> target, boolean isFirstResource) {
                        albumItem.error = true;
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GifDrawable resource, Object model, Target<GifDrawable> target,
                                                   DataSource dataSource, boolean isFirstResource) {
                        resource.start();
                        gifViewHolder.setAttacher(imageView);
                        return false;
                    }
                })
                .apply(albumItem.getGlideRequestOptions(imageView.getContext()))
                .into(imageView);
    }
}
