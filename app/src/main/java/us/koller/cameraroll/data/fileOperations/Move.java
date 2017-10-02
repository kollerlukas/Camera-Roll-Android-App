package us.koller.cameraroll.data.fileOperations;

import android.content.Intent;

import java.io.File;
import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.models.File_POJO;

public class Move extends FileOperation {

    public static final String MOVED_FILES_PATHS = "MOVED_FILES_PATHS";

    private ArrayList<String> movedFilePaths;

    private boolean failed = false;

    @Override
    String getNotificationTitle() {
        return getString(R.string.move);
    }

    @Override
    public int getNotificationSmallIconRes() {
        return R.drawable.ic_folder_move_white_24dp;
    }

    @Override
    public void execute(Intent workIntent) {
        File_POJO[] files = getFiles(workIntent);
        File_POJO target = workIntent.getParcelableExtra(TARGET);

        movedFilePaths = new ArrayList<>();

        if (target == null) {
            return;
        }

        int success_count = 0;

        onProgress(success_count, files.length);

        //check if file is on removable storage
        boolean movingOntoRemovableStorage = Util.isOnRemovableStorage(target.getPath());

        if (movingOntoRemovableStorage) {
            failed = true;
        } else {
            for (int i = files.length - 1; i >= 0; i--) {
                boolean movingFromRemovableStorage = Util.isOnRemovableStorage(files[i].getPath());

                if (movingFromRemovableStorage) {
                    failed = true;
                    break;
                }

                boolean result = moveFile(files[i].getPath(), target.getPath());
                if (result) {
                    movedFilePaths.add(files[i].getPath());
                }
                success_count += result ? 1 : 0;
                onProgress(success_count, files.length);
            }
        }

        if (failed) {
            showRemovableStorageToast();
        } else if (success_count == 0) {
            onProgress(success_count, files.length);
        }
    }

    @Override
    public int getType() {
        return FileOperation.MOVE;
    }

    private boolean moveFile(String path, String destination) {
        ArrayList<String> oldPaths = Util.getAllChildPaths(new ArrayList<String>(), path);

        File file = new File(path);
        File newFile = new File(destination, file.getName());

        //moving file
        boolean success = renameFile(file, newFile);

        //re-scan all paths
        ArrayList<String> newPaths = Util.getAllChildPaths(new ArrayList<String>(), newFile.getPath());
        addPathsToScan(oldPaths);
        addPathsToScan(newPaths);
        return success;
    }

    private static boolean renameFile(File file, File newFile) {
        //moving file
        return file.renameTo(newFile);
    }

    @Override
    public Intent getDoneIntent() {
        Intent intent = super.getDoneIntent();
        intent.putExtra(MOVED_FILES_PATHS, movedFilePaths);
        return intent;
    }

    private void showRemovableStorageToast() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String message = getString(R.string.move_error);
                showToast(message);
            }
        });
    }

    @Override
    public boolean autoSendDoneBroadcast() {
        return !failed;
    }
}
