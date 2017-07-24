package us.koller.cameraroll.data.fileOperations;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.themes.Theme;
import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.ui.BaseActivity;

public class Rename extends FileOperation {

    public static final String NEW_FILE_PATH = "NEW_FILE_PATH";

    @Override
    public void execute(Intent workIntent) {
        final File_POJO[] files = getFiles(workIntent);
        final String newFileName = workIntent.getStringExtra(FileOperation.NEW_FILE_NAME);
        if (files.length > 0 && newFileName != null) {
            final File_POJO file = files[0];
            boolean result = renameFile(file.getPath(), newFileName);
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
        ArrayList<String> oldPaths = FileOperation.Util.getAllChildPaths(new ArrayList<String>(), path);

        File file = new File(path);
        final String newFilePath = getNewFilePath(path, newFileName);
        File newFile = new File(newFilePath);

        //renaming file
        boolean success = file.renameTo(newFile);

        //re-scan all paths
        ArrayList<String> newPaths = FileOperation.Util.getAllChildPaths(new ArrayList<String>(), newFile.getPath());
        addPathsToScan(oldPaths);
        addPathsToScan(newPaths);

        return success;
    }


    public static class Util {

        public static AlertDialog getRenameDialog(final BaseActivity activity,
                                                  final File_POJO file,
                                                  final BroadcastReceiver broadcastReceiver) {

            View dialogLayout = LayoutInflater.from(activity).inflate(R.layout.input_dialog_layout,
                    (ViewGroup) activity.findViewById(R.id.root_view), false);

            final EditText editText = dialogLayout.findViewById(R.id.edit_text);
            String name = file.getName();
            int index = name.lastIndexOf(".");
            name = name.substring(0, index != -1 ? index : name.length());
            editText.setText(name);
            editText.setSelection(name.length());

            Theme theme = Settings.getInstance(activity).getThemeInstance(activity);

            return new AlertDialog.Builder(activity, theme.getDialogThemeRes())
                    .setTitle(R.string.rename)
                    .setView(dialogLayout)
                    .setPositiveButton(R.string.rename, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            final String newFileName = editText.getText().toString();

                            if (broadcastReceiver != null) {
                                activity.registerLocalBroadcastReceiver(new BroadcastReceiver() {
                                    @Override
                                    public void onReceive(Context context, Intent intent) {
                                        activity.unregisterLocalBroadcastReceiver(this);
                                        broadcastReceiver.onReceive(context, intent);
                                    }
                                });
                            }

                            final File_POJO[] files = new File_POJO[]{file};
                            Intent intent =
                                    FileOperation.getDefaultIntent(activity, FileOperation.RENAME, files)
                                            .putExtra(FileOperation.NEW_FILE_NAME, newFileName);
                            activity.startService(intent);
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create();
        }
    }
}
