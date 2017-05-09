package us.koller.cameraroll.imageDecoder;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.davemorrissey.labs.subscaleview.decoder.ImageDecoder;

import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.util.Util;

public class GlideImageDecoder implements ImageDecoder {

    public GlideImageDecoder() {

    }

    @Override
    public Bitmap decode(Context context, Uri uri) throws Exception {
        int[] imageDimens = Util.getImageDimensions(context, uri);
        boolean use8BitColor = Settings.getInstance(context).use8BitColor();
        return Glide.with(context)
                .load(uri)
                .asBitmap()
                .format(use8BitColor ? DecodeFormat.PREFER_ARGB_8888 : DecodeFormat.DEFAULT)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(imageDimens[0], imageDimens[1])
                .get();
    }
}
