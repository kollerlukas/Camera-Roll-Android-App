package us.koller.cameraroll.data.FileOperations;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import java.io.File;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.StorageUtil;

public class Delete extends FileOperation {

    @Override
    void execute(Intent workIntent) {
        final File_POJO[] files = getFiles(workIntent);

        String s = getString(R.string.successfully_deleted);

        int success_count = 0;

        onProgress(s, success_count, files.length);

        for (int i = 0; i < files.length; i++) {
            boolean result;
            //check if file is on removable storage
            if (Util.isOnRemovableStorage(files[i].getPath())) {
                //file is on removable storage
                Uri treeUri = getTreeUri(workIntent, files[i].getPath());
                if (treeUri == null) {
                    return;
                }
                result = deleteFileOnRemovableStorage(getApplicationContext(), treeUri, files[i].getPath());
                Log.d("Delete", "execute: deleteFileOnRemovableStorage()");
            } else {
                result = deleteFile(getApplicationContext(), files[i].getPath());
                Log.d("Delete", "execute: deleteFile()");
            }

            if (result) {
                success_count++;
                onProgress(s, success_count, files.length);
            } else {
                sendFailedBroadcast(workIntent, files[i].getPath());
            }
        }

        if (success_count == 0) {
            onProgress(s, success_count, files.length);
        }

        sendDoneBroadcast();
    }

    @Override
    public boolean autoSendDoneBroadcast() {
        return false;
    }

    @Override
    public int getType() {
        return FileOperation.DELETE;
    }

    private static boolean deleteFile(final Context context, String path) {
        boolean success;
        if (MediaType.isMedia(path)) {
            Log.d("Delete", "deleteFile: ContentResolver");
            ContentResolver resolver = context.getContentResolver();

            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DATA, path);
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            String where = MediaStore.MediaColumns.DATA + "=?";
            String[] selectionArgs = new String[]{path};

            int rows = resolver.delete(MediaStore.Files.getContentUri("external"),
                    where, selectionArgs);

            success = rows > 0;
        } else {
            Log.d("Delete", "deleteFile: java.io.File");
            File file = new File(path);
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (int i = 0; i < files.length; i++) {
                    deleteFile(context, files[i].getPath());
                }
            }
            success = file.delete();
        }
        FileOperation.Util.scanPaths(context, new String[]{path}, new long[]{-1}, null);
        return success;
    }

    static boolean deleteFileOnRemovableStorage(Context context, Uri treeUri, String path) {
        boolean success = false;
        DocumentFile file = StorageUtil.parseDocumentFile(context, treeUri, path);
        if (file != null) {
            Log.d("Delete", "execute: file is on removable storage" + ", canWrite(): " + String.valueOf(file.canWrite()));
            success = file.delete();
        } else {
            Log.d("Delete", "execute: file is on removable storage" + ", file = null");
        }

        //remove from MediaStore
        FileOperation.Util.scanPaths(context, new String[]{path}, new long[]{-1}, null);
        return success;
    }
}
