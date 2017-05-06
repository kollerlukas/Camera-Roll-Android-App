package us.koller.cameraroll.adapter.album;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.michaelflisar.dragselectrecyclerview.DragSelectTouchListener;

import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.SelectorModeManager;
import us.koller.cameraroll.adapter.album.ViewHolder.AlbumItemHolder;
import us.koller.cameraroll.adapter.album.ViewHolder.GifViewHolder;
import us.koller.cameraroll.adapter.album.ViewHolder.PhotoViewHolder;
import us.koller.cameraroll.adapter.album.ViewHolder.VideoViewHolder;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.Gif;
import us.koller.cameraroll.data.Photo;
import us.koller.cameraroll.data.Video;
import us.koller.cameraroll.ui.AlbumActivity;
import us.koller.cameraroll.ui.ItemActivity;
import us.koller.cameraroll.ui.MainActivity;

public class RecyclerViewAdapter extends RecyclerView.Adapter {

    private final int VIEW_TYPE_PHOTO = 1;
    private final int VIEW_TYPE_GIF = 2;
    private final int VIEW_TYPE_VIDEO = 3;

    private Album album;

    private boolean pick_photos;

    private SelectorModeManager manager;

    private Callback callback;

    private DragSelectTouchListener dragSelectTouchListener;

    public RecyclerViewAdapter(Callback callback, final RecyclerView recyclerView,
                               final Album album, boolean pick_photos) {
        this.callback = callback;
        this.album = album;
        this.pick_photos = pick_photos;
        if (pick_photos) {
            setSelectorMode(true);
            if (callback != null) {
                callback.onSelectorModeEnter();
            }
        }
        manager = new SelectorModeManager();

        //disable default change animation
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        if (callback != null && dragSelectEnabled()) {
            dragSelectTouchListener = new DragSelectTouchListener()
                    .withSelectListener(new DragSelectTouchListener.OnDragSelectListener() {
                        @Override
                        public void onSelectChange(int start, int end, boolean isSelected) {
                            for (int i = start; i <= end; i++) {
                                manager.onItemSelect(album.getAlbumItems().get(i).getPath());

                                if (RecyclerViewAdapter.this.callback != null) {
                                    RecyclerViewAdapter.this.callback
                                            .onItemSelected(getSelectedItemCount());
                                    checkForNoSelectedItems();
                                }

                                //update ViewHolder
                                notifyItemChanged(i);
                            }
                        }
                    });
            recyclerView.addOnItemTouchListener(dragSelectTouchListener);
        }
    }

    public RecyclerViewAdapter setSelectorModeManager(SelectorModeManager manager) {
        this.manager = manager;
        return this;
    }

    @Override
    public int getItemViewType(int position) {
        AlbumItem albumItem = album.getAlbumItems().get(position);
        if (albumItem instanceof Photo) {
            return VIEW_TYPE_PHOTO;
        } else if (albumItem instanceof Gif) {
            return VIEW_TYPE_GIF;
        } else {
            return VIEW_TYPE_VIDEO;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.albumitem_cover, parent, false);
        switch (viewType) {
            case VIEW_TYPE_VIDEO:
                return new VideoViewHolder(v);
            case VIEW_TYPE_GIF:
                return new GifViewHolder(v);
            default:
                return new PhotoViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        final AlbumItem albumItem = album.getAlbumItems().get(position);

        ((AlbumItemHolder) holder).setAlbumItem(albumItem);

        boolean selected = manager.isItemSelected(albumItem.getPath());

        ((AlbumItemHolder) holder).setSelected(selected);

        holder.itemView.setTag(albumItem.getPath());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getSelectorMode()) {
                    onItemSelected((AlbumItemHolder) holder);
                } else if ((albumItem instanceof Photo
                        || albumItem instanceof Gif
                        || albumItem instanceof Video) && !albumItem.error) {
                    Intent intent = new Intent(holder.itemView.getContext(), ItemActivity.class);
                    intent.putExtra(ItemActivity.ALBUM_ITEM, albumItem);
                    //intent.putExtra(ItemActivity.ALBUM, getAlbum());
                    intent.putExtra(ItemActivity.ALBUM_PATH, album.getPath());
                    intent.putExtra(ItemActivity.ITEM_POSITION,
                            album.getAlbumItems().indexOf(albumItem));

                    ActivityOptionsCompat options =
                            ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    (Activity) holder.itemView.getContext(),
                                    holder.itemView.findViewById(R.id.image),
                                    albumItem.getPath());
                    ((Activity) holder.itemView.getContext())
                            .startActivityForResult(intent,
                                    MainActivity.REFRESH_PHOTOS_REQUEST_CODE, options.toBundle());
                }
            }
        });

        if (callback != null) {
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (!getSelectorMode()) {
                        setSelectorMode(true);
                        clearSelectedItemsList();

                        //notify AlbumActivity
                        if (callback != null) {
                            callback.onSelectorModeEnter();
                        }
                    }

                    onItemSelected((AlbumItemHolder) holder);

                    if (dragSelectEnabled()) {
                        //notify DragSelectTouchListener
                        boolean selected = manager.isItemSelected(albumItem.getPath());
                        if (selected) {
                            int position = getAlbum().getAlbumItems().indexOf(albumItem);
                            dragSelectTouchListener.startDragSelection(position);
                        }
                    }
                    return true;
                }
            });
        }
    }

    public boolean isSelectorModeActive() {
        return getSelectorMode() && !pick_photos;
    }

    public void restoreSelectedItems() {
        //notify AlbumActivity
        if (callback != null) {
            callback.onSelectorModeEnter();
        }

        for (int i = 0; i < this.album.getAlbumItems().size(); i++) {
            if (manager.isItemSelected(album.getAlbumItems().get(i).getPath())) {
                notifyItemChanged(i);
            }
        }

        if (callback != null) {
            callback.onItemSelected(manager.getSelectedItemCount());
        }
    }

    public void checkForNoSelectedItems() {
        if (getSelectedItemCount() == 0 && !pick_photos) {
            setSelectorMode(false);
            cancelSelectorMode();
        }
    }

    public int getSelectedItemCount() {
        return manager.getSelectedItemCount();
    }

    public void onItemSelected(AlbumItemHolder holder) {
        boolean selected = manager.onItemSelect(holder.albumItem.getPath());
        holder.setSelected(selected);

        if (callback != null) {
            callback.onItemSelected(getSelectedItemCount());
        }
        checkForNoSelectedItems();
    }

    public String[] cancelSelectorMode() {
        setSelectorMode(false);
        //update ui
        for (int i = 0; i < this.album.getAlbumItems().size(); i++) {
            if (manager.isItemSelected(album.getAlbumItems().get(i).getPath())) {
                notifyItemChanged(i);
            }
        }
        //generate paths array
        String[] paths = manager.createStringArray();
        //clear manager list
        clearSelectedItemsList();
        //notify that SelectorMode was exited
        if (callback != null) {
            callback.onSelectorModeExit();
        }
        return paths;
    }

    public boolean onBackPressed() {
        if (getSelectorMode() && !pick_photos) {
            cancelSelectorMode();
            return true;
        }
        return false;
    }

    public boolean getSelectorMode() {
        return manager.isSelectorModeActive();
    }

    public void setSelectorMode(boolean activate) {
        manager.setSelectorMode(activate);
    }

    public boolean dragSelectEnabled() {
        return true;
    }

    public void clearSelectedItemsList() {
        manager.clearList();
    }

    @Override
    public int getItemCount() {
        return getAlbum().getAlbumItems().size();
    }

    public Album getAlbum() {
        return album;
    }

    public Callback getCallback() {
        return callback;
    }

    public SelectorModeManager getManager() {
        return manager;
    }

    public void saveInstanceState(Bundle state) {
        manager.saveInstanceState(state);
    }

    public interface Callback {
        void onSelectorModeEnter();

        void onSelectorModeExit();

        void onItemSelected(int selectedItemCount);
    }
}
