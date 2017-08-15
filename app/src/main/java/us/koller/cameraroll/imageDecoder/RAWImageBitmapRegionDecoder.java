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
    public Point init(Context context, Uri uri) throws Exception {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        options.inJustDecodeBounds = false;

        ContentResolver resolver = context.getContentResolver();
        InputStream inputStream = resolver.openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

        String filename = String.valueOf(uri.toString().hashCode());
        cacheFile = new File(context.getCacheDir(), filename);
        FileOutputStream fileOutputStream = new FileOutputStream(cacheFile);
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fileOutputStream);

        decoder = new CustomRegionDecoder();
        return decoder.init(context, Uri.fromFile(cacheFile));
    }

    @Override
    public Bitmap decodeRegion(Rect rect, int sampleSize) {
        return decoder.decodeRegion(rect, sampleSize);
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
