package us.koller.cameraroll.adapter.album;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.michaelflisar.dragselectrecyclerview.DragSelectTouchListener;

import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.album.ViewHolder.AlbumItemHolder;
import us.koller.cameraroll.adapter.album.ViewHolder.GifViewHolder;
import us.koller.cameraroll.adapter.album.ViewHolder.PhotoViewHolder;
import us.koller.cameraroll.adapter.album.ViewHolder.VideoViewHolder;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.Gif;
import us.koller.cameraroll.data.Photo;
import us.koller.cameraroll.data.Video;
import us.koller.cameraroll.ui.ItemActivity;
import us.koller.cameraroll.ui.MainActivity;

public class RecyclerViewAdapter extends RecyclerView.Adapter {

    private final int VIEW_TYPE_PHOTO = 1;
    private final int VIEW_TYPE_GIF = 2;
    private final int VIEW_TYPE_VIDEO = 3;

    private Album album;

    private boolean selector_mode = false;
    private boolean pick_photos;

    private boolean[] selected_items;

    private Callback callback;

    private DragSelectTouchListener dragSelectTouchListener;

    public RecyclerViewAdapter(Callback callback, final RecyclerView recyclerView,
                               Album album, boolean pick_photos) {
        this.callback = callback;
        this.album = album;
        this.pick_photos = pick_photos;
        if (pick_photos) {
            selector_mode = true;
            if (callback != null) {
                callback.onSelectorModeEnter();
            }
        }
        selected_items = new boolean[album.getAlbumItems().size()];

        //disable default change animation
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        if (callback != null) {
            dragSelectTouchListener = new DragSelectTouchListener()
                    .withSelectListener(new DragSelectTouchListener.OnDragSelectListener() {
                        @Override
                        public void onSelectChange(int start, int end, boolean isSelected) {
                            for (int i = start; i <= end; i++) {
                                selected_items[i] = isSelected;

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

        boolean selected = selected_items[album.getAlbumItems()
                .indexOf(albumItem)];

        ((AlbumItemHolder) holder).setSelected(selected);

        holder.itemView.setTag(albumItem.getPath());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selector_mode) {
                    onItemSelected((AlbumItemHolder) holder);
                } else if ((albumItem instanceof Photo
                        || albumItem instanceof Gif
                        || albumItem instanceof Video) && !albumItem.error) {
                    Intent intent = new Intent(holder.itemView.getContext(), ItemActivity.class);
                    intent.putExtra(ItemActivity.ALBUM_ITEM, albumItem);
                    //intent.putExtra(ItemActivity.ALBUM, album);
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
                    if (!selector_mode) {
                        selector_mode = true;
                        selected_items = new boolean[album.getAlbumItems().size()];

                        //notify AlbumActivity
                        if (callback != null) {
                            callback.onSelectorModeEnter();
                        }
                    }

                    onItemSelected((AlbumItemHolder) holder);

                    //notify DragSelectTouchListener
                    if (selected_items[album.getAlbumItems()
                            .indexOf(((AlbumItemHolder) holder).albumItem)]) {
                        int position = album.getAlbumItems().indexOf(albumItem);
                        dragSelectTouchListener.startDragSelection(position);
                    }
                    return true;
                }
            });
        }
    }

    public boolean isSelectorModeActive() {
        return selector_mode && !pick_photos;
    }

    public void restoreSelectedItems(int[] selectedItemsPos) {
        selector_mode = true;

        //notify AlbumActivity
        if (callback != null) {
            callback.onSelectorModeEnter();
        }

        selected_items = new boolean[album.getAlbumItems().size()];
        for (int i = 0; i < selectedItemsPos.length; i++) {
            int pos = selectedItemsPos[i];
            selected_items[pos] = true;
            notifyItemChanged(pos);
        }

        if (callback != null) {
            callback.onItemSelected(selectedItemsPos.length);
        }
    }

    public int[] getSelectedItemsPositions() {
        if (!selector_mode && !pick_photos) {
            return null;
        }

        int[] selectedItemsPos = new int[getSelectedItemCount()];
        int index = 0;
        for (int i = 0; i < selected_items.length; i++) {
            if (selected_items[i]) {
                selectedItemsPos[index] = i;
                index++;
            }
        }

        return selectedItemsPos;
    }

    private void checkForNoSelectedItems() {
        if (getSelectedItemCount() == 0 && !pick_photos) {
            selector_mode = false;
            cancelSelectorMode();
        }
    }

    private int getSelectedItemCount() {
        int k = 0;
        for (int i = 0; i < selected_items.length; i++) {
            if (selected_items[i]) {
                k++;
            }
        }
        return k;
    }

    private void onItemSelected(AlbumItemHolder holder) {
        int index = album.getAlbumItems().indexOf(holder.albumItem);
        boolean selected = !selected_items[index];
        selected_items[index] = selected;
        holder.setSelected(selected);

        if (callback != null) {
            callback.onItemSelected(getSelectedItemCount());
        }
        checkForNoSelectedItems();
    }

    public AlbumItem[] cancelSelectorMode() {
        ArrayList<AlbumItem> selected_items = new ArrayList<>();
        selector_mode = false;
        for (int i = 0; i < this.selected_items.length; i++) {
            if (this.selected_items[i]) {
                notifyItemChanged(i);
                selected_items.add(album.getAlbumItems().get(i));
            }
        }
        this.selected_items = new boolean[album.getAlbumItems().size()];
        AlbumItem[] arr = new AlbumItem[selected_items.size()];
        if (callback != null) {
            callback.onSelectorModeExit();
        }
        return selected_items.toArray(arr);
    }

    public boolean onBackPressed() {
        if (selector_mode && !pick_photos) {
            cancelSelectorMode();
            return true;
        }
        return false;
    }

    @Override
    public int getItemCount() {
        return album.getAlbumItems().size();
    }

    public interface Callback {
        void onSelectorModeEnter();

        void onSelectorModeExit();

        void onItemSelected(int selectedItemCount);
    }
}
