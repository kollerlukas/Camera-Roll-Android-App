package us.koller.cameraroll.adapter.item;

import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import us.koller.cameraroll.adapter.item.ViewHolder.GifViewHolder;
import us.koller.cameraroll.adapter.item.ViewHolder.PhotoViewHolder;
import us.koller.cameraroll.adapter.item.ViewHolder.RAWImageViewHolder;
import us.koller.cameraroll.adapter.item.ViewHolder.VideoViewHolder;
import us.koller.cameraroll.adapter.item.ViewHolder.ViewHolder;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.Gif;
import us.koller.cameraroll.data.RAWImage;
import us.koller.cameraroll.data.Video;
import us.koller.cameraroll.ui.ItemActivity;

public class ViewPagerAdapter extends PagerAdapter {

    private Album album;

    private ArrayList<ViewHolder> viewHolders;

    private ItemActivity.ViewPagerOnInstantiateItemCallback callback;

    public ViewPagerAdapter(Album album) {
        this.album = album;
        this.viewHolders = new ArrayList<>();
    }

    public void addOnInstantiateItemCallback(
            ItemActivity.ViewPagerOnInstantiateItemCallback callback) {
        this.callback = callback;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    @Override
    public int getCount() {
        return album.getAlbumItems().size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(final ViewGroup container, int position) {
        AlbumItem albumItem = album.getAlbumItems().get(position);

        ViewHolder viewHolder;
        if (albumItem instanceof Video) {
            viewHolder = new VideoViewHolder(albumItem, position);
        } else if (albumItem instanceof Gif) {
            viewHolder = new GifViewHolder(albumItem, position);
        } else if (albumItem instanceof RAWImage) {
            viewHolder = new RAWImageViewHolder(albumItem, position);
        } else {
            viewHolder = new PhotoViewHolder(albumItem, position);
        }
        viewHolders.add(viewHolder);

        View v = viewHolder.getView(container);
        container.addView(v);

        if (callback != null) {
            boolean b = callback.onInstantiateItem(viewHolder);
            if (!b) {
                callback = null;
            }
        }

        return v;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
        ViewHolder viewHolder = findViewHolderByPosition(position);
        if (viewHolder != null) {
            viewHolder.onDestroy();
            viewHolders.remove(viewHolder);
        }
    }

    private ViewHolder findViewHolderByPosition(int position) {
        for (int i = 0; i < viewHolders.size(); i++) {
            if (position == viewHolders.get(i).getPosition()) {
                return viewHolders.get(i);
            }
        }
        return null;
    }

    public ViewHolder findViewHolderByTag(String tag) {
        for (int i = 0; i < viewHolders.size(); i++) {
            if (viewHolders.get(i).getTag().equals(tag)) {
                return viewHolders.get(i);
            }
        }
        return null;
    }

    //for deleting items from the list
    @Override
    public int getItemPosition(Object object) {
        return PagerAdapter.POSITION_NONE;
    }
}
