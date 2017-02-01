package us.koller.cameraroll.data.MediaLoader;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.ui.MainActivity;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.SortUtil;
import us.koller.cameraroll.util.Util;

public class MediaLoader {

    public interface MediaLoaderCallback {
        void onMediaLoaded(ArrayList<Album> albums, boolean wasStorageSearched);

        void timeout();
    }

    private interface Callback {
        void callback(ArrayList<Album> albums);
    }

    private static final String FILE_TYPE_NO_MEDIA = ".nomedia";
    public static final int PERMISSION_REQUEST_CODE = 16;
    private static final String[] projection = new String[]{
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.PARENT};

    private ArrayList<MediaLoaderThread> mediaLoaderThreads;

    public MediaLoader() {

    }

    public void loadAlbums(final Activity context, final boolean hiddenFolders,
                           final MediaLoaderCallback callback) {
        final long startTime = System.currentTimeMillis();

        if (!checkPermission(context)) {
            return;
        }

        //Idea: load content resolver media first,
        // then asynchronous search the external storage for media

        final ArrayList<Album> albums = new ArrayList<>();

        // Return only video and image metadata.
        String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                + " OR "
                + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

        Uri queryUri = MediaStore.Files.getContentUri("external");

        CursorLoader cursorLoader = new CursorLoader(
                context,
                queryUri,
                projection,
                selection,
                null, // Selection args (none).
                MediaStore.Files.FileColumns.DATE_ADDED);

        final Cursor cursor = cursorLoader.loadInBackground();

        if (cursor == null) {
            return;
        }

        //search hiddenFolders
        if (hiddenFolders) {
            ArrayList<Album> hiddenAlbums = loadHiddenFolders(context);
            albums.addAll(hiddenAlbums);
        }

        //handle timeout
        final Handler handler = new Handler();
        final Runnable timeout = new Runnable() {
            @Override
            public void run() {
                callback.timeout();
            }
        };
        handler.postDelayed(timeout, 1000);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if (cursor.moveToFirst()) {
                    String path;
                    int pathColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);

                    do {
                        path = cursor.getString(pathColumn);

                        AlbumItem albumItem = AlbumItem.getInstance(context, path);
                        if (albumItem != null) {
                            //search bucket
                            boolean foundBucket = false;
                            for (int i = 0; i < albums.size(); i++) {
                                if (albums.get(i).getPath().equals(Util.getParentPath(path))) {
                                    albums.get(i).getAlbumItems().add(0, albumItem);
                                    foundBucket = true;
                                    break;
                                }
                            }

                            if (!foundBucket) {
                                //no bucket found
                                albums.add(new Album().setPath(Util.getParentPath(path)));
                                albums.get(albums.size() - 1).getAlbumItems().add(0, albumItem);
                            }
                        }

                    } while (cursor.moveToNext());

                }
                cursor.close();

                //done loading media with content resolver
                SortUtil.sortAlbums(context, albums, SortUtil.BY_NAME);
                callback.onMediaLoaded(albums, false);
                Log.d("MediaLoader", "onMediaLoaded(..., false): " + String.valueOf(System.currentTimeMillis() - startTime));


                //load media from storage
                searchStorage(context, albums, new Callback() {
                    @Override
                    public void callback(ArrayList<Album> albums) {
                        if (!hiddenFolders) {
                            for (int i = albums.size() - 1; i >= 0; i--) {
                                if (albums.get(i).hiddenAlbum) {
                                    albums.remove(i);
                                }
                            }
                        }

                        //done loading media from storage
                        SortUtil.sortAlbums(context, albums, SortUtil.BY_NAME);
                        callback.onMediaLoaded(albums, true);
                        handler.removeCallbacks(timeout);
                        Log.d("MediaLoader", "onMediaLoaded(..., true): " + String.valueOf(System.currentTimeMillis() - startTime));
                    }
                });
            }
        });
    }

    private void searchStorage(final Activity context, final ArrayList<Album> albums, final Callback callback) {
        File dir = Environment.getExternalStorageDirectory(); //new File("/storage/emulated/0");
        File[] dirs = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return !file.getName().equals("Android");
            }
        });

        mediaLoaderThreads = new ArrayList<>();

        for (int i = 0; i < dirs.length; i++) {
            final File file = dirs[i];
            MediaLoaderThread mediaLoaderThread
                    = new MediaLoaderThread(context, file, new MediaLoaderThread.Callback() {
                @Override
                public void done(MediaLoaderThread thread, ArrayList<Album> albumsToAdd) {
                    mergeAlbums(albums, albumsToAdd);
                    mediaLoaderThreads.remove(thread);
                    thread.cancel();
                    if (mediaLoaderThreads.size() == 0) {
                        callback.callback(albums);
                        mediaLoaderThreads = null;
                    }
                }
            });
            mediaLoaderThread.start();
            mediaLoaderThreads.add(mediaLoaderThread);
        }
    }

    private void mergeAlbums(ArrayList<Album> albums, ArrayList<Album> albumsToAdd) {
        for (int i = albumsToAdd.size() - 1; i >= 0; i--) {
            for (int k = 0; k < albums.size(); k++) {
                if (albumsToAdd.get(i).getPath()
                        .equals(albums.get(k).getPath())) {
                    albumsToAdd.remove(i);
                    break;
                }
            }
        }
        albums.addAll(albumsToAdd);
    }

    public void onDestroy() {
        //cancel all mediaLoaderThreads when Activity is being destroyed
        if (mediaLoaderThreads != null) {
            for (int i = 0; i < mediaLoaderThreads.size(); i++) {
                mediaLoaderThreads.get(i).cancel();
            }
        }
    }

    private ArrayList<Album> loadHiddenFolders(final Activity context) {

        ArrayList<Album> hiddenAlbums = new ArrayList<>();

        // Scan all no Media files
        String nonMediaCondition = MediaStore.Files.FileColumns.MEDIA_TYPE
                + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_NONE;

        // Files with name contain .nomedia
        String selection = nonMediaCondition + " AND "
                + MediaStore.Files.FileColumns.TITLE + " LIKE ?";

        String[] params = new String[]{"%" + FILE_TYPE_NO_MEDIA + "%"};

        // make query for non media files with file title contain ".nomedia" as
        // text on External Media URI
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Files.getContentUri("external"),
                new String[]{MediaStore.Files.FileColumns.DATA},
                selection,
                params,
                MediaStore.Images.Media.DATE_TAKEN);

        if (cursor == null || cursor.getCount() == 0) {
            return hiddenAlbums;
        }

        if (cursor.moveToFirst()) {
            int pathColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);

            do {
                String path = cursor.getString(pathColumn);
                Log.d("MediaLoader", path);

                path = path.replace(FILE_TYPE_NO_MEDIA, "");
                File folder = new File(path);
                final Album album = new Album().setPath(folder.getPath());
                album.hiddenAlbum = true;

                folder.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        if (MediaType.isMedia(context, file.getPath())) {
                            AlbumItem albumItem = AlbumItem.getInstance(context, file.getPath());
                            album.getAlbumItems().add(albumItem);
                            return true;
                        }
                        return false;
                    }
                });

                if (album.getAlbumItems().size() > 0) {
                    hiddenAlbums.add(album);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        return hiddenAlbums;
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
}