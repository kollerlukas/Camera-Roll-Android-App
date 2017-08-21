package us.koller.cameraroll.adapter.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.themes.Theme;
import us.koller.cameraroll.adapter.SelectorModeManager;
import us.koller.cameraroll.adapter.main.viewHolder.AlbumHolder;
import us.koller.cameraroll.adapter.main.viewHolder.NestedRecyclerViewAlbumHolder;
import us.koller.cameraroll.adapter.main.viewHolder.SimpleAlbumHolder;
import us.koller.cameraroll.data.models.Album;
import us.koller.cameraroll.data.provider.Provider;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.ui.AlbumActivity;
import us.koller.cameraroll.ui.MainActivity;
import us.koller.cameraroll.ui.ThemeableActivity;

public class RecyclerViewAdapter extends RecyclerView.Adapter {

    private int viewType;

    private ArrayList<Album> albums;

    private boolean pick_photos;

    private SelectorModeManager selectorManager;

    public RecyclerViewAdapter(Context context, boolean pick_photos) {
        this.pick_photos = pick_photos;
        Settings settings = Settings.getInstance(context);

        viewType = settings.getStyle(context, pick_photos);
        // not allowing NestedRecyclerView Style, when picking photos
        Resources res = context.getResources();
        if (pick_photos && viewType == res
                .getInteger(R.integer.STYLE_NESTED_RECYCLER_VIEW_VALUE)) {
            viewType = res.getInteger(R.integer.STYLE_CARDS_VALUE);
        }

        selectorManager = new SelectorModeManager();
    }

    public RecyclerViewAdapter setAlbums(ArrayList<Album> albums) {
        this.albums = albums;
        return this;
    }

    @Override
    public int getItemViewType(int position) {
        boolean albumExcluded
                = Provider.isDirExcluded(albums.get(position).getPath(),
                Provider.getExcludedPaths()) ||
                Provider.isDirExcludedBecauseParentDirIsExcluded(
                        albums.get(position).getPath(), Provider.getExcludedPaths());
        if (albumExcluded) {
            return viewType + 1;
        } else {
            return viewType;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder;
        Resources res = parent.getContext().getResources();
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == res.getInteger(R.integer.STYLE_PARALLAX_VALUE)) {
            View v = inflater.inflate(R.layout.album_cover_parallax, parent, false);
            viewHolder = new SimpleAlbumHolder(v);
        } else if (viewType == res.getInteger(R.integer.STYLE_CARDS_VALUE)) {
            View v = inflater.inflate(R.layout.album_cover_card, parent, false);
            viewHolder = new SimpleAlbumHolder(v);
        } else if (viewType == res.getInteger(R.integer.STYLE_CARDS_2_VALUE)) {
            View v = inflater.inflate(R.layout.album_cover_card_2, parent, false);
            viewHolder = new SimpleAlbumHolder(v);
        } else /*if (viewType == res.getInteger(R.integer.STYLE_NESTED_RECYCLER_VIEW_VALUE))*/ {
            View v = inflater.inflate(R.layout.album_cover_nested_recyclerview, parent, false);
            viewHolder = new NestedRecyclerViewAlbumHolder(v).setSelectorModeManager(selectorManager);
        }
        Context context = viewHolder.itemView.getContext();
        Theme theme = Settings.getInstance(context).getThemeInstance(context);
        ThemeableActivity.checkTags((ViewGroup) viewHolder.itemView, theme);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        final Album album = albums.get(position);

        if (!album.equals(((AlbumHolder) holder).getAlbum())) {
            ((AlbumHolder) holder).setAlbum(album);
        } else {
            ((AlbumHolder) holder).onItemChanged();
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(holder.itemView.getContext(), AlbumActivity.class);

                //intent.putExtra(AlbumActivity.ALBUM, album);
                intent.putExtra(AlbumActivity.ALBUM_PATH, album.getPath());

                if (pick_photos) {
                    Context c = holder.itemView.getContext();
                    boolean allowMultiple = false;
                    if (c instanceof Activity) {
                        Activity a = (Activity) c;
                        allowMultiple = a.getIntent()
                                .getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
                    }
                    intent.setAction(MainActivity.PICK_PHOTOS);
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);
                } else {
                    intent.setAction(AlbumActivity.VIEW_ALBUM);
                }

                ActivityOptionsCompat options;
                Activity context = (Activity) holder.itemView.getContext();
                if (!pick_photos) {
                    //noinspection unchecked
                    options = ActivityOptionsCompat.makeSceneTransitionAnimation(context);
                    context.startActivityForResult(intent,
                            MainActivity.REFRESH_PHOTOS_REQUEST_CODE, options.toBundle());
                } else {
                    View toolbar = context.findViewById(R.id.toolbar);
                    options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            context, toolbar, context.getString(R.string.toolbar_transition_name));
                    context.startActivityForResult(intent,
                            MainActivity.PICK_PHOTOS_REQUEST_CODE, options.toBundle());
                }
            }
        });
    }

    public void setSelectorModeManager(SelectorModeManager selectorManager) {
        this.selectorManager = selectorManager;
        notifyItemRangeChanged(0, getItemCount() - 1);
    }

    public SelectorModeManager getSelectorManager() {
        return selectorManager;
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    public boolean onBackPressed() {
        return getSelectorManager().onBackPressed();
    }

    public void saveInstanceState(Bundle outState) {
        getSelectorManager().saveInstanceState(outState);
    }
}
