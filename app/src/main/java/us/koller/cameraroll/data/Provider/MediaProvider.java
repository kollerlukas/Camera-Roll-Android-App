package us.koller.cameraroll.data.Provider;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;

import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.Provider.Retriever.MediaStoreRetriever;
import us.koller.cameraroll.data.Provider.Retriever.StorageRetriever;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.util.SortUtil;

public class MediaProvider extends Provider {

    private static ArrayList<Album> albums;

    public interface Callback {
        void onMediaLoaded(ArrayList<Album> albums);

        void timeout();

        void needPermission();
    }

    private static final int MODE_STORAGE = 1;
    private static final int MODE_MEDIASTORE = 2;

    public static final String FILE_TYPE_NO_MEDIA = ".nomedia";
    public static final int PERMISSION_REQUEST_CODE = 16;

    public MediaProvider(Context context) {
        super(context);
    }

    public static boolean checkPermission(Activity context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permissionCheck = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(context,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    public void loadAlbums(final Activity context,
                           final boolean hiddenFolders,
                           final Callback callback) {

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
        }

        if (retriever != null) {
            retriever.loadAlbums(context, hiddenFolders,
                    new Callback() {
                        @Override
                        public void onMediaLoaded(ArrayList<Album> albums) {
                            if (!hiddenFolders) {
                                //remove excluded albums
                                for (int i = albums.size() - 1; i >= 0; i--) {
                                    if (albums.get(i).excluded) {
                                        albums.remove(i);
                                    }
                                }
                            }

                            int sort_by = Settings.getInstance(context).sortAlbumsBy();
                            SortUtil.sortAlbums(context, albums, sort_by);

                            callback.onMediaLoaded(albums);
                            setAlbums(albums);
                        }

                        @Override
                        public void timeout() {
                            callback.timeout();
                        }

                        @Override
                        public void needPermission() {
                            callback.needPermission();
                        }
                    });
        } else {
            callback.onMediaLoaded(null);
        }
    }

    private static void setAlbums(ArrayList<Album> albums) {
        MediaProvider.albums = albums;
    }

    public static ArrayList<Album> getAlbums() {
        return albums;
    }

    public static Album loadAlbum(String path) {
        if (albums == null) {
            return getErrorAlbum();
        }

        for (int i = 0; i < albums.size(); i++) {
            if (albums.get(i).getPath().equals(path)) {
                return albums.get(i);
            }
        }

        return getErrorAlbum();
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