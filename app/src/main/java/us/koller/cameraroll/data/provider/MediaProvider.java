package us.koller.cameraroll.data.provider;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;

import us.koller.cameraroll.data.models.Album;
import us.koller.cameraroll.data.models.AlbumItem;
import us.koller.cameraroll.data.provider.retriever.MediaStoreRetriever;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.data.provider.retriever.StorageRetriever;
import us.koller.cameraroll.util.SortUtil;

public class MediaProvider extends Provider {

    private static ArrayList<Album> albums;

    private static final int MODE_STORAGE = 1;
    private static final int MODE_MEDIASTORE = 2;

    public static final String FILE_TYPE_NO_MEDIA = ".nomedia";
    public static final int PERMISSION_REQUEST_CODE = 16;

    public static boolean dataChanged = false;

    public abstract static class OnMediaLoadedCallback implements Provider.Callback {
        public abstract void onMediaLoaded(ArrayList<Album> albums);
    }

    public interface OnAlbumLoadedCallback {
        void onAlbumLoaded(Album album);
    }

    public MediaProvider(Context context) {
        super(context);
    }

    public static boolean checkPermission(Activity context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
            int write = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (read != PackageManager.PERMISSION_GRANTED && write != PackageManager.PERMISSION_GRANTED) {
                String[] requestedPermissions = new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(context, requestedPermissions, PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    public void loadAlbums(final Activity context,
                           final boolean hiddenFolders,
                           OnMediaLoadedCallback callback) {

        if (!MediaProvider.checkPermission(context)) {
            callback.needPermission();
            return;
        }

        int mode = getMode(context);

        switch (mode) {
            case MODE_STORAGE:
                retriever = new StorageRetriever();
                break;
            case MODE_MEDIASTORE:
                retriever = new MediaStoreRetriever();
                break;
            default:
                break;
        }

        if (retriever != null) {
            setCallback(callback);

            retriever.loadAlbums(context, hiddenFolders,
                    new OnMediaLoadedCallback() {
                        @Override
                        public void onMediaLoaded(ArrayList<Album> albums) {
                            //if (!hiddenFolders) {
                                //remove excluded albums
                                for (int i = albums.size() - 1; i >= 0; i--) {
                                    if (albums.get(i).excluded) {
                                        albums.remove(i);
                                    }
                                }
                            //}

                            SortUtil.sortAlbums(context, albums);

                            setAlbums(albums);
                            OnMediaLoadedCallback callback = getCallback();
                            if (callback != null) {
                                callback.onMediaLoaded(albums);
                            }
                        }

                        @Override
                        public void timeout() {
                            OnMediaLoadedCallback callback = getCallback();
                            if (callback != null) {
                                callback.timeout();
                            }
                        }

                        @Override
                        public void needPermission() {
                            OnMediaLoadedCallback callback = getCallback();
                            if (callback != null) {
                                callback.needPermission();
                            }
                        }
                    });
        } else {
            if (callback != null) {
                callback.onMediaLoaded(null);
            }
        }
    }

    private static void setAlbums(ArrayList<Album> albums) {
        MediaProvider.albums = albums;
    }

    public static ArrayList<Album> getAlbums() {
        return albums;
    }

    public static void loadAlbum(final Activity context, final String path,
                                 final OnAlbumLoadedCallback callback) {
        if (albums == null) {
            Settings s = Settings.getInstance(context);
            boolean hiddenFolders = s.getHiddenFolders();
            new MediaProvider(context).loadAlbums(context, hiddenFolders, new OnMediaLoadedCallback() {
                @Override
                public void onMediaLoaded(ArrayList<Album> albums) {
                    loadAlbum(context, path, callback);
                }

                @Override
                public void timeout() {
                    callback.onAlbumLoaded(getErrorAlbum());
                }

                @Override
                public void needPermission() {
                    callback.onAlbumLoaded(getErrorAlbum());
                }
            });
        } else {
            for (int i = 0; i < albums.size(); i++) {
                if (albums.get(i).getPath().equals(path)) {
                    final Album album = albums.get(i);
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onAlbumLoaded(album);
                        }
                    });
                }
            }
        }
    }

    public static Album getErrorAlbum() {
        //Error album
        Album album = new Album().setPath("ERROR");
        album.getAlbumItems().add(AlbumItem.getErrorItem());
        return album;
    }

    private static int getMode(Context context) {
        return Settings.getInstance(context).useStorageRetriever() ?
                MODE_STORAGE : MODE_MEDIASTORE;
    }
}