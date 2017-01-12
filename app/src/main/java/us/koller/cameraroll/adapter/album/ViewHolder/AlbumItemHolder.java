package us.koller.cameraroll.adapter.album.ViewHolder;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.AlbumItem;
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

    private static void loadImage(final ImageView imageView, final AlbumItem albumItem) {
        int[] imageDimens = Util.getImageDimensions(albumItem.getPath());
        int screenWidth = Util.getScreenWidth((Activity) imageView.getContext());
        float scale = ((float) screenWidth / 3) / (float) imageDimens[0];
        scale = scale > 1.0f ? 1.0f : scale == 0.0f ? 1.0f : scale;

        Glide.clear(imageView);
        Glide.with(imageView.getContext())
                .load(albumItem.getPath())
                .asBitmap()
                .override((int) (imageDimens[0] * scale), (int) (imageDimens[1] * scale))
                .skipMemoryCache(true)
                .error(R.drawable.error_placeholder)
                .into(imageView);
    }
}
