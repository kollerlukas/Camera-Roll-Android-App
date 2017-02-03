package us.koller.cameraroll.data.MediaLoader;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import java.util.ArrayList;

import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.MediaLoader.Loader.MediaStoreLoader;
import us.koller.cameraroll.data.MediaLoader.Loader.StorageLoader;
import us.koller.cameraroll.ui.MainActivity;

public class MediaLoader {

    public interface Loader {
        void loadAlbums(final Activity context, final boolean hiddenFolders, final LoaderCallback callback);

        void onDestroy();
    }

    public interface LoaderCallback {
        void onMediaLoaded(ArrayList<Album> albums);
        void timeout();
    }

    public interface Callback {
        void callback(ArrayList<Album> albums);
    }

    public static final int MODE_STORAGE = 1;
    public static final int MODE_MEDIASTORE = 2;
    private static final String MODE_KEY = "MODE_KEY";

    public static final String FILE_TYPE_NO_MEDIA = ".nomedia";
    public static final int PERMISSION_REQUEST_CODE = 16;

    private Loader loader;

    public MediaLoader() {
        loader = new StorageLoader();
    }

    public static boolean checkPermission(Activity context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(context,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    public void loadAlbums(Activity context,
                           boolean hiddenFolders,
                           LoaderCallback callback) {

        if (!MediaLoader.checkPermission(context)) {
            return;
        }

        int mode = getMode(context);

        switch (mode) {
            case MODE_STORAGE:
                loader = new StorageLoader();
                break;
            case MODE_MEDIASTORE:
                loader = new MediaStoreLoader();
                break;
        }

        if (loader != null) {
            loader.loadAlbums(context, hiddenFolders, callback);
        } else {
            callback.onMediaLoaded(null);
        }
    }

    public void onDestroy() {
        loader.onDestroy();
    }

    private static int getMode(Context context) {
        return context.getSharedPreferences(MainActivity.SHARED_PREF_NAME, Context.MODE_PRIVATE)
                .getInt(MODE_KEY, MODE_MEDIASTORE);
    }

    public static void toggleMode(Context context) {
        int mode = getMode(context);

        int newMode = mode == MODE_STORAGE ? MODE_MEDIASTORE : MODE_STORAGE;

        context.getSharedPreferences(MainActivity.SHARED_PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(MODE_KEY, newMode)
                .apply();

        String s = newMode == MODE_STORAGE ? "MODE_STORAGE" : "MODE_MEDIASTORE";

        Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }
}