package us.koller.cameraroll.adapter.main;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.ui.AlbumActivity;

public class RecyclerViewAdapter extends RecyclerView.Adapter {

    private ArrayList<Album> albums;

    public RecyclerViewAdapter() {
    }

    public RecyclerViewAdapter setAlbums(ArrayList<Album> albums) {
        this.albums = albums;
        return this;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.album_cover, parent, false);
        return new AlbumHolder(v);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        final Album album = albums.get(position);
        ((AlbumHolder) holder).setAlbum(album);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(holder.itemView.getContext(), AlbumActivity.class);
                intent.setAction(AlbumActivity.VIEW_ALBUM);
                intent.putExtra(AlbumActivity.ALBUM, album);
                holder.itemView.getContext().startActivity(intent,
                        ActivityOptionsCompat.makeSceneTransitionAnimation(
                                (Activity) holder.itemView.getContext()).toBundle());
            }
        });
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    static class AlbumHolder extends RecyclerView.ViewHolder {

        private Album album;

        AlbumHolder(View itemView) {
            super(itemView);
        }

        void setAlbum(Album album) {
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

        void loadImage() {
            Glide.clear(itemView.findViewById(R.id.image));
            Glide.with(itemView.getContext())
                    .load(album.getAlbumItems().get(0).getPath())
                    .skipMemoryCache(true)
                    .error(R.drawable.error_placeholder)
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
}
