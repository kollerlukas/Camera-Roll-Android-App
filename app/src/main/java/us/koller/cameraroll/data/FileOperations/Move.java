package us.koller.cameraroll.data.FileOperations;

import android.content.Context;
import android.content.Intent;

import java.io.File;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.File_POJO;

public class Move extends FileOperation {

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    void execute(Intent workIntent) {
        File_POJO[] files = getFiles(workIntent);
        File_POJO target = workIntent.getParcelableExtra(TARGET);

        if (target == null) {
            return;
        }

        String s = getString(R.string.successfully_moved);

        int success_count = 0;

        onProgress(s, success_count, files.length);

        for (int i = files.length - 1; i >= 0; i--) {
            boolean result = moveFile(getApplicationContext(), files[i].getPath(), target.getPath());
            success_count += result ? 1 : 0;
            onProgress(s, success_count, files.length);
        }

        if (success_count == 0) {
            onProgress(s, success_count, files.length);
        }
    }

    @Override
    public int getType() {
        return FileOperation.MOVE;
    }

    private static boolean moveFile(Context context, String path, String destination) {
        File file = new File(path);
        File newFile = new File(destination, file.getName());
        boolean success = file.renameTo(newFile);

        FileOperation.Util.scanPaths(context, new String[]{path, newFile.getPath()});

        return success;
    }
}
