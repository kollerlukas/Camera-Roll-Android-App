package us.koller.cameraroll.data.fileOperations;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.models.File_POJO;

public class Move extends FileOperation {

    public static final String MOVED_FILES_PATHS = "MOVED_FILES_PATHS";

    private ArrayList<String> movedFilePaths;

    @Override
    public void execute(Intent workIntent) {
        File_POJO[] files = getFiles(workIntent);
        File_POJO target = workIntent.getParcelableExtra(TARGET);

        movedFilePaths = new ArrayList<>();

        if (target == null) {
            return;
        }

        String s = getString(R.string.successfully_moved);

        int success_count = 0;

        onProgress(s, success_count, files.length);

        //check if file is on removable storage
        boolean movingOntoRemovableStorage = Util.isOnRemovableStorage(target.getPath());

        Uri treeUri = null;
        if (movingOntoRemovableStorage) {
            treeUri = getTreeUri(workIntent, target.getPath());
            if (treeUri == null) {
                return;
            }
        }

        for (int i = files.length - 1; i >= 0; i--) {
            boolean movingFromRemovableStorage = Util.isOnRemovableStorage(files[i].getPath());

            if (treeUri == null && movingFromRemovableStorage) {
                treeUri = getTreeUri(workIntent, files[i].getPath());
                if (treeUri == null) {
                    return;
                }
            }

            boolean result = moveFile(this, /*treeUri,*/ files[i].getPath(), target.getPath());
            if (result) {
                movedFilePaths.add(files[i].getPath());
            }
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

    private boolean moveFile(Context context, /*Uri treeUri,*/ String path, String destination) {
        ArrayList<String> oldPaths = Util.getAllChildPaths(new ArrayList<String>(), path);

        File file = new File(path);
        File newFile = new File(destination, file.getName());

        //moving file
        boolean success;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!Util.isOnRemovableStorage(file.getPath())
                    && !Util.isOnRemovableStorage(newFile.getPath())) {
                Log.d("Move", "renameFile()");
                success = renameFile(file, newFile);
            } else {
                Log.d("Move", "renameFileRemovableStorage()");
                success = renameFileRemovableStorage(context/*, treeUri, file, newFile*/);
            }
        } else {
            success = renameFile(file, newFile);
        }

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

    private static boolean renameFileRemovableStorage(Context context/*, Uri treeUri, File file, File newFile*/) {
        //TODO implement
        Toast.makeText(context, "Moving files to/from removable Storage is currently not supported. Please just copy and delete the file", Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public Intent getDoneIntent() {
        Intent intent = super.getDoneIntent();
        intent.putExtra(MOVED_FILES_PATHS, movedFilePaths);
        return intent;
    }
}
