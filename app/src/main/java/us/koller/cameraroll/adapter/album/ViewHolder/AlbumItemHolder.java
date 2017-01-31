package us.koller.cameraroll.adapter.album.ViewHolder;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.util.ColorFade;
import us.koller.cameraroll.util.Util;

public abstract class AlbumItemHolder extends RecyclerView.ViewHolder {

    public AlbumItem albumItem;

    AlbumItemHolder(View itemView) {
        super(itemView);
    }

    public void setAlbumItem(AlbumItem albumItem) {
        this.albumItem = albumItem;
        ImageView imageView = (ImageView) itemView.findViewById(R.id.image);
        loadImage(imageView, albumItem);
    }

    public int[] loadImage(final ImageView imageView, final AlbumItem albumItem) {

        final Context context = itemView.getContext();

        int screenWidth = Util.getScreenWidth((Activity) context);
        int columnCount = Util.getAlbumActivityGridColumnCount(context);
        //square image
        int[] imageDimens = {
                (int) ((float) screenWidth / columnCount),
                (int) ((float) screenWidth / columnCount)};

        return imageDimens;
    }

    void fadeIn() {
        albumItem.hasFadedIn = true;
        ColorFade.fadeSaturation((ImageView) itemView.findViewById(R.id.image));
    }
}
