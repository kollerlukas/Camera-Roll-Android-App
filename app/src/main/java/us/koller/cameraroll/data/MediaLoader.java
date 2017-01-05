package us.koller.cameraroll.data;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

public class MediaLoader {

    public static final int PERMISSION_REQUEST_CODE = 16;

    private final String[] imageFileEndings = new String[]{"jpg", "png", "gif", "jpeg"};

    public MediaLoader() {

    }

    public ArrayList<Album> loadPhotos(Activity context, boolean hiddenFolders) {
        if (!checkPermission(context)) {
            return new ArrayList<>();
        }

        ArrayList<Album> albums = new ArrayList<>();

        // Get relevant columns for use later.
        String[] projection = {
                MediaStore.Files.FileColumns.TITLE,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        };

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

        Cursor cursor = cursorLoader.loadInBackground();

        if (cursor == null) {
            return albums;
        }

        if (cursor.moveToFirst()) {
            String name;
            String path;
            String bucket;

            int bucketColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
            int nameColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.TITLE);
            int pathColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);

            do {
                name = cursor.getString(nameColumn);
                path = cursor.getString(pathColumn);
                bucket = cursor.getString(bucketColumn);

                Album.AlbumItem albumItem;
                if (isImage(path)) {
                    albumItem = new Album.Photo()
                            .setName(name)
                            .setPath(path);
                } else {
                    albumItem = new Album.Video()
                            .setName(name)
                            .setPath(path);
                }

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
                    albums.add(new Album().setName(bucket));
                    albums.get(albums.size() - 1).getAlbumItems().add(0, albumItem);
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

        return albums;
    }

    public Album.Photo loadPhoto(Activity context, Uri uri) {
        // which image properties are we querying
        String[] projection = new String[]{
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        };

        // Make the query.
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(uri,
                projection, // Which columns to return
                null,       // Which rows to return (all rows)
                null,       // Selection arguments (none)
                MediaStore.Images.Media.DATE_TAKEN        // Ordering
        );

        if (cursor == null) {
            return (Album.Photo) new Album.Photo()
                    .setName(new File(uri.getPath()).getName())
                    .setPath(uri.getPath());
        }

        cursor.moveToFirst();
        String name = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        cursor.close();

        Album.Photo photo = (Album.Photo) new Album.Photo()
                .setName(name)
                .setPath(path);

        photo.contentUri = path == null;
        if (photo.contentUri) {
            photo.setPath(uri.toString());
        }
        return photo;
    }

    private static final String FILE_TYPE_NO_MEDIA = ".nomedia";

    private ArrayList<Album> loadHiddenFolders(Activity context) {

        ArrayList<Album> hiddenAlbums = new ArrayList<>();

        // which image properties are we querying
        String[] projection = new String[]{MediaStore.Images.Media.DATA};

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
                Album album = checkDirForImages(context, cursor.getString(pathColumn));
                if (album != null) {
                    hiddenAlbums.add(album);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        return hiddenAlbums;
    }

    private Album checkDirForImages(Activity context, String path) {
        path = path.replace(FILE_TYPE_NO_MEDIA, "");
        File folder = new File(path);
        Album album = new Album().setName(folder.getName());
        album.hiddenAlbum = true;
        File[] files = folder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return isImage(file.getPath());
            }
        });
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (isImage(files[i].getPath())) {
                    Album.Photo photo = loadPhoto(context, Uri.parse(files[i].getPath()));
                    album.getAlbumItems().add(photo);
                }
            }
        }
        if (album.getAlbumItems().size() == 0) {
            return null;
        }
        return album;
    }

    private boolean isImage(String filename) {
        if (filename == null) {
            return false;
        }

        for (int i = 0; i < imageFileEndings.length; i++) {
            if (filename.endsWith(imageFileEndings[i])) {
                return true;
            }
        }
        return false;
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