package us.koller.cameraroll.data.FileOperations;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.util.MediaType;

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

    static void scanPaths(final Activity context, String[] paths) {
        MediaScannerConnection.scanFile(context,
                paths,
                null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        if (MediaType.isMedia_MimeType(context, path)) {
                            ContentResolver resolver = context.getContentResolver();
                            if (new File(path).exists()) {
                                //add File to media store
                                ContentValues values = new ContentValues();
                                values.put(MediaStore.MediaColumns.DATA, path);
                                if (MediaType.isImage(context, path)) {
                                    values.put(MediaStore.Images.Media.MIME_TYPE,
                                            MediaType.getMimeType(context, path));
                                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                                } else if (MediaType.isVideo(context, path)) {
                                    values.put(MediaStore.Video.Media.MIME_TYPE,
                                            MediaType.getMimeType(context, path));
                                    resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
                                }
                            } else {
                                //remove file from media store
                                String where = MediaStore.MediaColumns.DATA + "=?";
                                String[] selectionArgs = new String[]{path};

                                resolver.delete(MediaStore.Files.getContentUri("external"),
                                        where, selectionArgs);
                            }
                        }
                    }
                });
    }
}
