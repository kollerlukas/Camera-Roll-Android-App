package us.koller.cameraroll.data.FileOperations;

import android.content.Intent;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.File_POJO;

public class Move extends FileOperation {

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
            boolean result = moveFile(this, files[i].getPath(), target.getPath());
            success_count += result ? 1 : 0;
            onProgress(s, success_count, files.length);
        }

        if (success_count == 0) {
            onProgress(s, success_count, files.length);
        }
    }

    @Override
    public boolean autoSendDoneBroadcast() {
        return false;
    }

    @Override
    public int getType() {
        return FileOperation.MOVE;
    }

    private static boolean moveFile(final FileOperation fileOperation, String path, String destination) {
        String[] oldPaths = FileOperation.Util.getAllChildPaths(new ArrayList<String>(), path);

        File file = new File(path);
        File newFile = new File(destination, file.getName());
        boolean success = file.renameTo(newFile);

        //re-scan all paths
        String[] newPaths = FileOperation.Util.getAllChildPaths(new ArrayList<String>(), newFile.getPath());

        ArrayList<String> pathsList = new ArrayList<>();
        Collections.addAll(pathsList, oldPaths);
        Collections.addAll(pathsList, newPaths);

        String[] paths = new String[pathsList.size()];
        pathsList.toArray(paths);

        FileOperation.Util.scanPaths(fileOperation.getApplicationContext(), paths, new Util.MediaScannerCallback() {
            @Override
            public void onAllPathsScanned() {
                fileOperation.sendDoneBroadcast();
            }
        });

        return success;
    }
}
