package us.koller.cameraroll.data.FileOperations;

import android.app.Activity;
import android.widget.Toast;

import java.io.File;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.File_POJO;

public class NewDirectory extends FileOperation {

    public NewDirectory(File_POJO[] files) {
        super(files);
    }

    @Override
    void executeAsync(final Activity context, File_POJO target) {
        final File_POJO[] files = getFiles();
        if (files.length > 0) {
            final File_POJO file = files[0];
            boolean result = createNewFolder(file.getPath());
            if (!result) {
                if (callback != null) {
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.failed(file.getPath());
                        }
                    });
                }
            }

            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, context.getString(R.string.successfully_created_new_folder),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.done();
                }
            }
        });

        operation = EMPTY;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public int getType() {
        return FileOperation.NEW_DIR;
    }

    private static boolean createNewFolder(String newFolderPath) {
        File dir = new File(newFolderPath);
        return !dir.exists() && dir.mkdirs();
    }
}
