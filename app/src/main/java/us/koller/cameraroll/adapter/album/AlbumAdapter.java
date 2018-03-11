package us.koller.cameraroll.adapter.album;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.michaelflisar.dragselectrecyclerview.DragSelectTouchListener;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.AbstractRecyclerViewAdapter;
import us.koller.cameraroll.adapter.SelectorModeManager;
import us.koller.cameraroll.adapter.album.viewHolder.AlbumItemHolder;
import us.koller.cameraroll.adapter.album.viewHolder.GifViewHolder;
import us.koller.cameraroll.adapter.album.viewHolder.PhotoViewHolder;
import us.koller.cameraroll.adapter.album.viewHolder.RAWImageHolder;
import us.koller.cameraroll.adapter.album.viewHolder.VideoViewHolder;
import us.koller.cameraroll.data.models.Album;
import us.koller.cameraroll.data.models.AlbumItem;
import us.koller.cameraroll.data.models.Gif;
import us.koller.cameraroll.data.models.Photo;
import us.koller.cameraroll.data.models.RAWImage;
import us.koller.cameraroll.data.models.Video;
import us.koller.cameraroll.ui.ItemActivity;

public class AlbumAdapter extends AbstractRecyclerViewAdapter<Album> {

    @SuppressWarnings("FieldCanBeLocal")
    private final int VIEW_TYPE_PHOTO = 1;
    private final int VIEW_TYPE_GIF = 2;
    private final int VIEW_TYPE_VIDEO = 3;
    private final int VIEW_TYPE_RAW = 4;

    private DragSelectTouchListener dragSelectTouchListener;

    public AlbumAdapter(SelectorModeManager.Callback callback, final RecyclerView recyclerView,
                        final Album album, boolean pick_photos) {
        super(pick_photos);

        setData(album);
        setSelectorModeManager(new SelectorModeManager());
        if (callback != null) {
            getSelectorManager().addCallback(callback);
        }

        if (pick_photos) {
            getSelectorManager().setSelectorMode(true);
            if (callback != null) {
                callback.onSelectorModeEnter();
            }
        }

        //disable default change animation
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        if (callback != null && dragSelectEnabled()) {
            dragSelectTouchListener = new DragSelectTouchListener()
                    .withSelectListener(new DragSelectTouchListener.OnDragSelectListener() {
                        @Override
                        public void onSelectChange(int start, int end, boolean isSelected) {
                            for (int i = start; i <= end; i++) {
                                getSelectorManager().onItemSelect(getData()
                                        .getAlbumItems().get(i).getPath());
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
        AlbumItem albumItem = getData().getAlbumItems().get(position);
        if (albumItem instanceof RAWImage) {
            return VIEW_TYPE_RAW;
        } else if (albumItem instanceof Gif) {
            return VIEW_TYPE_GIF;
        } else if (albumItem instanceof Photo) {
            return VIEW_TYPE_PHOTO;
        } else if (albumItem instanceof Video) {
            return VIEW_TYPE_VIDEO;
        }
        return -1;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return onCreateViewHolder(parent, viewType, R.layout.albumitem_cover);
    }

    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType, int layoutRes) {
        View v = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        switch (viewType) {
            case VIEW_TYPE_RAW:
                return new RAWImageHolder(v);
            case VIEW_TYPE_GIF:
                return new GifViewHolder(v);
            case VIEW_TYPE_VIDEO:
                return new VideoViewHolder(v);
            case VIEW_TYPE_PHOTO:
                return new PhotoViewHolder(v);
            default:
                break;
        }
        return null;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        final AlbumItem albumItem = getData().getAlbumItems().get(position);

        if (!albumItem.equals(((AlbumItemHolder) holder).getAlbumItem())) {
            ((AlbumItemHolder) holder).setAlbumItem(albumItem);
        }

        boolean selected = getSelectorManager().isItemSelected(albumItem.getPath());

        ((AlbumItemHolder) holder).setSelected(selected);

        holder.itemView.setTag(albumItem.getPath());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getSelectorMode()) {
                    onItemSelected((AlbumItemHolder) holder);
                } else {
                    Log.d("AlbumAdapter", "onClick: " + getData().getPath());
                    Context context = holder.itemView.getContext();
                    Intent intent = new Intent(context, ItemActivity.class);
                    intent.putExtra(ItemActivity.ALBUM_ITEM, albumItem);
                    intent.putExtra(ItemActivity.ALBUM_PATH, getData().getPath());
                    intent.putExtra(ItemActivity.ITEM_POSITION, getData().getAlbumItems().indexOf(albumItem));

                    ActivityOptionsCompat options =
                            ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    (Activity) context, holder.itemView.findViewById(R.id.image),
                                    albumItem.getPath());
                    ActivityCompat.startActivityForResult((Activity) context, intent,
                            ItemActivity.VIEW_IMAGE, options.toBundle());
                }
            }
        });

        if (getSelectorManager().callbacksAttached()) {
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (!getSelectorMode()) {
                        setSelectorMode(true);
                        clearSelectedItemsList();
                    }

                    onItemSelected((AlbumItemHolder) holder);

                    if (dragSelectEnabled()) {
                        //notify DragSelectTouchListener
                        boolean selected = getSelectorManager().isItemSelected(albumItem.getPath());
                        if (selected) {
                            int position = getData().getAlbumItems().indexOf(albumItem);
                            dragSelectTouchListener.startDragSelection(position);
                        }
                    }
                    return true;
                }
            });
        }
    }

    public boolean isSelectorModeActive() {
        return getSelectorMode() && !pickPhotos();
    }

    public void restoreSelectedItems() {
        //notify AlbumActivity
        getSelectorManager().onSelectorModeEnter();

        for (int i = 0; i < getData().getAlbumItems().size(); i++) {
            if (getSelectorManager().isItemSelected(getData().getAlbumItems().get(i).getPath())) {
                notifyItemChanged(i);
            }
        }

        getSelectorManager().onItemSelected(getSelectorManager().getSelectedItemCount());
    }

    private void checkForNoSelectedItems() {
        if (getSelectedItemCount() == 0 && !pickPhotos()) {
            cancelSelectorMode(null);
        }
    }

    private int getSelectedItemCount() {
        return getSelectorManager().getSelectedItemCount();
    }

    private void onItemSelected(AlbumItemHolder holder) {
        boolean selected = getSelectorManager().onItemSelect(holder.albumItem.getPath());
        holder.setSelected(selected);
        checkForNoSelectedItems();
    }

    public String[] cancelSelectorMode(Activity context) {
        setSelectorMode(false);
        //update ui
        for (int i = 0; i < getData().getAlbumItems().size(); i++) {
            if (getSelectorManager().isItemSelected(getData().getAlbumItems().get(i).getPath())) {
                notifyItemChanged(i);
            }
        }
        //generate paths array
        String[] paths;
        if (context != null) {
            paths = getSelectorManager().createStringArray(context);
        } else {
            paths = null;
        }
        //clear manager list
        clearSelectedItemsList();
        return paths;
    }

    public boolean onBackPressed() {
        if (getSelectorMode() && !pickPhotos()) {
            cancelSelectorMode(null);
            return true;
        }
        return false;
    }

    private boolean getSelectorMode() {
        return getSelectorManager().isSelectorModeActive();
    }

    private void setSelectorMode(boolean activate) {
        getSelectorManager().setSelectorMode(activate);
    }

    public boolean dragSelectEnabled() {
        return true;
    }

    private void clearSelectedItemsList() {
        getSelectorManager().clearList();
    }

    @Override
    public int getItemCount() {
        return getData() != null ? getData().getAlbumItems().size() : 0;
    }
}