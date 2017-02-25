package us.koller.cameraroll.data.FileOperations;

import android.app.Activity;
import android.content.Context;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.File_POJO;

public abstract class FileOperation {

    public interface Callback {
        void done();

        void failed(String path);
    }

    public static final int EMPTY = 0;
    public static final int MOVE = 1;
    public static final int COPY = 2;
    public static final int DELETE = 3;

    public static int operation = EMPTY;

    private File_POJO[] files;

    FileOperation(File_POJO[] files) {
        this.files = files;
    }

    public File_POJO[] getFiles() {
        return files;
    }

    public abstract void execute(final Activity context, final File_POJO target, final Callback callback);

    public static String getModeString(Context context) {
        switch (operation) {
            case EMPTY:
                return "empty";
            case MOVE:
                return context.getString(R.string.move);
            case COPY:
                return context.getString(R.string.copy);
            case DELETE:
                return context.getString(R.string.delete);
        }
        return "";
    }
}
