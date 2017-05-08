package us.koller.cameraroll.data.FileOperations;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Arrays;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.data.Gif;
import us.koller.cameraroll.data.Photo;
import us.koller.cameraroll.data.Video;
import us.koller.cameraroll.util.MediaType;

public abstract class FileOperation implements Parcelable {

    public interface Callback {
        void done();

        void failed(String path);
    }

    public static final int EMPTY = 0;
    public static final int MOVE = 1;
    public static final int COPY = 2;
    public static final int DELETE = 3;
    public static final int NEW_DIR = 4;

    public static int operation = EMPTY;

    private File_POJO[] files;

    private WeakReference<Toast> toastWeakReference;

    FileOperation(File_POJO[] files) {
        this.files = files;
    }

    public File_POJO[] getFiles() {
        return files;
    }

    public void execute(final Activity context, final File_POJO target, final Callback callback) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                executeAsync(context, target, callback);
            }
        });
    }

    abstract void executeAsync(final Activity context, final File_POJO target, final Callback callback);

    public abstract int getType();


    @SuppressLint("ShowToast")
    void setToastProgress(final Activity context, final String s, final int successCount) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (toastWeakReference == null) {
                    toastWeakReference = new WeakReference<>(Toast.makeText(context, "", Toast.LENGTH_SHORT));
                }

                final String text = s + String.valueOf(successCount) + "/"
                        + String.valueOf(files.length);

                Toast toast = toastWeakReference.get();
                if (toast != null) {
                    toast.setText(text);
                    toast.show();
                }
            }
        });
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(getType());
        parcel.writeParcelableArray(files, 0);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public FileOperation createFromParcel(Parcel parcel) {
            File_POJO[] files = (File_POJO[])
                    parcel.readParcelableArray(File_POJO.class.getClassLoader());
            switch (parcel.readInt()) {
                case MOVE:
                    return new Move(files);
                case COPY:
                    return new Copy(files);
                default:
                    return new Delete(files);
            }
        }

        public AlbumItem[] newArray(int i) {
            return new AlbumItem[i];
        }
    };



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
        Log.d("FileOperation", "scanPaths(), paths = [" + Arrays.deepToString(paths) + "]");
        MediaScannerConnection.scanFile(context.getApplicationContext(),
                paths,
                null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        Log.d("FileOperation", "onScanCompleted: " + path);
                        if (MediaType.isMedia_MimeType(context, path)) {
                            ContentResolver resolver = context.getContentResolver();
                            if (new File(path).exists()) {
                                Log.d("FileOperation", "add File to media store: " + path);
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
                                Log.d("FileOperation", "remove File to media store: " + path);
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
