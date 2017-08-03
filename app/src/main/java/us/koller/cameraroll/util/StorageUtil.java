package us.koller.cameraroll.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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

    public static File[] getRemovableStorageRoots(Context context) {
        File[] roots = context.getExternalFilesDirs("external");
        ArrayList<File> rootsArrayList = new ArrayList<>();

        for (int i = 0; i < roots.length; i++) {
            if (roots[i] != null) {
                String path = roots[i].getPath();
                int index = path.lastIndexOf("/Android/data/");
                if (index > 0) {
                    path = path.substring(0, index);
                    if (!path.equals(Environment.getExternalStorageDirectory().getPath())) {
                        rootsArrayList.add(new File(path));
                    }
                }
            }
        }

        roots = new File[rootsArrayList.size()];
        rootsArrayList.toArray(roots);
        return roots;
    }

    private static String getSdCardRootPath(Context context, String path) {
        File[] roots = getRemovableStorageRoots(context);
        for (int i = 0; i < roots.length; i++) {
            if (path.startsWith(roots[i].getPath())) {
                return roots[i].getPath();
            }
        }
        return null;
    }

    public static DocumentFile parseDocumentFile(Context context, Uri treeUri, File file) {
        DocumentFile treeRoot;
        try {
            treeRoot = DocumentFile.fromTreeUri(context, treeUri);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }

        String path;
        try {
            path = file.getCanonicalPath();
            String sdCardPath = getSdCardRootPath(context, path);
            if (sdCardPath != null) {
                if (sdCardPath.equals(path)) {
                    return treeRoot;
                }
                path = path.substring(sdCardPath.length() + 1);
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        Log.d("StorageUtil", "path: " + path);

        if (treeRoot != null) {
            treeRoot = DocumentFile.fromTreeUri(context, treeUri);
            String[] pathParts = path.split("/");
            DocumentFile documentFile = treeRoot;
            for (int i = 0; i < pathParts.length; i++) {
                if (documentFile != null) {
                    documentFile = documentFile.findFile(pathParts[i]);
                } else {
                    return null;
                }
            }
            return documentFile;
        }
        return null;
    }

    public static DocumentFile createDocumentFile(Context context, Uri treeUri, String path, String mimeType) {
        int index = path.lastIndexOf("/");
        String dirPath = path.substring(0, index);
        DocumentFile file = parseDocumentFile(context, treeUri, new File(dirPath));
        if (file != null) {
            String name = path.substring(index + 1);
            file = file.createFile(mimeType, name);
        }
        return file;
    }

    public static DocumentFile createDocumentDir(Context context, Uri treeUri, String path) {
        int index = path.lastIndexOf("/");
        String dirPath = path.substring(0, index);
        DocumentFile file = parseDocumentFile(context, treeUri, new File(dirPath));
        if (file != null) {
            String name = path.substring(index + 1);
            file = file.createDirectory(name);
        }
        return file;
    }
}
