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
    void executeAsync(final Activity context, File_POJO target) {
        if (target == null) {
            return;
        }

        String s = context.getString(R.string.successfully_moved);

        final File_POJO[] files = getFiles();

        int success_count = 0;
        for (int i = files.length - 1; i >= 0; i--) {
            boolean result = moveFile(context, files[i].getPath(), target.getPath());
            success_count += result ? 1 : 0;
            setToastProgress(context, s, success_count);
        }

        if (success_count == 0) {
            setToastProgress(context, s, success_count);
        }

        if (callback != null) {
            callback.done();
        }

        operation = EMPTY;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public int getType() {
        return FileOperation.MOVE;
    }

    private static boolean moveFile(Activity context, String path, String destination) {
        File file = new File(path);
        File newFile = new File(destination, file.getName());
        boolean success = file.renameTo(newFile);

        scanPaths(context, new String[]{path, newFile.getPath()});

        return success;
    }
}
