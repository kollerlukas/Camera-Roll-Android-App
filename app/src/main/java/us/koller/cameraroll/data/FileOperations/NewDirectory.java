package us.koller.cameraroll.data.FileOperations;

import android.content.Intent;
import android.widget.Toast;

import java.io.File;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.File_POJO;

public class NewDirectory extends FileOperation {

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    void execute(Intent workIntent) {
        final File_POJO[] files = getFiles(workIntent);
        if (files.length > 0) {
            final File_POJO file = files[0];
            boolean result = createNewFolder(file.getPath());
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
