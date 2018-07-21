package us.koller.cameraroll.data.provider.retriever;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import us.koller.cameraroll.data.fileOperations.FileOperation;
import us.koller.cameraroll.data.models.Album;
import us.koller.cameraroll.data.models.AlbumItem;
import us.koller.cameraroll.data.models.Video;
import us.koller.cameraroll.data.provider.MediaProvider;

//loading media through MediaStore
//advantage: speed, disadvantage: might be missing some items
public class MediaStoreRetriever extends Retriever {

    private static final String[] projection = new String[]{
            MediaStore.Files.FileColumns.DATA,
            /*MediaStore.Files.FileColumns.PARENT,*/
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Images.ImageColumns.DATE_TAKEN,
            MediaStore.Video.VideoColumns.DATE_TAKEN,
            BaseColumns._ID};

    public static String getPathForUri(Context c, Uri uri) {
        CursorLoader cL = new CursorLoader(c, uri, new String[]{MediaStore.Files.FileColumns.DATA},
                null, null, null);
        try {
            final Cursor crsr = cL.loadInBackground();
            if (crsr != null && crsr.getCount() > 0) {
                crsr.moveToFirst();
                return crsr.getString(crsr.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA));
            }
            return null;
        } catch (SecurityException e) {
            Toast.makeText(c, "Permission Error", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    @Override
    void loadAlbums(final Activity c, boolean hiddenFolders) {

        final long startTime = System.currentTimeMillis();

        final ArrayList<Album> as = new ArrayList<>();

        // Return only video and image metadata.
        String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                + " OR "
                + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

        Uri queryUri = MediaStore.Files.getContentUri("external");

        CursorLoader cursorLoader = new CursorLoader(
                c,
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
            ArrayList<Album> hiddenAlbums = checkHiddenFolders(c);
            as.addAll(hiddenAlbums);
        }

        AsyncTask.execute(() -> {
            if (cursor.moveToFirst()) {
                String path;
                long dateTaken, id;
                int pathColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
                int idColumn = cursor.getColumnIndex(BaseColumns._ID);

                do {
                    path = cursor.getString(pathColumn);
                    AlbumItem aI = AlbumItem.getInstance(c, path);
                    if (aI != null) {
                        //set dateTaken
                        int dateTakenColumn = cursor.getColumnIndex(
                                !(aI instanceof Video) ?
                                        MediaStore.Images.ImageColumns.DATE_TAKEN :
                                        MediaStore.Video.VideoColumns.DATE_TAKEN);
                        dateTaken = cursor.getLong(dateTakenColumn);
                        aI.setDate(dateTaken);

                        id = cursor.getLong(idColumn);
                        Uri uri = ContentUris.withAppendedId(
                                MediaStore.Files.getContentUri("external"), id);
                        aI.setUri(uri);

                        //search bucket
                        boolean foundBucket = false;
                        for (int i = 0; i < as.size(); i++) {
                            if (as.get(i).getPath().equals(FileOperation.Util.getParentPath(path))) {
                                as.get(i).getAlbumItems().add(0, aI);
                                foundBucket = true;
                                break;
                            }
                        }

                        if (!foundBucket) {
                            //no bucket found
                            String bucketPath = FileOperation.Util.getParentPath(path);
                            if (bucketPath != null) {
                                as.add(new Album().setPath(bucketPath));
                                as.get(as.size() - 1).getAlbumItems().add(0, aI);
                            }
                        }
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();

            //done loading media with content resolver
            MediaProvider.OnMediaLoadedCallback ca = getCallback();
            if (ca != null) {
                ca.onMediaLoaded(as);
            }

            Log.d("MediaStoreRetriever", "onMediaLoaded(): "
                    + String.valueOf(System.currentTimeMillis() - startTime) + " ms");
        });
    }

    @Override
    public void onDestroy() {
    }

    private ArrayList<Album> checkHiddenFolders(final Activity c) {

        ArrayList<Album> hAs = new ArrayList<>();

        // Scan all no Media files
        String nonMediaCondition = MediaStore.Files.FileColumns.MEDIA_TYPE
                + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_NONE;

        // Files with name contain .nomedia
        String selection = nonMediaCondition + " AND "
                + MediaStore.Files.FileColumns.TITLE + " LIKE ?";

        String[] params = new String[]{"%" + MediaProvider.FILE_TYPE_NO_MEDIA + "%"};

        // make query for non media files with file title contain ".nomedia" as
        // text on External Media URI
        Cursor crsr = c.getContentResolver().query(
                MediaStore.Files.getContentUri("external"),
                new String[]{MediaStore.Files.FileColumns.DATA},
                selection,
                params,
                MediaStore.Images.Media.DATE_TAKEN);

        if (crsr == null || crsr.getCount() == 0) {
            return hAs;
        }

        if (crsr.moveToFirst()) {
            int pathColumn = crsr.getColumnIndex(MediaStore.Files.FileColumns.DATA);
            do {
                String path = crsr.getString(pathColumn);
                path = path.replace(MediaProvider.FILE_TYPE_NO_MEDIA, "");
                File dir = new File(path);
                final Album a = new Album().setPath(path);

                File[] files = dir.listFiles();

                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        AlbumItem aI = AlbumItem.getInstance(files[i].getPath());
                        if (aI != null) {
                            a.getAlbumItems().add(aI);
                        }
                    }
                }
                if (a.getAlbumItems().size() > 0) {
                    hAs.add(a);
                }
            } while (crsr.moveToNext());
        }
        crsr.close();
        return hAs;
    }
}
