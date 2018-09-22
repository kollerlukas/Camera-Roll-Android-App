package us.koller.cameraroll.data.fileOperations;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.provider.DocumentFile;

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
        return R.drawable.ic_delete_white;
    }

    @Override
    public void execute(Intent workIntent) {
        final File_POJO[] files = getFiles(workIntent);

        int success_count = 0;

        onProgress(success_count, files.length);

        for (File_POJO file : files) {
            boolean result;
            //check if file is on removable storage
            if (Util.isOnRemovableStorage(file.getPath())) {
                //file is on removable storage
                Uri treeUri = getTreeUri(workIntent, file.getPath());
                if (treeUri == null) {
                    return;
                }
                result = deleteFileOnRemovableStorage(getApplicationContext(), treeUri, file.getPath());
            } else {
                result = deleteFile(file.getPath());
                //Delete Album, when empty
                /*String parentPath = Util.getParentPath(files[i].getPath());
                if (result && Util.isDirectoryEmpty(parentPath)) {
                    deleteFile(parentPath);
                }*/
            }

            if (result) {
                success_count++;
                onProgress(success_count, files.length);
            } else {
                sendFailedBroadcast(workIntent, file.getPath());
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
        File file = new File(path);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File file1 : files) {
                deleteFile(file1.getPath());
            }
        }
        success = file.delete();
        addPathToScan(path);
        return success;
    }

    boolean deleteFileOnRemovableStorage(Context context, Uri treeUri, String path) {
        boolean success = false;
        DocumentFile file = StorageUtil.parseDocumentFile(context, treeUri, new File(path));
        if (file != null) {
            success = file.delete();
        }
        //remove from MediaStore
        addPathToScan(path);
        return success;
    }
}
