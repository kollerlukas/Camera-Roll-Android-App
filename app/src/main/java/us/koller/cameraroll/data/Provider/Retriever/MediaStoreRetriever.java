package us.koller.cameraroll.data.Provider.Retriever;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.Provider.MediaProvider;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.SortUtil;
import us.koller.cameraroll.util.Util;

//loading media through MediaStore
//advantage: speed, disadvantage: might be missing some items
public class MediaStoreRetriever implements Retriever {

    private static final String[] projection = new String[]{
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.PARENT};


    @Override
    public void loadAlbums(final Activity context, boolean hiddenFolders, final MediaProvider.Callback callback) {

        final long startTime = System.currentTimeMillis();

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
            ArrayList<Album> hiddenAlbums = checkHiddenFolders(context);
            albums.addAll(hiddenAlbums);
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
                                String bucketPath = Util.getParentPath(path);
                                if (bucketPath != null) {
                                    albums.add(new Album().setPath(bucketPath));
                                    albums.get(albums.size() - 1).getAlbumItems().add(0, albumItem);
                                }
                            }
                        }

                    } while (cursor.moveToNext());

                }
                cursor.close();

                //done loading media with content resolver
                //SortUtil.sortAlbums(context, albums, SortUtil.BY_NAME);
                SortUtil.sortByName(albums);
                callback.onMediaLoaded(albums);
                Log.d("MediaStoreRetriever", "onMediaLoaded(): "
                        + String.valueOf(System.currentTimeMillis() - startTime) + " ms");
            }
        });
    }

    @Override
    public void onDestroy() {

    }

    private ArrayList<Album> checkHiddenFolders(final Activity context) {

        ArrayList<Album> hiddenAlbums = new ArrayList<>();

        // Scan all no Media files
        String nonMediaCondition = MediaStore.Files.FileColumns.MEDIA_TYPE
                + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_NONE;

        // Files with name contain .nomedia
        String selection = nonMediaCondition + " AND "
                + MediaStore.Files.FileColumns.TITLE + " LIKE ?";

        String[] params = new String[]{"%" + MediaProvider.FILE_TYPE_NO_MEDIA + "%"};

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

                path = path.replace(MediaProvider.FILE_TYPE_NO_MEDIA, "");
                File dir = new File(path);
                final Album album = new Album().setPath(path);

                File[] files = dir.listFiles();

                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        if (MediaType.isMedia(context, files[i].getPath())) {
                            AlbumItem albumItem = AlbumItem.getInstance(context, files[i].getPath());
                            if (albumItem != null) {
                                album.getAlbumItems().add(albumItem);
                            }
                        }
                    }
                }

                if (album.getAlbumItems().size() > 0) {
                    hiddenAlbums.add(album);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        return hiddenAlbums;
    }
}
