package us.koller.cameraroll.adapter.album.ViewHolder;

import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.util.Util;
import us.koller.cameraroll.util.animators.ColorFade;

public abstract class AlbumItemHolder extends RecyclerView.ViewHolder {

    public AlbumItem albumItem;

    AlbumItemHolder(View itemView) {
        super(itemView);
    }

    public void setAlbumItem(AlbumItem albumItem) {
        this.albumItem = albumItem;
        ImageView imageView = (ImageView) itemView.findViewById(R.id.image);
        loadImage(imageView, albumItem);
    }

    public void loadImage(final ImageView imageView, final AlbumItem albumItem) {
        Glide.clear(imageView);
    }

    void fadeIn() {
        albumItem.hasFadedIn = true;
        ColorFade.fadeSaturation((ImageView) itemView.findViewById(R.id.image));
    }

    public void setSelected(boolean selected) {
        final View imageView = itemView.findViewById(R.id.image);

        final Drawable selectorOverlay = Util
                .getAlbumItemSelectorOverlay(imageView.getContext());
        if (selected) {
            imageView.post(new Runnable() {
                @Override
                public void run() {
                    imageView.getOverlay().clear();
                    selectorOverlay.setBounds(0, 0,
                            imageView.getWidth(),
                            imageView.getHeight());
                    imageView.getOverlay().add(selectorOverlay);
                }
            });
        } else {
            imageView.post(new Runnable() {
                @Override
                public void run() {
                    imageView.getOverlay().clear();
                }
            });
        }
    }
}
