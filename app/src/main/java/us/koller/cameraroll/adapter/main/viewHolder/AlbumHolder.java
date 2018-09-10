package us.koller.cameraroll.adapter.main.viewHolder;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.io.File;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.models.Album;
import us.koller.cameraroll.data.models.AlbumItem;
import us.koller.cameraroll.data.models.VirtualAlbum;
import us.koller.cameraroll.data.provider.MediaProvider;
import us.koller.cameraroll.ui.widget.ParallaxImageView;
import us.koller.cameraroll.util.animators.ColorFade;

public abstract class AlbumHolder extends RecyclerView.ViewHolder {
    private Album album;

    AlbumHolder(View itemView) {
        super(itemView);
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        if (album == null) {
            //Error album
            album = MediaProvider.getErrorAlbum();
        }

        this.album = album;

        TextView nameTv = itemView.findViewById(R.id.name);
        nameTv.setText(album.getName());
        //to fix ellipsize
        nameTv.requestLayout();
        //pinned indicator
        /*Drawable pinIndicator = null;
        if (album.pinned) {
            pinIndicator = AppCompatResources
                    .getDrawable(getContext(), R.drawable.pin_indicator);
            if (pinIndicator != null) {
                int color = nameTv.getTextColors().getDefaultColor();
                DrawableCompat.wrap(pinIndicator);
                DrawableCompat.setTint(pinIndicator, color);
                DrawableCompat.unwrap(pinIndicator);
            }
        }
        nameTv.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null, null, pinIndicator, null);*/

        //set album itemCount
        int itemCount = album.getAlbumItems().size();
        boolean oneItem = itemCount == 1;
        String count = getContext().getString(oneItem ?
                R.string.item_count : R.string.items_count, itemCount);
        ((TextView) itemView.findViewById(R.id.count)).setText(Html.fromHtml(count));

        ImageView hiddenFolderIndicator = itemView.findViewById(R.id.hidden_folder_indicator);
        if (hiddenFolderIndicator != null) {
            hiddenFolderIndicator
                    .setVisibility(album.isHidden() ? View.VISIBLE : View.GONE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !(album instanceof VirtualAlbum)) {
            ImageView removableStorageIndicator = itemView.findViewById(R.id.removable_storage_indicator);
            if (removableStorageIndicator != null) {
                try {
                    boolean removable = Environment.isExternalStorageRemovable(new File(album.getPath()));
                    removableStorageIndicator.setVisibility(removable ? View.VISIBLE : View.GONE);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void loadImage(final ImageView image) {
        if (album.getAlbumItems().size() == 0) {
            Glide.with(getContext())
                    .load(R.drawable.error_placeholder)
                    .apply(new RequestOptions().skipMemoryCache(true))
                    .into(image);
            return;
        }

        final AlbumItem coverImage = album.getAlbumItems().get(0);
        Glide.with(getContext())
                .asBitmap()
                .load(coverImage.getPath())
                .listener(new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Bitmap> target, boolean isFirstResource) {
                        coverImage.error = true;
                        if (image instanceof ParallaxImageView) {
                            ((ParallaxImageView) image).setParallaxTranslation();
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target,
                                                   DataSource dataSource, boolean isFirstResource) {
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
                .apply(coverImage.getGlideRequestOptions(getContext()))
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
