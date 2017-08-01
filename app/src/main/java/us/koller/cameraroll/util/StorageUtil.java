package us.koller.cameraroll.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import java.util.Arrays;

import us.koller.cameraroll.data.models.AlbumItem;
import us.koller.cameraroll.data.models.Video;

//workarounds to handle removable storage

//heavily inspired by:
//https://github.com/arpitkh96/AmazeFileManager/blob/master/src/main/java/com/amaze/filemanager/filesystem/MediaStoreHack.java
public class StorageUtil {

    public static Uri getContentUri(Context context, String path) {
        return getContentUri(context, AlbumItem.getInstance(path));
    }

    public static Uri getContentUri(Context context, AlbumItem albumItem) {
        Uri uri;
        if (albumItem instanceof Video) {
            uri = getContentUriForVideoFromMediaStore(context, albumItem.getPath());
        } else {
            uri = getContentUriForImageFromMediaStore(context, albumItem.getPath());
        }
        return uri;
    }

    private static Uri getContentUriForImageFromMediaStore(Context context, String path) {
        ContentResolver resolver = context.getContentResolver();
        //Uri photoUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        // to handle hidden images
        Uri photoUri = MediaStore.Files.getContentUri("external");
        Cursor cursor = resolver.query(photoUri,
                new String[]{BaseColumns._ID},
                MediaStore.MediaColumns.DATA + " = ?",
                new String[]{path}, null);
        if (cursor == null) {
            return Uri.parse(path);
        }
        cursor.moveToFirst();
        if (cursor.isAfterLast()) {
            cursor.close();
            // insert system media db
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DATA, path);
            values.put(MediaStore.Images.Media.MIME_TYPE, MediaType.getMimeType(path));
            return context.getContentResolver().insert(photoUri, values);
        } else {
            long id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
            Uri uri = ContentUris.withAppendedId(photoUri, id);
            cursor.close();
            return uri;
        }
    }

    private static Uri getContentUriForVideoFromMediaStore(Context context, String path) {
        ContentResolver resolver = context.getContentResolver();
        //Uri videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        // to handle hidden videos
        Uri videoUri = MediaStore.Files.getContentUri("external");
        Cursor cursor = resolver.query(videoUri,
                new String[]{BaseColumns._ID},
                MediaStore.MediaColumns.DATA + " = ?",
                new String[]{path}, null);

        if (cursor == null) {
            return Uri.parse(path);
        }
        cursor.moveToFirst();
        if (cursor.isAfterLast()) {
            cursor.close();
            // insert system media db
            ContentValues values = new ContentValues();
            values.put(MediaStore.Video.Media.DATA, path);
            values.put(MediaStore.Video.Media.MIME_TYPE, MediaType.getMimeType(path));
            return context.getContentResolver().insert(videoUri, values);
        } else {
            int imageId = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID));
            Uri uri = ContentUris.withAppendedId(videoUri, imageId);
            cursor.close();
            return uri;
        }
    }

    public static DocumentFile parseDocumentFile(Context context, Uri treeUri, String path) {
        DocumentFile treeRoot;
        try {
            treeRoot = DocumentFile.fromTreeUri(context, treeUri);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }

        if (treeRoot != null) {
            treeRoot = DocumentFile.fromTreeUri(context, treeUri);
            path = path.replace("/storage/", "");
            String[] pathParts = path.split("/");
            Log.d("StorageUtil", "pathParts: " + Arrays.toString(pathParts));

            DocumentFile file = treeRoot;
            for (int i = 1; i < pathParts.length; i++) {
                Log.d("StorageUtil", "pathParts[i]: " + pathParts[i]);
                if (file != null) {
                    file = file.findFile(pathParts[i]);
                } else {
                    Log.d("StorageUtil", "could find file: " + pathParts[i]);
                    break;
                }
            }
            return file;
        }
        return null;
    }

    public static DocumentFile createDocumentFile(Context context, Uri treeUri, String path, String mimeType) {
        int index = path.lastIndexOf("/");
        DocumentFile file = parseDocumentFile(context, treeUri, path.substring(0, index));

        if (file != null) {
            file = file.createFile(mimeType, path.substring(index));
        }
        return file;
    }

    public static DocumentFile createDocumentDir(Context context, Uri treeUri, String path) {
        int index = path.lastIndexOf("/");
        DocumentFile file = parseDocumentFile(context, treeUri, path.substring(0, index));

        if (file != null) {
            file = file.createDirectory(path.substring(index));
        }
        return file;
    }
}
