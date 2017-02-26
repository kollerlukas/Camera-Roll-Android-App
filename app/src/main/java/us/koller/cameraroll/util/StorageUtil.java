package us.koller.cameraroll.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.IOException;

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

    //not working; need to fix
    public static boolean delete(final Context context, final File file) throws IOException {
        final String where = MediaStore.MediaColumns.DATA + "=?";
        final String[] selectionArgs = new String[]{
                file.getCanonicalPath()
        };
        final ContentResolver contentResolver = context.getContentResolver();
        final Uri filesUri = MediaStore.Files.getContentUri("external");
        Log.d("StorageUtil", "delete: " + filesUri);

        // Delete the entry from the media database. This will actually delete media files.
        contentResolver.delete(filesUri, where, selectionArgs);
        // If the file is not a media file, create a new entry.
        if (file.exists()) {
            final ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            // Delete the created entry, such that content provider will delete the file.
            contentResolver.delete(filesUri, where, selectionArgs);
        }
        return !file.exists();
    }
}
