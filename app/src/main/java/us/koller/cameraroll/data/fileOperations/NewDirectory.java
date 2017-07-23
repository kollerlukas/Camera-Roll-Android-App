package us.koller.cameraroll.data.fileOperations;

import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import java.io.File;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.util.StorageUtil;

public class NewDirectory extends FileOperation {

    @Override
    void execute(Intent workIntent) {
        final File_POJO[] files = getFiles(workIntent);
        if (files.length > 0) {
            final File_POJO file = files[0];

            //check if file is on removable storage
            boolean writingOntoRemovableStorage = Util.isOnRemovableStorage(file.getPath());

            Uri treeUri = null;
            if (writingOntoRemovableStorage) {
                treeUri = getTreeUri(workIntent, null);
                if (treeUri == null) {
                    return;
                }
            }

            boolean result;
            if (!writingOntoRemovableStorage) {
                result = createNewFolder(file.getPath());
            } else {
                result = StorageUtil.createDocumentDir(getApplicationContext(), treeUri, file.getPath()) != null;
            }

            if (!result) {
                sendFailedBroadcast(workIntent, file.getPath());
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), getString(R.string.successfully_created_new_folder),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    @Override
    public int getType() {
        return FileOperation.NEW_DIR;
    }

    private static boolean createNewFolder(String newFolderPath) {
        File dir = new File(newFolderPath);
        return !dir.exists() && dir.mkdirs();
    }
}
