package us.koller.cameraroll.adapter.main.ViewHolder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Album;

public class AlbumHolder extends RecyclerView.ViewHolder {

    private Album album;

    public AlbumHolder(View itemView) {
        super(itemView);
    }

    public void setAlbum(Album album) {
        this.album = album;
        ((TextView) itemView.findViewById(R.id.name)).setText(album.getName());
        String count = album.getAlbumItems().size()
                + (album.getAlbumItems().size() > 1 ?
                itemView.getContext().getString(R.string.items) :
                itemView.getContext().getString(R.string.item));
        ((TextView) itemView.findViewById(R.id.count)).setText(count);
        itemView.findViewById(R.id.hidden_folder_indicator)
                .setVisibility(album.hiddenAlbum ? View.VISIBLE : View.INVISIBLE);
        loadImage();
    }

    private void loadImage() {
        Glide.clear(itemView.findViewById(R.id.image));
        Glide.with(itemView.getContext())
                .load(album.getAlbumItems().get(0).getPath())
                .error(R.drawable.error_placeholder)
                .skipMemoryCache(true)
                //.diskCacheStrategy(DiskCacheStrategy.NONE)
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<GlideDrawable> target,
                                               boolean isFirstResource) {
                        album.getAlbumItems().get(0).error = true;
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target,
                                                   boolean isFromMemoryCache, boolean isFirstResource) {
                        return false;
                    }
                })
                .into((ImageView) itemView.findViewById(R.id.image));
    }
}
