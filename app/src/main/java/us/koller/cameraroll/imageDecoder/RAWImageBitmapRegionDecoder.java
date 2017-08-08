package us.koller.cameraroll.imageDecoder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;

import com.davemorrissey.labs.subscaleview.decoder.ImageRegionDecoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

//simple RegionDecoder using BitmapFactory.decodeStream(..., rect,...),
//because BitmapRegionDecoder doesn't support RAW(.dng) images
public class RAWImageBitmapRegionDecoder implements ImageRegionDecoder {

    private static final int JPEG_QUALITY = 90;

    private BitmapRegionDecoder decoder;
    private final Object decoderLock = new Object();
    private boolean ready = false;

    private File cacheFile;

    @Override
    public Point init(Context context, Uri uri) throws Exception {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        options.inJustDecodeBounds = false;

        Bitmap bitmap = new GlideImageDecoder().decode(context, uri);

        String filename = String.valueOf(uri.toString().hashCode());
        cacheFile = new File(context.getCacheDir(), filename);
        FileOutputStream fileOutputStream = new FileOutputStream(cacheFile);
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fileOutputStream);

        FileInputStream fileInputStream = new FileInputStream(cacheFile);
        this.decoder = BitmapRegionDecoder.newInstance(fileInputStream, false);
        Point p = new Point(decoder.getWidth(), decoder.getHeight());

        ready = true;

        return p;
    }

    @Override
    public Bitmap decodeRegion(Rect rect, int sampleSize) {
        synchronized (this.decoderLock) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize;
            Bitmap bitmap = this.decoder.decodeRegion(rect, options);
            if (bitmap == null) {
                throw new RuntimeException("Region decoder returned null bitmap - image format may not be supported");
            } else {
                return bitmap;
            }
        }
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void recycle() {
        decoder.recycle();
        //noinspection ResultOfMethodCallIgnored
        cacheFile.delete();
    }
}
