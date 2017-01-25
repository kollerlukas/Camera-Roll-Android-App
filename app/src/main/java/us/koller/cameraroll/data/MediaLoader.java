package us.koller.cameraroll.data;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

import us.koller.cameraroll.util.MediaType;

public class MediaLoader {

    private String[] projection = new String[]{
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME};

    public static final int PERMISSION_REQUEST_CODE = 16;

    public MediaLoader() {

    }

    public ArrayList<Album> loadAlbums(Activity context, boolean hiddenFolders) {
        if (!checkPermission(context)) {
            return new ArrayList<>();
        }

        ArrayList<Album> albums = new ArrayList<>();

        /*// Return only video and image metadata.
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

        Cursor cursor = cursorLoader.loadInBackground();

        if (cursor == null) {
            return albums;
        }

        if (cursor.moveToFirst()) {
            String path;
            String bucket;

            int pathColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
            int bucketColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);

            do {
                path = cursor.getString(pathColumn);
                bucket = cursor.getString(bucketColumn);

                AlbumItem albumItem = AlbumItem.getInstance(context, path);
                if (albumItem != null) {
                    //search bucket
                    boolean foundBucket = false;
                    for (int i = 0; i < albums.size(); i++) {
                        if (albums.get(i).getName().equals(bucket)) {
                            albums.get(i).getAlbumItems().add(0, albumItem);
                            foundBucket = true;
                            break;
                        }
                    }

                    if (!foundBucket) {
                        //no bucket found
                        albums.add(new Album().setPath(bucket));
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
        }*/

        albums = searchStorage(context, albums);

        if (!hiddenFolders) {
            for (int i = albums.size() - 1; i >= 0; i--) {
                if (albums.get(i).hiddenAlbum) {
                    albums.remove(i);
                }
            }
        }

        return albums;
    }

    private ArrayList<Album> searchStorage(final Activity context, final ArrayList<Album> albums) {
        File file = new File("/storage/emulated/0");
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
                MediaStore.Images.Media.DATE_TAKEN        // Ordering
        );

        if (cursor == null) {
            return AlbumItem.getInstance(context, uri.toString());
        }

        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        cursor.close();

        return AlbumItem.getInstance(context, path != null ? path : uri.toString());
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
            int pathColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
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