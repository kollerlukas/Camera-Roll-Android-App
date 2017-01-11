package us.koller.cameraroll.util;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.ImageViewState;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import uk.co.senab.photoview.PhotoViewAttacher;

import java.io.IOException;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.Gif;
import us.koller.cameraroll.data.Photo;
import us.koller.cameraroll.ui.ItemActivity;

public class ViewUtil {
    public static ViewGroup inflateView(ViewGroup container) {
        final ViewGroup v = (ViewGroup) LayoutInflater.from(container.getContext())
                .inflate(R.layout.photo_view, container, false);
        return v;
    }

    public static View bindSubsamplingImageView(SubsamplingScaleImageView imageView,
                                                Photo photo, final View placeholderView) {
        ImageViewState imageViewState = null;
        if (photo.getImageViewSavedState() != null) {
            imageViewState = (ImageViewState) photo.getImageViewSavedState();
            photo.putImageViewSavedState(null);
        }

        if (!photo.contentUri) {
            if (imageViewState != null) {
                imageView.setImage(ImageSource.uri(photo.getPath()), imageViewState);
                Log.d("ViewUtil", "restored state...");
            } else {
                imageView.setImage(ImageSource.uri(photo.getPath()));
            }
        } else {
            try {
                Bitmap bmp = MediaStore.Images.Media.getBitmap(
                        imageView.getContext().getContentResolver(), Uri.parse(photo.getPath()));
                if (imageViewState != null) {
                    imageView.setImage(ImageSource.bitmap(bmp), imageViewState);
                    Log.d("ViewUtil", "restored state...");
                } else {
                    imageView.setImage(ImageSource.bitmap(bmp));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        imageView.setMinimumDpi(1);
        if (placeholderView != null) {
            imageView.setOnImageEventListener(
                    new SubsamplingScaleImageView.DefaultOnImageEventListener() {
                        @Override
                        public void onImageLoaded() {
                            placeholderView.setVisibility(View.INVISIBLE);
                            super.onImageLoaded();
                        }
                    });
        }
        return imageView;
    }

    public static View bindTransitionView(final ImageView imageView,
                                          final AlbumItem albumItem) {
        int imageWidth = Util.getScreenWidth((Activity) imageView.getContext())
                / (albumItem instanceof Gif ? 1 : 2);
        Glide.clear(imageView);
        Glide.with(imageView.getContext())
                .load(albumItem.getPath())
                .override(imageWidth, imageWidth)
                .skipMemoryCache(true)
                .error(R.drawable.error_placeholder)
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model,
                                               Target<GlideDrawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable>
                            target, boolean isFromMemoryCache, boolean isFirstResource) {
                        if (albumItem.isSharedElement) {
                            albumItem.isSharedElement = false;
                            ((ItemActivity) imageView.getContext()).startPostponedEnterTransition();
                        }

                        if (!albumItem.isSharedElement && albumItem instanceof Gif) {
                            new PhotoViewAttacher(imageView);
                        }
                        return false;
                    }
                })
                .into(imageView);
        imageView.setTransitionName(albumItem.getPath());
        return imageView;
    }
}
