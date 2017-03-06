package us.koller.cameraroll.adapter.item.ViewHolder;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import uk.co.senab.photoview.PhotoViewAttacher;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.ui.ItemActivity;
import us.koller.cameraroll.util.ItemViewUtil;

public class GifViewHolder extends ViewHolder {

    private PhotoViewAttacher attacher;

    public GifViewHolder(AlbumItem albumItem, int position) {
        super(albumItem, position);
    }

    @Override
    public View getView(ViewGroup container) {
        ViewGroup v = super.inflatePhotoView(container);
        v.removeView(v.findViewById(R.id.subsampling));
        View view = v.findViewById(R.id.image);
        if (albumItem.isSharedElement) {
            ItemViewUtil.bindTransitionView((ImageView) view, albumItem);
        } else {
            ItemViewUtil.bindGif(this, (ImageView) view, albumItem);
        }
        return v;
    }

    public void reloadGif() {
        View view = itemView.findViewById(R.id.image);
        ItemViewUtil.bindGif(this, (ImageView) view, albumItem);
    }

    public void setAttacher(ImageView imageView) {
        if (attacher != null) {
            attacher.update();
        } else {
            attacher = new PhotoViewAttacher(imageView);
            attacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
                @Override
                public void onViewTap(View view, float x, float y) {
                    imageOnClick(view);
                }
            });
        }
    }

    @Override
    public void onSharedElement(final ItemActivity.Callback callback) {
        if (attacher != null) {
            attacher.cleanup();
            attacher = null;
        }
        callback.callback();
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
