package us.koller.cameraroll.data;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.SortUtil;
import us.koller.cameraroll.util.Util;

public class MediaLoader {

    public interface MediaLoaderCallback {
        void onMediaLoaded(ArrayList<Album> albums, boolean wasStorageSearched);
    }

    private String[] projection = new String[]{
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.PARENT};

    public static final int PERMISSION_REQUEST_CODE = 16;

    public MediaLoader() {

    }

    public void loadAlbums(final Activity context, final boolean hiddenFolders,
                           final MediaLoaderCallback callback) {
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

                if (hiddenFolders) {
                    ArrayList<Album> hiddenAlbums = loadHiddenFolders(context);
                    for (int i = 0; i < hiddenAlbums.size(); i++) {
                        albums.add(hiddenAlbums.get(i));
                    }
                }
                //done loading media with content resolver
                SortUtil.sortAlbums(context, albums, SortUtil.BY_NAME);
                callback.onMediaLoaded(albums, false);
            }
        });

        //load storage albums asynchronous
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                ArrayList<Album> albumsWithStorage = searchStorage(context, albums);

                if (!hiddenFolders) {
                    for (int i = albumsWithStorage.size() - 1; i >= 0; i--) {
                        if (albumsWithStorage.get(i).hiddenAlbum) {
                            albumsWithStorage.remove(i);
                        }
                    }
                }

                //done loading media from storage
                SortUtil.sortAlbums(context, albumsWithStorage, SortUtil.BY_NAME);
                callback.onMediaLoaded(albumsWithStorage, true);
            }
        });
    }

    private ArrayList<Album> searchStorage(final Activity context, final ArrayList<Album> albums) {
        File file = Environment.getExternalStorageDirectory(); //new File("/storage/emulated/0");
        File[] files = file.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (!files[i].getPath().equals("/storage/emulated/0/Android")) {
                recursivelySearchStorage(context, files[i], albums);
            }
        }
        return albums;
    }

    private void recursivelySearchStorage(Activity context, File file, ArrayList<Album> albums) {
        if (file == null) {
            return;
        }

        if (!file.isDirectory()) {
            return;
        }

        Log.d("MediaLoader", file.getPath());

        Album album = checkDirForMedia(context, file.getPath());
        if (album != null) {
            boolean alreadyExists = false;
            for (int k = 0; k < albums.size(); k++) {
                if (file.getPath().equals(albums.get(k).getPath())) {
                    alreadyExists = true;
                    break;
                }
            }

            if (!alreadyExists) {
                albums.add(album);
            }
        }

        File[] files = file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });

        for (int i = 0; i < files.length; i++) {
            Log.d("MediaLoader", file.getPath());
            recursivelySearchStorage(context, files[i], albums);
        }
    }

    private AlbumItem loadMediaItem(Activity context, Uri uri) {
        // Make the query.
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(uri,
                projection, // Which columns to return
                null,       // Which rows to return (all rows)
                null,       // Selection arguments (none)
                null        // Ordering
        );

        if (cursor == null) {
            return AlbumItem.getInstance(context, uri.toString());
        }

        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
        cursor.close();

        return AlbumItem.getInstance(context, path != null ? path : uri.toString());
    }

    static long getDateAdded(Activity context, String path) {

        long dateAdded = new File(path).lastModified();

        String[] projection = {MediaStore.Images.Media.DATE_TAKEN};

        // Make the query.
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(Uri.parse(path),
                projection, // Which columns to return
                null,       // Which rows to return (all rows)
                null,       // Selection arguments (none)
                MediaStore.Images.Media.DATE_TAKEN        // Ordering
        );

        if (cursor == null) {
            return dateAdded;
        }

        cursor.moveToFirst();
        String dateTaken = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN));
        cursor.close();

        dateAdded = Long.parseLong(dateTaken);

        return dateAdded;
    }

    private static final String FILE_TYPE_NO_MEDIA = ".nomedia";

    private ArrayList<Album> loadHiddenFolders(Activity context) {

        ArrayList<Album> hiddenAlbums = new ArrayList<>();

        // Scan all no Media files
        String nonMediaCondition = MediaStore.Files.FileColumns.MEDIA_TYPE
                + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_NONE;

        // Files with name contain .nomedia
        String where = nonMediaCondition + " AND "
                + MediaStore.Files.FileColumns.TITLE + " LIKE ?";

        String[] params = new String[]{"%" + FILE_TYPE_NO_MEDIA + "%"};

        // make query for non media files with file title contain ".nomedia" as
        // text on External Media URI
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Files.getContentUri("external"),
                projection,
                where,
                params,
                MediaStore.Images.Media.DATE_TAKEN);

        if (cursor == null || cursor.getCount() == 0) {
            return hiddenAlbums;
        }

        if (cursor.moveToFirst()) {
            int pathColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
            do {
                Album album = checkDirForMedia(context, cursor.getString(pathColumn));
                if (album != null) {
                    hiddenAlbums.add(album);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        return hiddenAlbums;
    }

    private Album checkDirForMedia(final Activity context, String path) {
        path = path.replace(FILE_TYPE_NO_MEDIA, "");
        File folder = new File(path);
        Album album = new Album().setPath(folder.getPath());
        album.hiddenAlbum = folder.isHidden();

        File[] files = folder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return MediaType.isMedia(context, file.getPath());
            }
        });

        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (MediaType.isMedia(context, files[i].getPath())) {
                    AlbumItem albumItem = loadMediaItem(context, Uri.parse(files[i].getPath()));
                    album.getAlbumItems().add(albumItem);
                }
            }
        }

        if (album.getAlbumItems().size() == 0) {
            return null;
        }
        return album;
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