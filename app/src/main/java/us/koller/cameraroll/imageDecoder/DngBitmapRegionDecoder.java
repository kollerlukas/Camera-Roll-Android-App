package us.koller.cameraroll.imageDecoder;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.util.Log;

import com.davemorrissey.labs.subscaleview.decoder.ImageRegionDecoder;

import java.io.InputStream;

import us.koller.cameraroll.util.Util;

//simple RegionDecoder using BitmapFactory.decodeStream(..., rect,...),
//because BitmapRegionDecoder doesn't support RAW(.dng) images
public class DngBitmapRegionDecoder implements ImageRegionDecoder {

    private InputStream inputStream;
    private boolean ready = false;
    private final Object decoderLock = new Object();

    @Override
    public Point init(Context context, Uri uri) throws Exception {
        ContentResolver contentResolver = context.getContentResolver();
        this.inputStream = contentResolver.openInputStream(uri);
        if (inputStream == null) {
            throw new NullPointerException("inputStream = null");
        }

        int[] imageDimens = Util.getImageDimensions(context, uri);

        ready = true;

        return new Point(imageDimens[0], imageDimens[1]);
    }

    @Override
    public Bitmap decodeRegion(Rect rect, int sampleSize) {
        Log.d("DngBitmapRegionDecoder", "decodeRegion() called with: rect = [" + rect + "], sampleSize = [" + sampleSize + "]");
        synchronized (this.decoderLock) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize;
            return BitmapFactory.decodeStream(inputStream, rect, options);
        }
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void recycle() {
        inputStream = null;
    }
}
