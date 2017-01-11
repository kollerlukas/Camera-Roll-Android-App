package us.koller.cameraroll.adapter.album.ViewHolder;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

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
        int imageWidth = Util.getScreenWidth((Activity) imageView.getContext()) / 3;
        Glide.clear(imageView);
        Glide.with(imageView.getContext())
                .load(albumItem.getPath())
                .override(imageWidth, imageWidth)
                .skipMemoryCache(true)
                .error(R.drawable.error_placeholder)
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<GlideDrawable> target,
                                               boolean isFirstResource) {
                        albumItem.error = true;
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target,
                                                   boolean isFromMemoryCache, boolean isFirstResource) {
                        return false;
                    }
                })
                .into(imageView);
    }
}
