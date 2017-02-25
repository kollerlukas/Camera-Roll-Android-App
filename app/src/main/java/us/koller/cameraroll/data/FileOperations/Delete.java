package us.koller.cameraroll.data.FileOperations;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.util.StorageUtil;

public class Delete extends FileOperation {

    public Delete(File_POJO[] files) {
        super(files);
    }

    @Override
    public void execute(Activity context, File_POJO target, Callback callback) {
        File_POJO[] files = getFiles();

        int success_count = 0;
        for (int i = 0; i < files.length; i++) {
            boolean result = deleteFile(context, files[i].getPath());
            if (result) {
                success_count++;
                /*context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.parse(files[i].getPath())));*/

                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
                        Uri.parse(files[i].getPath())));
            } else {
                if (callback != null) {
                    callback.failed(files[i].getPath());
                }
            }
        }

        Toast.makeText(context, context.getString(R.string.successfully_deleted)
                + String.valueOf(success_count) + "/"
                + String.valueOf(files.length), Toast.LENGTH_SHORT).show();

        if (callback != null) {
            callback.done();
        }

        operation = EMPTY;
    }

    private static boolean deleteFile(Activity context, String path) {
        boolean success = false;
        if (!path.startsWith("content")) {
            File file = new File(path);
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (int i = 0; i < files.length; i++) {
                    deleteFile(context, files[i].getPath());
                }
            }
            if (Environment.isExternalStorageRemovable(file)) {
                success = StorageUtil.delete(context, file);
            } else {
                success = file.exists() && file.delete();
            }
        }
        return success;
    }
}
