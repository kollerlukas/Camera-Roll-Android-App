package us.koller.cameraroll.data.FileOperations;

import android.app.Activity;
import android.widget.Toast;

import java.io.File;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.File_POJO;

public class Move extends FileOperation {

    public Move(File_POJO[] files) {
        super(files);
    }

    @Override
    public void execute(Activity context, File_POJO target, Callback callback) {
        if (target == null) {
            return;
        }

        File_POJO[] files = getFiles();

        int success_count = 0;
        for (int i = 0; i < files.length; i++) {
            boolean result = moveFile(context, files[i].getPath(), target.getPath());
            success_count += result ? 1 : 0;
        }

        Toast.makeText(context, context.getString(R.string.successfully_moved)
                + String.valueOf(success_count) + "/"
                + String.valueOf(files.length), Toast.LENGTH_SHORT).show();

        if (callback != null) {
            callback.done();
        }

        operation = EMPTY;
    }

    private static boolean moveFile(Activity context, String path, String destination) {
        File file = new File(path);
        File newFile = new File(destination, file.getName());
        boolean success = file.renameTo(newFile);

        scanPaths(context, new String[]{path, newFile.getPath()});

        return success;
    }
}
