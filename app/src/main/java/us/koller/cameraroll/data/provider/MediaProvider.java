package us.koller.cameraroll.data.provider;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;

import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.data.models.Album;
import us.koller.cameraroll.data.models.AlbumItem;
import us.koller.cameraroll.data.models.VirtualAlbum;
import us.koller.cameraroll.data.provider.retriever.MediaStoreRetriever;
import us.koller.cameraroll.data.provider.retriever.StorageRetriever;
import us.koller.cameraroll.util.SortUtil;

public class MediaProvider extends Provider {

    private static ArrayList<Album> albums;

    public static final String SINGLE_FOLDER_PATH = "CAMERA_ROLL_SINGLE_FOLDER_PATH";
    private static final int MODE_STORAGE = 1;
    private static final int MODE_MEDIASTORE = 2;

    public static final String FILE_TYPE_NO_MEDIA = ".nomedia";
    public static final int PERMISSION_REQUEST_CODE = 16;

    public static boolean dataChanged = false;

    public abstract static class OnMediaLoadedCallback implements Provider.Callback {
        public abstract void onMediaLoaded(ArrayList<Album> albums);
    }

    public MediaProvider(Context c) {
        super(c);
    }

    public static boolean checkPermission(Activity c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int read = ContextCompat.checkSelfPermission(c, Manifest.permission.READ_EXTERNAL_STORAGE);
            int write = ContextCompat.checkSelfPermission(c, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (read != PackageManager.PERMISSION_GRANTED || write != PackageManager.PERMISSION_GRANTED) {
                String[] requestedPermissions = new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(c, requestedPermissions, PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    public static ArrayList<Album> getAlbums() {
        return albums;
    }

    private static void setAlbums(ArrayList<Album> albums) {
        MediaProvider.albums = albums;
    }

    public static ArrayList<Album> getAlbumsWithVirtualDirectories(Activity c) {
        Settings s = Settings.getInstance(c);
        boolean virtualDirs = s.getVirtualDirectories();
        if (!virtualDirs) {
            return getAlbums();
        }

        ArrayList<VirtualAlbum> vAs = getVirtualAlbums(c);
        ArrayList<Album> albums = getAlbums();
        if (albums == null || vAs == null) {
            return albums;
        }
        //noinspection unchecked
        albums = (ArrayList<Album>) getAlbums().clone();
        ArrayList<Album> albumsWithVirtualDirs = new ArrayList<>();

        for (int i = vAs.size() - 1; i >= 0; i--) {
            VirtualAlbum vA = vAs.get(i);
            if (vA.getDirectories().size() > 0) {
                vA.create(c, albums);
                if (vA.getAlbumItems().size() > 0) {
                    albumsWithVirtualDirs.add(vA);
                }
            }
        }

        albumsWithVirtualDirs.addAll(albums);
        for (int i = albumsWithVirtualDirs.size() - 1; i >= 0; i--) {
            Album a = albumsWithVirtualDirs.get(i);
            if (!(a instanceof VirtualAlbum)) {
                for (int k = 0; k < vAs.size(); k++) {
                    if (vAs.get(k).contains(a.getPath())) {
                        albumsWithVirtualDirs.remove(i);
                        break;
                    }
                }
            }
        }
        SortUtil.sortAlbums(c, albumsWithVirtualDirs);
        return albumsWithVirtualDirs;
    }

    public static void loadAlbum(final Activity c, final String path, final OnAlbumLoadedCallback ca) {
        if (path == null) {
            c.runOnUiThread(() -> ca.onAlbumLoaded(null));
            return;
        }

        if (albums == null) {
            Settings s = Settings.getInstance(c);
            boolean hiddenFolders = s.getHiddenFolders();
            new MediaProvider(c).loadAlbums(c, hiddenFolders, new OnMediaLoadedCallback() {
                @Override
                public void onMediaLoaded(ArrayList<Album> albums) {
                    loadAlbum(c, path, ca);
                }

                @Override
                public void timeout() {
                    ca.onAlbumLoaded(getErrorAlbum());
                }

                @Override
                public void needPermission() {
                    ca.onAlbumLoaded(getErrorAlbum());
                }
            });
        } else {
            if (path.startsWith(VirtualAlbum.VIRTUAL_ALBUMS_DIR)) {
                ArrayList<VirtualAlbum> virtualDirectories = getVirtualAlbums(c);
                for (int i = 0; i < virtualDirectories.size(); i++) {
                    if (virtualDirectories.get(i).getPath().equals(path)) {
                        final VirtualAlbum album = virtualDirectories.get(i);
                        album.create(c, albums);
                        c.runOnUiThread(() -> ca.onAlbumLoaded(album));
                        return;
                    }
                }
            } else if (path.equals(SINGLE_FOLDER_PATH)) {
                final Album a = new Album().setPath(MediaProvider.SINGLE_FOLDER_PATH);
                for (int i = 0; i < albums.size(); i++) {
                    a.getAlbumItems().addAll(albums.get(i).getAlbumItems());
                }
                int sortBy = Settings.getInstance(c).sortAlbumsBy();
                SortUtil.sort(a.getAlbumItems(), sortBy);
                c.runOnUiThread(() -> ca.onAlbumLoaded(a));
            } else {
                for (int i = 0; i < albums.size(); i++) {
                    if (albums.get(i).getPath().equals(path)) {
                        final Album a = albums.get(i);
                        c.runOnUiThread(() -> ca.onAlbumLoaded(a));
                        return;
                    }
                }
            }
        }
    }

    public static Album getErrorAlbum() {
        //Error album
        Album a = new Album().setPath("ERROR");
        a.getAlbumItems().add(AlbumItem.getErrorItem());
        return a;
    }

    private static int getMode(Context c) {
        return Settings.getInstance(c).useStorageRetriever() ?
                MODE_STORAGE : MODE_MEDIASTORE;
    }

    public void loadAlbums(final Activity c, final boolean hiddenFolders, OnMediaLoadedCallback ca) {
        if (!MediaProvider.checkPermission(c)) {
            ca.needPermission();
            return;
        }
        int mode = getMode(c);
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
            setCallback(ca);

            retriever.loadAlbums(c, hiddenFolders, new OnMediaLoadedCallback() {
                @Override
                public void onMediaLoaded(ArrayList<Album> albums) {
                    //remove excluded albums
                    for (int i = albums.size() - 1; i >= 0; i--) {
                        if (albums.get(i) == null || albums.get(i).excluded) {
                            albums.remove(i);
                        }
                    }
                    SortUtil.sortAlbums(c, albums);
                    setAlbums(albums);
                    OnMediaLoadedCallback ca = getCallback();
                    if (ca != null) {
                        ca.onMediaLoaded(albums);
                    }
                }

                @Override
                public void timeout() {
                    OnMediaLoadedCallback ca = getCallback();
                    if (ca != null) {
                        ca.timeout();
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
            if (ca != null) {
                ca.onMediaLoaded(null);
            }
        }
    }

    public interface OnAlbumLoadedCallback {
        void onAlbumLoaded(Album a);
    }
}