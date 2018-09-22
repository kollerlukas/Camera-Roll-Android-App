package us.koller.cameraroll.data.fileOperations;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.data.models.File_POJO;
import us.koller.cameraroll.themes.Theme;
import us.koller.cameraroll.ui.BaseActivity;
import us.koller.cameraroll.util.StorageUtil;

public class Rename extends FileOperation {

    public static final String NEW_FILE_PATH = "NEW_FILE_PATH";

    private String newFilePath;

    @Override
    String getNotificationTitle() {
        return getString(R.string.rename);
    }

    @Override
    public int getNotificationSmallIconRes() {
        return R.drawable.ic_text_format_white;
    }

    @Override
    public void execute(Intent workIntent) {
        final File_POJO[] files = getFiles(workIntent);
        final String newFileName = workIntent.getStringExtra(FileOperation.NEW_FILE_NAME);
        if (files.length > 0 && newFileName != null) {
            final File_POJO file = files[0];
            boolean result;
            if (FileOperation.Util.isOnRemovableStorage(file.getPath())) {
                //file is on removable storage
                Uri treeUri = getTreeUri(workIntent, file.getPath());
                if (treeUri == null) {
                    return;
                }
                result = renameFileRemovableStorage(getApplicationContext(), treeUri, file.getPath(), newFileName);
            } else {
                result = renameFile(file.getPath(), newFileName);
            }

            if (!result) {
                sendFailedBroadcast(workIntent, file.getPath());
            } else {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                        getString(R.string.successfully_renamed_file),
                        Toast.LENGTH_SHORT).show());
            }
        }
    }

    @Override
    public Intent getDoneIntent() {
        Intent intent = super.getDoneIntent();
        intent.putExtra(NEW_FILE_PATH, newFilePath);
        return intent;
    }

    @Override
    public int getType() {
        return FileOperation.RENAME;
    }

    private static String getFileExtension(String filename) {
        int index = filename.lastIndexOf(".");
        if (index != -1) {
            return filename.substring(index);
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

    private boolean renameFile(String path, String newFileName) {
        //keep old paths to remove them from MediaStore afterwards
        ArrayList<String> oldPaths = FileOperation.Util.getAllChildPaths(new ArrayList<>(), path);

        File file = new File(path);
        newFilePath = getNewFilePath(path, newFileName);
        File newFile = new File(newFilePath);

        //renaming file
        boolean success = file.renameTo(newFile);

        //re-scan all paths
        ArrayList<String> newPaths = FileOperation.Util.getAllChildPaths(new ArrayList<>(), newFile.getPath());
        addPathsToScan(oldPaths);
        addPathsToScan(newPaths);

        return success;
    }

    private boolean renameFileRemovableStorage(Context context, Uri treeUri, String path, String newFileName) {
        //keep old paths to remove them from MediaStore afterwards
        ArrayList<String> oldPaths = FileOperation.Util.getAllChildPaths(new ArrayList<>(), path);

        newFilePath = getNewFilePath(path, newFileName);
        boolean success = false;
        DocumentFile file = StorageUtil.parseDocumentFile(context, treeUri, new File(path));
        if (file != null) {
            success = file.renameTo(new File(newFilePath).getName());
        }

        //re-scan all paths
        ArrayList<String> newPaths = FileOperation.Util.getAllChildPaths(new ArrayList<>(), newFilePath);
        addPathsToScan(oldPaths);
        addPathsToScan(newPaths);
        return success;
    }

    public static class Util {

        public static AlertDialog getRenameDialog(final BaseActivity activity,
                                                  final File_POJO file,
                                                  final BroadcastReceiver broadcastReceiver) {

            Theme theme = Settings.getInstance(activity).getThemeInstance(activity);
            ContextThemeWrapper wrapper = new ContextThemeWrapper(activity, theme.getDialogThemeRes());

            @SuppressLint("InflateParams")
            View dialogLayout = LayoutInflater.from(wrapper)
                    .inflate(R.layout.input_dialog_layout, null, false);

            final EditText editText = dialogLayout.findViewById(R.id.edit_text);
            String name = file.getName();
            int index = name.lastIndexOf(".");
            //String fileExtension = name.substring(index, name.length());
            name = name.substring(0, index != -1 ? index : name.length());
            editText.setText(name);
            editText.setSelection(name.length());

            AlertDialog dialog = new AlertDialog.Builder(wrapper)
                    .setTitle(R.string.rename)
                    .setView(dialogLayout)
                    .setPositiveButton(R.string.rename, (dialogInterface, i) -> {
                        final String newFileName = editText.getText().toString();

                        if (broadcastReceiver != null) {
                            activity.registerLocalBroadcastReceiver(broadcastReceiver);
                        }

                        final File_POJO[] files = new File_POJO[]{file};
                        Intent intent = FileOperation.getDefaultIntent(activity, FileOperation.RENAME, files)
                                .putExtra(FileOperation.NEW_FILE_NAME, newFileName);
                        activity.startService(intent);
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create();
            //noinspection ConstantConditions
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            return dialog;
        }
    }
}
