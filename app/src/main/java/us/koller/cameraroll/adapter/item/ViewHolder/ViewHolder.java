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

    ViewGroup inflateView(ViewGroup container) {
        ViewGroup v = ItemViewUtil.inflateView(container);
        v.setTag(albumItem.getPath());
        this.itemView = v;
        return v;
    }

    void imageOnClick(View view) {
        try {
            ((ItemActivity) view.getContext()).imageOnClick();
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
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
