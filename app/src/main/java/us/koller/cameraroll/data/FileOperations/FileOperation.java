package us.koller.cameraroll.data.FileOperations;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

import java.io.File;

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
