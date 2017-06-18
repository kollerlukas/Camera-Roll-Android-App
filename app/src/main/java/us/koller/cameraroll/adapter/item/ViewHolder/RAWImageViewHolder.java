package us.koller.cameraroll.adapter.item.ViewHolder;

import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import uk.co.senab.photoview.PhotoViewAttacher;
import us.koller.cameraroll.R;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.ui.ItemActivity;
import us.koller.cameraroll.util.ItemViewUtil;

public class RAWImageViewHolder extends ViewHolder {

    private PhotoViewAttacher attacher;

    public RAWImageViewHolder(AlbumItem albumItem, int position) {
        super(albumItem, position);
    }

    @Override
    public View inflateView(ViewGroup container) {
        ViewGroup v = super.inflateRAWPhotoView(container);
        final View view = v.findViewById(R.id.full_size_image);
        final View transitionView = itemView.findViewById(R.id.image);

        ItemViewUtil.bindTransitionView((ImageView) transitionView, albumItem);
        view.setVisibility(View.INVISIBLE);
        return v;
    }

    private void swapView(final boolean isReturning) {
        final View view = itemView.findViewById(R.id.full_size_image);
        final View transitionView = itemView.findViewById(R.id.image);
        if (!isReturning) {
            view.setVisibility(View.VISIBLE);
            loadFullSizeImage();
        } else {
            view.setVisibility(View.INVISIBLE);
            transitionView.setVisibility(View.VISIBLE);
        }
    }

    private void loadFullSizeImage() {
        final ImageView view = itemView.findViewById(R.id.full_size_image);
        final View transitionView = itemView.findViewById(R.id.image);
        RequestListener<String, Bitmap> requestListener
                = new RequestListener<String, Bitmap>() {
            @Override
            public boolean onException(Exception e, String model,
                                       Target<Bitmap> target,
                                       boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(Bitmap resource, String model,
                                           Target<Bitmap> target, boolean isFromMemoryCache,
                                           boolean isFirstResource) {
                setAttacher(view);
                transitionView.setVisibility(View.VISIBLE);
                return false;
            }
        };
        ItemViewUtil.bindRAWImage(view, albumItem, requestListener);
    }

    private void setAttacher(ImageView imageView) {
        attacher = new PhotoViewAttacher(imageView);
        attacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override
            public void onViewTap(View view, float x, float y) {
                imageOnClick(view);
            }
        });
        attacher.setMaximumScale(10.0f);
    }

    @Override
    public void onSharedElementEnter() {
        swapView(false);
    }

    @Override
    public void onSharedElementExit(final ItemActivity.Callback callback) {
        attacher.setZoomTransitionDuration(300);
        attacher.setScale(1.0f, true);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (attacher != null) {
                    attacher.cleanup();
                    attacher = null;
                }
                swapView(true);
                callback.done();
            }
        }, 300);
    }

    @Override
    public void onDestroy() {
        if (attacher != null) {
            attacher.cleanup();
            attacher = null;
        }
        super.onDestroy();
    }
}
