package us.koller.cameraroll.data.FileOperations;

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
import java.util.Collections;

import us.koller.cameraroll.R;
import us.koller.cameraroll.themes.Theme;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.ui.BaseActivity;
import us.koller.cameraroll.util.DateTakenRetriever;

public class Rename extends FileOperation {

    public static final String NEW_FILE_PATH = "NEW_FILE_PATH";

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

    private static boolean renameFile(final FileOperation fileOperation, String path, String newFileName) {
        //keep old paths to remove them from MediaStore afterwards
        String[] oldPaths = FileOperation.Util.getAllChildPaths(new ArrayList<String>(), path);

        //try to get dateAdded TimeStamps for MediaStore
        Context context = fileOperation.getApplicationContext();
        //old paths are being removed --> no point in figuring out the timeStamps
        long[] dateAddedTimeStamps = new long[oldPaths.length * 2];
        for (int i = 0; i < oldPaths.length; i++) {
            if (i < oldPaths.length) {
                dateAddedTimeStamps[i] = -1;
            } else {
                AlbumItem albumItem = AlbumItem.getInstance(oldPaths[i - oldPaths.length]);
                DateTakenRetriever.tryToRetrieveDateTaken(context, albumItem);
                dateAddedTimeStamps[i] = albumItem.getDateTaken();
            }
        }

        File file = new File(path);
        final String newFilePath = getNewFilePath(path, newFileName);
        File newFile = new File(newFilePath);

        //renaming file
        boolean success = file.renameTo(newFile);

        //re-scan all paths
        String[] newPaths = FileOperation.Util.getAllChildPaths(new ArrayList<String>(), newFile.getPath());

        ArrayList<String> pathsList = new ArrayList<>();
        Collections.addAll(pathsList, oldPaths);
        Collections.addAll(pathsList, newPaths);

        String[] paths = new String[pathsList.size()];
        pathsList.toArray(paths);

        FileOperation.Util.scanPaths(fileOperation.getApplicationContext(), paths,
                dateAddedTimeStamps, new FileOperation.Util.MediaScannerCallback() {
                    @Override
                    public void onAllPathsScanned() {
                        Intent intent = fileOperation.getDoneIntent();
                        intent.putExtra(NEW_FILE_PATH, newFilePath);
                        fileOperation.sendLocalBroadcast(intent);
                    }
                });
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

            return new android.support.v7.app.AlertDialog.Builder(activity, theme.getDialogThemeRes())
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
