package us.koller.cameraroll.adapter.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.AbstractRecyclerViewAdapter;
import us.koller.cameraroll.adapter.SelectorModeManager;
import us.koller.cameraroll.adapter.main.viewHolder.AlbumHolder;
import us.koller.cameraroll.adapter.main.viewHolder.NestedRecyclerViewAlbumHolder;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.data.models.Album;
import us.koller.cameraroll.styles.Style;
import us.koller.cameraroll.themes.Theme;
import us.koller.cameraroll.ui.AlbumActivity;
import us.koller.cameraroll.ui.MainActivity;
import us.koller.cameraroll.ui.ThemeableActivity;

public class MainAdapter extends AbstractRecyclerViewAdapter<ArrayList<Album>> {

    private Style style;

    public MainAdapter(Context context, boolean pick_photos) {
        super(pick_photos);
        Settings settings = Settings.getInstance(context);
        style = settings.getStyleInstance(context, pick_photos);
        setSelectorModeManager(new SelectorModeManager());
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder vH;

        vH = style.createViewHolderInstance(parent);
        if (vH instanceof NestedRecyclerViewAlbumHolder) {
            ((NestedRecyclerViewAlbumHolder) vH).setSelectorModeManager(getSelectorManager());
        }

        Context c = vH.itemView.getContext();
        Theme theme = Settings.getInstance(c).getThemeInstance(c);
        ThemeableActivity.checkTags((ViewGroup) vH.itemView, theme);
        return vH;
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
        final Album album = getData().get(position);

        ((AlbumHolder) holder).setAlbum(album);

        holder.itemView.setOnClickListener((View v) -> {
            Intent intent = new Intent(holder.itemView.getContext(), AlbumActivity.class);

            //intent.putExtra(AlbumActivity.ALBUM, album);
            intent.putExtra(AlbumActivity.ALBUM_PATH, album.getPath());

            if (pickPhotos()) {
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
            Activity c = (Activity) holder.itemView.getContext();
            if (!pickPhotos()) {
                //noinspection unchecked
                options = ActivityOptionsCompat.makeSceneTransitionAnimation(c);
                c.startActivityForResult(intent,
                        MainActivity.REFRESH_PHOTOS_REQUEST_CODE, options.toBundle());
            } else {
                View toolbar = c.findViewById(R.id.toolbar);
                options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        c, toolbar, c.getString(R.string.toolbar_transition_name));
                c.startActivityForResult(intent,
                        MainActivity.PICK_PHOTOS_REQUEST_CODE, options.toBundle());
            }
        });
    }

    @Override
    public int getItemCount() {
        return getData() != null ? getData().size() : 0;
    }

    public boolean onBackPressed() {
        return getSelectorManager().onBackPressed();
    }

    @Override
    public void setSelectorModeManager(SelectorModeManager sM) {
        super.setSelectorModeManager(sM);
        notifyItemRangeChanged(0, getItemCount());
    }
}
