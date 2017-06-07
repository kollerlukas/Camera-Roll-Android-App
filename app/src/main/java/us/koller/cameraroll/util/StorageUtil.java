package us.koller.cameraroll.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import java.io.File;
import java.util.Arrays;
import java.util.List;

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

    //TODO implement
    public static boolean haveRemovableStoragePermission(ContentResolver contentResolver) {
        List<UriPermission> uriPermissions = contentResolver.getPersistedUriPermissions();
        Log.d("StorageUtil", "uriPermissions: " + uriPermissions.toString());
        return true;
    }
}
