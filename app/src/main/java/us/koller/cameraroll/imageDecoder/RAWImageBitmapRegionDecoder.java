package us.koller.cameraroll.imageDecoder;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;

import com.davemorrissey.labs.subscaleview.decoder.ImageRegionDecoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

//caching converted image as jpg and the using CustomRegionDecoder
public class RAWImageBitmapRegionDecoder implements ImageRegionDecoder {
    private static final int JPEG_QUALITY = 90;
    private ImageRegionDecoder decoder;
    private File cacheFile;

    @Override
    public Point init(Context c, Uri uri) throws Exception {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inSampleSize = 1;
        o.inJustDecodeBounds = false;

        ContentResolver resolver = c.getContentResolver();
        InputStream iS = resolver.openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(iS);

        String fn = String.valueOf(uri.toString().hashCode());
        cacheFile = new File(c.getCacheDir(), fn);
        FileOutputStream fOS = new FileOutputStream(cacheFile);
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fOS);

        decoder = new CustomRegionDecoder();
        return decoder.init(c, Uri.fromFile(cacheFile));
    }

    @Override
    public Bitmap decodeRegion(Rect r, int sampleSize) {
        return decoder.decodeRegion(r, sampleSize);
    }

    @Override
    public boolean isReady() {
        return decoder.isReady();
    }

    @Override
    public void recycle() {
        decoder.recycle();
        //noinspection ResultOfMethodCallIgnored
        cacheFile.delete();
    }
}
