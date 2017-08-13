package us.koller.cameraroll.data;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;

public class ContentObserver extends android.database.ContentObserver {

    private static final Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

    public static boolean selfChange = false;

    private Listener listener;

    public interface Listener {
        void onChange(boolean selfChange, Uri uri);
    }

    public ContentObserver(Handler handler) {
        super(handler);
    }

    @Override
    public void onChange(boolean selfChange) {
        onChange(selfChange, null);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        if (listener != null && !ContentObserver.selfChange) {
            listener.onChange(selfChange, uri);
        }
    }

    public void unregister(Context context) {
        ContentResolver resolver = context.getContentResolver();
        resolver.unregisterContentObserver(this);
    }

    public void register(Context context) {
        ContentResolver resolver = context.getContentResolver();
        resolver.registerContentObserver(uri, false, this);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }
}
