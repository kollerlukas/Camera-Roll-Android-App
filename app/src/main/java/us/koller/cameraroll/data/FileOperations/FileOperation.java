package us.koller.cameraroll.data.FileOperations;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Arrays;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.util.MediaType;

public abstract class FileOperation extends IntentService implements Parcelable {

    public static final String RESULT_DONE = "us.koller.cameraroll.data.FileOperations.FileOperation.RESULT_DONE";
    public static final String FAILED = "us.koller.cameraroll.data.FileOperations.FileOperation.FAILED";

    public static final int EMPTY = 0;
    public static final int MOVE = 1;
    public static final int COPY = 2;
    public static final int DELETE = 3;
    public static final int NEW_DIR = 4;

    public static final String WORK_INTENT = "WORK_INTENT";
    public static final String FILES = "FILES";
    public static final String TARGET = "TARGET";

    private ProgressUpdater updater;

    public FileOperation() {
        super("");
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setUpdater(new ToastUpdater(getApplicationContext()));
            }
        });

        execute(workIntent);

        sendDoneBroadcast(workIntent);

        stopSelf();
    }

    abstract void execute(Intent workIntent);

    private void sendDoneBroadcast(Intent workIntent) {
        Intent intent = new Intent(RESULT_DONE);
        intent.putExtra(WORK_INTENT, workIntent);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void sendFailedBroadcast(Intent workIntent, String path) {
        Intent intent = new Intent(FAILED);
        intent.putExtra(FILES, path);
        intent.putExtra(WORK_INTENT, workIntent);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public abstract int getType();

    public void setUpdater(ProgressUpdater updater) {
        this.updater = updater;
    }

    public void onProgress(final String action, final int progress, final int totalNumber) {
        if (updater != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updater.onProgress(action, progress, totalNumber);
                }
            });
        }
    }

    public void runOnUiThread(Runnable r) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(r);
    }

    public static File_POJO[] getFiles(Intent workIntent) {
        Parcelable[] parcelables = workIntent.getParcelableArrayExtra(FILES);
        return File_POJO.generateArray(parcelables);
    }

    public static Intent getDefaultIntent(Context context, int action, File_POJO[] files) {
        String actionString = Util.getActionString(context, EMPTY);
        Class service = null;
        switch (action) {
            case COPY:
                service = Copy.class;
                actionString = Util.getActionString(context, COPY);
                break;
            case MOVE:
                service = Move.class;
                actionString = Util.getActionString(context, MOVE);
                break;
            case DELETE:
                service = Delete.class;
                actionString = Util.getActionString(context, DELETE);
                break;
            case NEW_DIR:
                service = NewDirectory.class;
                actionString = Util.getActionString(context, NEW_DIR);
                break;
        }
        if (service != null) {
            return new Intent(context, service)
                    .setAction(actionString)
                    .putExtra(FileOperation.FILES, files);
        }
        return new Intent();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(getType());
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public FileOperation createFromParcel(Parcel parcel) {
            switch (parcel.readInt()) {
                case MOVE:
                    return new Move();
                case COPY:
                    return new Copy();
                default:
                    return new Delete();
            }
        }

        public AlbumItem[] newArray(int i) {
            return new AlbumItem[i];
        }
    };


    public interface ProgressUpdater {
        void onProgress(String action, int progress, int totalNumber);
    }

    public static class ToastUpdater implements ProgressUpdater {

        private Toast toast;

        private Handler handler;

        @SuppressLint("ShowToast")
        ToastUpdater(Context context) {
            handler = new Handler(Looper.getMainLooper());

            if (toast == null) {
                toast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
            }
        }

        @Override
        public void onProgress(String action, int progress, int totalNumber) {
            final String text = action + String.valueOf(progress) + "/"
                    + String.valueOf(totalNumber);
            if (toast != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        toast.setText(text);
                        toast.show();
                    }
                });
            }
        }
    }


    public static class Util {
        public static IntentFilter getIntentFilter() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(FileOperation.RESULT_DONE);
            filter.addAction(FileOperation.FAILED);
            return filter;
        }

        public static String getActionString(Context context, int type) {
            switch (type) {
                case EMPTY:
                    return "empty";
                case MOVE:
                    return context.getString(R.string.move);
                case COPY:
                    return context.getString(R.string.copy);
                case DELETE:
                    return context.getString(R.string.delete);
                case NEW_DIR:
                    return context.getString(R.string.new_folder);
            }
            return "";
        }

        public static int getActionInt(Context context, String action) {
            if (action.equals(context.getString(R.string.move))) {
                return MOVE;
            } else if (action.equals(context.getString(R.string.copy))) {
                return COPY;
            } else if (action.equals(context.getString(R.string.delete))) {
                return DELETE;
            }
            return EMPTY;
        }

        static void scanPaths(final Context context, String[] paths) {
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
}
