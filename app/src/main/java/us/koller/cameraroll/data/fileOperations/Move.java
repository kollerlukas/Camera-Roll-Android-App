package us.koller.cameraroll.data.fileOperations;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.models.File_POJO;

public class Move extends FileOperation {

    public static final String MOVED_FILES_PATHS = "MOVED_FILES_PATHS";

    private ArrayList<String> movedFilePaths;

    @Override
    String getNotificationTitle() {
        return getString(R.string.move);
    }

    @Override
    public int getNotificationSmallIconRes() {
        return R.drawable.ic_folder_move_white;
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

        /*if (movingOntoRemovableStorage) {
            //failed = true;
            Uri treeUri = getTreeUri(workIntent, target.getPath());
            if (treeUri == null) {
                return;
            }
        } else {*/
        for (int i = files.length - 1; i >= 0; i--) {
            boolean movingFromRemovableStorage = Util.isOnRemovableStorage(files[i].getPath());

            boolean result;
            if (movingFromRemovableStorage || movingOntoRemovableStorage) {
                //failed = true;
                Uri treeUri;
                if (movingFromRemovableStorage) {
                    treeUri = getTreeUri(workIntent, files[i].getPath());
                } else {
                    treeUri = getTreeUri(workIntent, target.getPath());
                }

                if (treeUri == null) {
                    return;
                }
                result = copyAndDeleteFiles(getApplicationContext(), treeUri,
                        files[i].getPath(), target.getPath());
                //break;
            } else {
                result = moveFile(files[i].getPath(), target.getPath());
            }

            //boolean result = moveFile(files[i].getPath(), target.getPath());
            if (result) {
                movedFilePaths.add(files[i].getPath());
            }
            success_count += result ? 1 : 0;
            onProgress(success_count, files.length);
        }
        //}

        /*if (failed) {
            showRemovableStorageToast();
        } else */
        if (success_count == 0) {
            onProgress(success_count, files.length);
        }
    }

    @Override
    public int getType() {
        return FileOperation.MOVE;
    }

    private boolean moveFile(String path, String destination) {
        ArrayList<String> oldPaths = Util.getAllChildPaths(new ArrayList<>(), path);

        File file = new File(path);
        File newFile = new File(destination, file.getName());

        //moving file
        boolean success = renameFile(file, newFile);

        //re-scan all paths
        ArrayList<String> newPaths = Util.getAllChildPaths(new ArrayList<>(), newFile.getPath());
        addPathsToScan(oldPaths);
        addPathsToScan(newPaths);
        return success;
    }

    private boolean copyAndDeleteFiles(Context context, Uri treeUri,
                                       String path, String destination) {
        Copy copy = new Copy();
        boolean result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                Environment.isExternalStorageRemovable(new File(path))) {
            result = copy.copyFilesRecursively(context, null,
                    path, destination, true);
        } else {
            result = copy.copyFilesRecursively(context, treeUri,
                    path, destination, true);
        }
        addPathsToScan(copy.getPathsToScan());
        Log.d("Move", "copyAndDeleteFiles(): " + result);
        if (result) {
            Delete delete = new Delete();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    && Environment.isExternalStorageRemovable(new File(path))) {
                result = delete.deleteFileOnRemovableStorage(context, treeUri, path);
            } else {
                result = delete.deleteFile(path);
            }
            addPathsToScan(delete.getPathsToScan());
        }
        return result;
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
}
