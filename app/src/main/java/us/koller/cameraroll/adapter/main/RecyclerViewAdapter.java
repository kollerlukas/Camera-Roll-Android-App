package us.koller.cameraroll.adapter.main;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.main.ViewHolder.AlbumHolder;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.ui.AlbumActivity;
import us.koller.cameraroll.ui.MainActivity;

public class RecyclerViewAdapter extends RecyclerView.Adapter {

    private ArrayList<Album> albums;

    private boolean pick_photos;

    public RecyclerViewAdapter(boolean pick_photos) {
        this.pick_photos = pick_photos;
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
                intent.putExtra(AlbumActivity.ALBUM, album);
                if (pick_photos) {
                    intent.setAction(MainActivity.PICK_PHOTOS);
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,
                            intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false));
                } else {
                    intent.setAction(AlbumActivity.VIEW_ALBUM);
                }
                ActivityOptionsCompat options;
                Activity context = (Activity) holder.itemView.getContext();
                if (!pick_photos) {
                    options = ActivityOptionsCompat.makeSceneTransitionAnimation(context);
                    holder.itemView.getContext().startActivity(intent, options.toBundle());
                } else {
                    View toolbar = context.findViewById(R.id.toolbar);
                    options = ActivityOptionsCompat.makeSceneTransitionAnimation(context,
                            toolbar, context.getString(R.string.toolbar_transition_name));
                    context.startActivityForResult(intent, MainActivity.PICK_PHOTOS_REQUEST_CODE, options.toBundle());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }
}
