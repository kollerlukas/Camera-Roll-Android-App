package us.koller.cameraroll.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import java.util.Arrays;

//workarounds to handle removable storage

//heavily inspired by:
//https://github.com/arpitkh96/AmazeFileManager/blob/master/src/main/java/com/amaze/filemanager/filesystem/MediaStoreHack.java
public class StorageUtil {

    //workaround to get content-Uri for items on removable storage
    public static Uri getContentUriFromFilePath(Context context, String path) {
        ContentResolver resolver = context.getContentResolver();

        Cursor cursor = resolver.query(MediaStore.Files.getContentUri("external"),
                new String[]{BaseColumns._ID}, MediaStore.MediaColumns.DATA + " = ?",
                new String[]{path}, MediaStore.MediaColumns.DATE_ADDED + " desc");

        if (cursor == null) {
            return Uri.parse(path);
        }

        cursor.moveToFirst();

        if (cursor.isAfterLast()) {
            cursor.close();
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DATA, path);
            return resolver.insert(MediaStore.Files.getContentUri("external"), values);
        } else {
            int imageId = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID));
            Uri uri = MediaStore.Files.getContentUri("external").buildUpon().appendPath(
                    Integer.toString(imageId)).build();
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
