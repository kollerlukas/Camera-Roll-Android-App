package us.koller.cameraroll.adapter.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.main.ViewHolder.AlbumHolder;
import us.koller.cameraroll.adapter.main.ViewHolder.CardAlbumHolder;
import us.koller.cameraroll.adapter.main.ViewHolder.ExcludedAlbumHolder;
import us.koller.cameraroll.adapter.main.ViewHolder.NestedRecyclerViewAlbumHolder;
import us.koller.cameraroll.adapter.main.ViewHolder.ParallaxAlbumHolder;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.Provider.Provider;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.ui.AlbumActivity;
import us.koller.cameraroll.ui.MainActivity;

public class RecyclerViewAdapter extends RecyclerView.Adapter {

    private int viewType;

    private ArrayList<Album> albums;

    private boolean pick_photos;

    public RecyclerViewAdapter(Context context, boolean pick_photos) {
        this.pick_photos = pick_photos;
        Settings settings = Settings.getInstance(context);
        viewType = settings.getStyle();
    }

    public RecyclerViewAdapter setAlbums(ArrayList<Album> albums) {
        this.albums = albums;
        return this;
    }

    @Override
    public int getItemViewType(int position) {
        boolean albumExcluded
                = Provider.isDirExcluded(albums.get(position).getPath(), Provider.getExcludedPaths()) ||
                Provider.isDirExcludedBecauseParentDirIsExcluded(albums.get(position).getPath(), Provider.getExcludedPaths());
        if (albumExcluded) {
            return viewType + 1;
        } else {
            return viewType;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Resources res = parent.getContext().getResources();
        if (viewType == res.getInteger(R.integer.STYLE_PARALLAX_VALUE)) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.album_cover_parallax, parent, false);
            return new ParallaxAlbumHolder(v);
        } else if (viewType == res.getInteger(R.integer.STYLE_PARALLAX_EXCLUDED_VALUE)) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.album_cover_default_excluded, parent, false);
            return new ExcludedAlbumHolder(v);
        } else if (viewType == res.getInteger(R.integer.STYLE_CARDS_VALUE)) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.album_cover_card, parent, false);
            return new CardAlbumHolder(v);
        } else if (viewType == res.getInteger(R.integer.STYLE_CARDS_EXCLUDED_VALUE)) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.album_cover_card_excluded, parent, false);
            return new CardAlbumHolder(v);
        } else if (viewType == res.getInteger(R.integer.STYLE_NESTED_RECYCLER_VIEW_VALUE)) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.album_cover_nested_recyclerview, parent, false);
            return new NestedRecyclerViewAlbumHolder(v);
        } else if (viewType == res.getInteger(R.integer.STYLE_NESTED_RECYCLER_VIEW_EXCLUDED_VALUE)) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.album_cover_default_excluded, parent, false);
            return new ExcludedAlbumHolder(v);
        }

        return null;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        final Album album = albums.get(position);

        ((AlbumHolder) holder).setAlbum(album);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(holder.itemView.getContext(), AlbumActivity.class);

                //intent.putExtra(AlbumActivity.ALBUM, album);
                intent.putExtra(AlbumActivity.ALBUM_PATH, album.getPath());

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
                    context.startActivityForResult(intent,
                            MainActivity.REFRESH_PHOTOS_REQUEST_CODE, options.toBundle());
                } else {
                    View toolbar = context.findViewById(R.id.toolbar);
                    options = ActivityOptionsCompat.makeSceneTransitionAnimation(context,
                            toolbar, context.getString(R.string.toolbar_transition_name));
                    context.startActivityForResult(intent,
                            MainActivity.PICK_PHOTOS_REQUEST_CODE, options.toBundle());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }
}
