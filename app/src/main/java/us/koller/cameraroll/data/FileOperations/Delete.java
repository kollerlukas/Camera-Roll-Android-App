package us.koller.cameraroll.data.FileOperations;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.StorageUtil;

public class Delete extends FileOperation {

    public Delete(File_POJO[] files) {
        super(files);
    }

    @Override
    void executeAsync(final Activity context, File_POJO target, final Callback callback) {
        final File_POJO[] files = getFiles();

        String s = context.getString(R.string.successfully_deleted);

        int success_count = 0;
        for (int i = 0; i < files.length; i++) {
            boolean result = deleteFile(context, files[i].getPath());
            if (result) {
                success_count++;
                setToastProgress(context, s, success_count);
            } else {
                if (callback != null) {
                    final int final_i = i;
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.failed(files[final_i].getPath());
                        }
                    });
                }
            }
        }

        if (success_count == 0) {
            setToastProgress(context, s, success_count);
        }

        if (callback != null) {
            callback.done();
        }

        operation = EMPTY;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public int getType() {
        return FileOperation.DELETE;
    }

    private static boolean deleteFile(final Activity context, String path) {
        boolean success;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && Environment.isExternalStorageRemovable(new File(path))) {
            //not working
            try {
                success = StorageUtil.delete(context, new File(path));
            } catch (IOException e) {
                success = false;
                e.printStackTrace();
            }
        } else if (MediaType.isMedia_MimeType(context, path)) {
            Log.d("Delete", "deleteFile: ContentResolver");
            ContentResolver resolver = context.getContentResolver();

            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DATA, path);
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            String where = MediaStore.MediaColumns.DATA + "=?";
            String[] selectionArgs = new String[]{path};

            int rows = resolver.delete(MediaStore.Files.getContentUri("external"),
                    where, selectionArgs);

            success = rows > 0;
        } else {
            Log.d("Delete", "deleteFile: java.io.File");
            File file = new File(path);
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (int i = 0; i < files.length; i++) {
                    deleteFile(context, files[i].getPath());
                }
            }
            success = file.exists() && file.delete();
        }
        return success;
    }
}
