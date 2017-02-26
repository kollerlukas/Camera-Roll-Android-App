package us.koller.cameraroll.data.FileOperations;

import android.app.Activity;
import android.widget.Toast;

import java.io.File;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.File_POJO;

public class NewDirectory extends FileOperation {

    public NewDirectory(File_POJO[] files) {
        super(files);
    }

    @Override
    public void execute(Activity context, File_POJO target, Callback callback) {
        File_POJO[] files = getFiles();
        if (files.length > 0) {
            File_POJO file = files[0];
            boolean result = createNewFolder(file.getPath());
            if (!result) {
                if (callback != null) {
                    callback.failed(file.getPath());
                }
            }

            Toast.makeText(context, context.getString(R.string.successfully_created_new_folder),
                    Toast.LENGTH_SHORT).show();
        }

        if (callback != null) {
            callback.done();
        }

        operation = EMPTY;
    }

    private static boolean createNewFolder(String newFolderPath) {
        File dir = new File(newFolderPath);
        return !dir.exists() && dir.mkdirs();
    }
}
