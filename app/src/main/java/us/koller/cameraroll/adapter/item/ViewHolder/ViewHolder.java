package us.koller.cameraroll.adapter.item.ViewHolder;

import android.view.View;
import android.view.ViewGroup;

import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.ui.ItemActivity;
import us.koller.cameraroll.util.ItemViewUtil;

public abstract class ViewHolder {

    View itemView;
    public AlbumItem albumItem;
    private int position;

    public ViewHolder(AlbumItem albumItem, int position) {
        this.albumItem = albumItem;
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    ViewGroup inflatePhotoView(ViewGroup container) {
        ViewGroup v = ItemViewUtil.inflatePhotoView(container);
        v.setTag(albumItem.getPath());
        this.itemView = v;
        return v;
    }

    ViewGroup inflateVideoView(ViewGroup container) {
        ViewGroup v = ItemViewUtil.inflateVideoView(container);
        v.setTag(albumItem.getPath());
        this.itemView = v;
        return v;
    }

    boolean imageOnClick(View view) {
        try {
            return ((ItemActivity) view.getContext()).imageOnClick();
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        return true;
    }

    void imageOnClick(View view, boolean show) {
        try {
            ((ItemActivity) view.getContext()).imageOnClick(show);
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }

    public abstract View getView(ViewGroup container);

    public void onDestroy() {
        this.itemView.setOnClickListener(null);
        this.itemView = null;
        this.albumItem = null;
    }

    public String getTag() {
        return albumItem.getPath();
    }

    public abstract void onSharedElement(ItemActivity.Callback callback);


}
