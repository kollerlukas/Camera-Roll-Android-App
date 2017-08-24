package us.koller.cameraroll.data.fileOperations;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import java.io.File;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.models.File_POJO;
import us.koller.cameraroll.util.StorageUtil;

public class Delete extends FileOperation {

    @Override
    String getNotificationTitle() {
        return getString(R.string.delete);
    }

    @Override
    public int getNotificationSmallIconRes() {
        return R.drawable.ic_delete_white_24dp;
    }

    @Override
    public void execute(Intent workIntent) {
        final File_POJO[] files = getFiles(workIntent);

        int success_count = 0;

        onProgress(success_count, files.length);

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
                result = deleteFile(files[i].getPath());
                Log.d("Delete", "execute: deleteFile()");
            }

            if (result) {
                success_count++;
                onProgress(success_count, files.length);
            } else {
                sendFailedBroadcast(workIntent, files[i].getPath());
            }
        }

        if (success_count == 0) {
            onProgress(success_count, files.length);
        }
    }

    @Override
    public int getType() {
        return FileOperation.DELETE;
    }

    public boolean deleteFile(String path) {
        boolean success;
        Log.d("Delete", "deleteFile: java.io.File");
        File file = new File(path);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                deleteFile(files[i].getPath());
            }
        }
        success = file.delete();
        addPathToScan(path);
        return success;
    }

    private boolean deleteFileOnRemovableStorage(Context context, Uri treeUri, String path) {
        boolean success = false;
        DocumentFile file = StorageUtil.parseDocumentFile(context, treeUri, new File(path));
        if (file != null) {
            Log.d("Delete", "execute: file is on removable storage" + ", canWrite(): " + String.valueOf(file.canWrite()));
            success = file.delete();
        } else {
            Log.d("Delete", "execute: file is on removable storage" + ", file = null");
        }

        //remove from MediaStore
        addPathToScan(path);
        return success;
    }
}
