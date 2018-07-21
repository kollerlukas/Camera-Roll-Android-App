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

    private static boolean renameFile(File f, File nF) {
        //moving file
        return f.renameTo(nF);
    }

    @Override
    String getNotificationTitle() {
        return getString(R.string.move);
    }

    @Override
    public int getNotificationSmallIconRes() {
        return R.drawable.ic_folder_move_white;
    }

    @Override
    public void execute(Intent wI) {
        File_POJO[] files = getFiles(wI);
        File_POJO target = wI.getParcelableExtra(TARGET);
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
            if (treeUri == null) { return; }
        } else {*/
        for (int i = files.length - 1; i >= 0; i--) {
            boolean movingFromRemovableStorage = Util.isOnRemovableStorage(files[i].getPath());

            boolean result;
            if (movingFromRemovableStorage || movingOntoRemovableStorage) {
                //failed = true;
                Uri treeUri;
                if (movingFromRemovableStorage) {
                    treeUri = getTreeUri(wI, files[i].getPath());
                } else {
                    treeUri = getTreeUri(wI, target.getPath());
                }

                if (treeUri == null) {
                    return;
                }
                result = copyAndDeleteFiles(getApplicationContext(), treeUri, files[i].getPath(), target.getPath());
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
        /*if (failed) { showRemovableStorageToast();} else */
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
        File f = new File(path);
        File nF = new File(destination, f.getName());

        //moving file
        boolean success = renameFile(f, nF);

        //re-scan all paths
        ArrayList<String> newPaths = Util.getAllChildPaths(new ArrayList<>(), nF.getPath());
        addPathsToScan(oldPaths);
        addPathsToScan(newPaths);
        return success;
    }

    private boolean copyAndDeleteFiles(Context context, Uri treeUri, String path, String destination) {
        Copy c = new Copy();
        boolean result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                Environment.isExternalStorageRemovable(new File(path))) {
            result = c.copyFilesRecursively(context, null, path, destination, true);
        } else {
            result = c.copyFilesRecursively(context, treeUri, path, destination, true);
        }
        addPathsToScan(c.getPathsToScan());
        Log.d("Move", "copyAndDeleteFiles(): " + result);
        if (result) {
            Delete d = new Delete();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    && Environment.isExternalStorageRemovable(new File(path))) {
                result = d.deleteFileOnRemovableStorage(context, treeUri, path);
            } else {
                result = d.deleteFile(path);
            }
            addPathsToScan(d.getPathsToScan());
        }
        return result;
    }

    @Override
    public Intent getDoneIntent() {
        Intent i = super.getDoneIntent();
        i.putExtra(MOVED_FILES_PATHS, movedFilePaths);
        return i;
    }
}
