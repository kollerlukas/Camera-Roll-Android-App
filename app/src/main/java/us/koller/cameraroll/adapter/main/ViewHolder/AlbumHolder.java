package us.koller.cameraroll.adapter.main.ViewHolder;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.Provider.MediaProvider;
import us.koller.cameraroll.data.Provider.Provider;
import us.koller.cameraroll.ui.widget.ParallaxImageView;
import us.koller.cameraroll.util.animators.ColorFade;

public abstract class AlbumHolder extends RecyclerView.ViewHolder {

    private Album album;
    boolean excluded;

    AlbumHolder(View itemView) {
        super(itemView);
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;

        if (album == null) {
            //Error album
            album = MediaProvider.getErrorAlbum();
        }

        ((TextView) itemView.findViewById(R.id.name)).setText(album.getName());

        try {
            Provider.loadExcludedPaths(getContext());
            excluded = Provider.isDirExcluded(album.getPath(), MediaProvider.getExcludedPaths())
                    || Provider.isDirExcludedBecauseParentDirIsExcluded(album.getPath(), Provider.getExcludedPaths());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            excluded = false;
        }

        ImageView hiddenFolderIndicator = (ImageView)
                itemView.findViewById(R.id.hidden_folder_indicator);
        if (hiddenFolderIndicator != null) {
            hiddenFolderIndicator
                    .setVisibility(album.isHidden() ? View.VISIBLE : View.GONE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ImageView removableStorageIndicator = (ImageView)
                    itemView.findViewById(R.id.removable_storage_indicator);
            if (removableStorageIndicator != null) {
                boolean removable = Environment
                        .isExternalStorageRemovable(new File(album.getPath()));
                removableStorageIndicator
                        .setVisibility(removable ? View.VISIBLE : View.GONE);
            }
        }
    }

    void loadImage(final ImageView image) {
        if (album.getAlbumItems().size() == 0) {
            Glide.with(getContext())
                    .load(R.drawable.error_placeholder_tinted)
                    .into(image);
            return;
        }

        final AlbumItem coverImage = album.getAlbumItems().get(0);

        Glide.with(getContext())
                .load(coverImage.getPath())
                .asBitmap()
                .error(R.drawable.error_placeholder_tinted)
                .listener(new RequestListener<String, Bitmap>() {
                    @Override
                    public boolean onException(Exception e, String model,
                                               Target<Bitmap> target, boolean isFirstResource) {
                        coverImage.error = true;

                        if (image instanceof ParallaxImageView) {
                            ((ParallaxImageView) image).setParallaxTranslation();
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, String model,
                                                   Target<Bitmap> target, boolean isFromMemoryCache,
                                                   boolean isFirstResource) {
                        if (!coverImage.hasFadedIn) {
                            coverImage.hasFadedIn = true;
                            ColorFade.fadeSaturation(image);
                        }

                        if (image instanceof ParallaxImageView) {
                            ((ParallaxImageView) image).setParallaxTranslation();
                        }
                        return false;
                    }

                })
                .into(image);
    }

    public void onItemChanged() {

    }

    public Context getContext() {
        if (itemView == null) {
            return null;
        }
        return itemView.getContext();
    }
}
