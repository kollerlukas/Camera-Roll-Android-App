package us.koller.cameraroll.adapter.item.ViewHolder;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import uk.co.senab.photoview.PhotoViewAttacher;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.Photo;
import us.koller.cameraroll.ui.ItemActivity;
import us.koller.cameraroll.util.ItemViewUtil;

public class PhotoViewHolder extends ViewHolder {

    public PhotoViewHolder(AlbumItem albumItem, int position) {
        super(albumItem, position);
    }

    @Override
    public View getView(ViewGroup container) {
        ViewGroup v = super.inflatePhotoView(container);
        final View view = v.findViewById(R.id.subsampling);
        final View transitionView = itemView.findViewById(R.id.image);

        //hide transitionView, when config was changed
        if (albumItem instanceof Photo
                && ((Photo) albumItem).getImageViewSavedState() != null) {
            transitionView.setVisibility(View.INVISIBLE);
        }
        ItemViewUtil.bindTransitionView((ImageView) transitionView, albumItem, null);
        /*if (albumItem.isSharedElement) {
            view.setVisibility(View.INVISIBLE);
        } else {
            bindImageView(view, transitionView);
        }*/
        view.setVisibility(View.INVISIBLE);
        return v;
    }

    public void swapView(final boolean isReturning) {
        final View view = itemView.findViewById(R.id.subsampling);
        final View transitionView = itemView.findViewById(R.id.image);
        if (!isReturning) {
            view.setVisibility(View.VISIBLE);
            bindImageView(view, transitionView);
        } else {
            view.setVisibility(View.INVISIBLE);
            transitionView.setVisibility(View.VISIBLE);
        }
    }

    private void bindImageView(View view, final View transitionView) {
        if (albumItem.error) {
            transitionView.setVisibility(View.VISIBLE);
            ItemViewUtil.bindTransitionView((ImageView) transitionView, albumItem, null);
            return;
        }

        final SubsamplingScaleImageView imageView
                = (SubsamplingScaleImageView) view;

        final GestureDetector gestureDetector
                = new GestureDetector(imageView.getContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        PhotoViewHolder.super.imageOnClick(imageView);
                        return super.onSingleTapUp(e);
                    }
                });
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gestureDetector.onTouchEvent(motionEvent);
            }
        });

        ItemViewUtil.bindSubsamplingImageView(
                imageView,
                (Photo) albumItem,
                new SubsamplingScaleImageView.DefaultOnImageEventListener() {
                    @Override
                    public void onImageLoaded() {
                        super.onImageLoaded();
                        transitionView.setVisibility(View.INVISIBLE);
                    }
                });
    }

    private void scaleDown(final ItemActivity.Callback callback) {
        final SubsamplingScaleImageView imageView
                = (SubsamplingScaleImageView) itemView.findViewById(R.id.subsampling);
        if (imageView != null) {
            try {
                imageView.animateScale(0.0f)
                        .withDuration(300)
                        .withOnAnimationEventListener(
                                new SubsamplingScaleImageView.DefaultOnAnimationEventListener() {
                                    @Override
                                    public void onComplete() {
                                        super.onComplete();
                                        swapView(true);
                                        callback.callback();
                                        //imageView.recycle();
                                    }
                                })
                        .start();
            } catch (NullPointerException e) {
                swapView(true);
                callback.callback();
                //imageView.recycle();
            }
        }
    }

    @Override
    public void onSharedElement(final ItemActivity.Callback callback) {
        scaleDown(new ItemActivity.Callback() {
            @Override
            public void callback() {
                callback.callback();
            }
        });
    }

    @Override
    public void onDestroy() {
        Log.d("PhotoViewHolder", "onDestroy() called");
        final SubsamplingScaleImageView imageView
                = (SubsamplingScaleImageView) itemView.findViewById(R.id.subsampling);
        if (imageView != null) {
            imageView.recycle();
        }

        super.onDestroy();
    }
}
