package us.koller.cameraroll.imageDecoder;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.davemorrissey.labs.subscaleview.decoder.ImageDecoder;

import java.io.InputStream;

import us.koller.cameraroll.data.Settings;

//simple ImageDecoder to have control over Bitmap.Config
public class CustomImageDecoder implements ImageDecoder {

    @Override
    public Bitmap decode(Context context, Uri uri) throws Exception {
        boolean use8BitColor = Settings.getInstance(context).use8BitColor();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = use8BitColor ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;

        ContentResolver resolver = context.getContentResolver();
        InputStream inputStream = resolver.openInputStream(uri);
        return BitmapFactory.decodeStream(inputStream, null, options);
    }
}
