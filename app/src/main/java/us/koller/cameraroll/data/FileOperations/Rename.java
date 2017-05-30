package us.koller.cameraroll.data.FileOperations;

import android.content.Intent;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.File_POJO;

public class Rename extends FileOperation {

    @Override
    void execute(Intent workIntent) {
        final File_POJO[] files = getFiles(workIntent);
        final String newFileName = workIntent.getStringExtra(FileOperation.NEW_FILE_NAME);
        if (files.length > 0 && newFileName != null) {
            final File_POJO file = files[0];
            boolean result = renameFile(this, file.getPath(), newFileName);
            if (!result) {
                sendFailedBroadcast(workIntent, file.getPath());
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), getString(R.string.successfully_renamed_file),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    @Override
    public boolean autoSendDoneBroadcast() {
        //sending broadcast after files are scanned
        return false;
    }

    @Override
    public int getType() {
        return FileOperation.RENAME;
    }

    private static String getFileExtension(String filename) {
        String[] pieces = filename.split(".");
        if (pieces.length > 1) {
            return pieces[pieces.length - 1];
        }
        return "";
    }

    public static String getNewFilePath(String path, String newFileName) {
        File file = new File(path);
        String fileExtension = getFileExtension(file.getName());
        String destination = file.getPath().replace(file.getName(), "");
        File newFile = new File(destination, newFileName + fileExtension);
        return newFile.getPath();
    }

    private static boolean renameFile(final FileOperation fileOperation, String path, String newFileName) {
        //keep old paths to remove them from MediaStore afterwards
        String[] oldPaths = FileOperation.Util.getAllChildPaths(new ArrayList<String>(), path);

        File file = new File(path);
        File newFile = new File(getNewFilePath(path, newFileName));
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
