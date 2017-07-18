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
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.StorageUtil;

public abstract class FileOperation extends IntentService implements Parcelable {

    public static final String RESULT_DONE = "us.koller.cameraroll.data.FileOperations.FileOperation.RESULT_DONE";
    public static final String FAILED = "us.koller.cameraroll.data.FileOperations.FileOperation.FAILED";
    public static final String NEED_REMOVABLE_STORAGE_PERMISSION = "us.koller.cameraroll.data.FileOperations.FileOperation.NEED_REMOVABLE_STORAGE_PERMISSION";

    public static final int EMPTY = 0;
    public static final int MOVE = 1;
    public static final int COPY = 2;
    public static final int DELETE = 3;
    public static final int NEW_DIR = 4;
    public static final int RENAME = 5;

    public static final String WORK_INTENT = "WORK_INTENT";
    public static final String FILES = "FILES";
    public static final String TARGET = "TARGET";
    public static final String NEW_FILE_NAME = "NEW_FILE_NAME";
    public static final String REMOVABLE_STORAGE_TREE_URI = "REMOVABLE_STORAGE_TREE_URI";

    private ProgressUpdater updater;

    public FileOperation() {
        super("");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setUpdater(new ToastUpdater(getApplicationContext()));
            }
        });
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        execute(workIntent);

        if (autoSendDoneBroadcast()) {
            sendDoneBroadcast();
        }
    }

    abstract void execute(Intent workIntent);

    Uri getTreeUri(Intent workIntent, String path) {
        Log.d("FileOperation", "getTreeUri");
        Uri treeUri;
        String treeUriExtra = workIntent.getStringExtra(FileOperation.REMOVABLE_STORAGE_TREE_URI);
        if (treeUriExtra != null) {
            treeUri = Uri.parse(treeUriExtra);
        } else {
            Settings s = Settings.getInstance(getApplicationContext());
            treeUri = s.getRemovableStorageTreeUri();
        }

        if (path != null) {
            //check if path is child of the treeUri
            DocumentFile file = StorageUtil.parseDocumentFile(getApplicationContext(), treeUri, path);
            Log.d("FileOperation", "DocumentFile: " + file);
            if (file != null) {
                return treeUri;
            } else {
                requestPermissionForRemovableStorageBroadcast(workIntent);
            }
        } else {
            return treeUri;
        }
        return null;
    }

    public boolean autoSendDoneBroadcast() {
        return true;
    }

    public void sendDoneBroadcast() {
        Intent intent = getDoneIntent();
        sendLocalBroadcast(intent);
    }

    public void sendFailedBroadcast(Intent workIntent, String path) {
        Intent intent = new Intent(FAILED);
        intent.putExtra(FILES, path);
        intent.putExtra(WORK_INTENT, workIntent);
        sendLocalBroadcast(intent);
    }

    public void requestPermissionForRemovableStorageBroadcast(Intent workIntent) {
        Intent intent = new Intent(NEED_REMOVABLE_STORAGE_PERMISSION);
        intent.putExtra(WORK_INTENT, workIntent);
        sendLocalBroadcast(intent);
    }

    public Intent getDoneIntent() {
        return new Intent(RESULT_DONE);
    }

    public void sendLocalBroadcast(Intent intent) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public abstract int getType();

    @Override
    public int describeContents() {
        return 0;
    }

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
            case RENAME:
                service = Rename.class;
                actionString = Util.getActionString(context, RENAME);
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


    interface ProgressUpdater {
        void onProgress(String action, int progress, int totalNumber);
    }

    private static class ToastUpdater implements ProgressUpdater {

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
        public static IntentFilter getIntentFilter(IntentFilter filter) {
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
                case RENAME:
                    return context.getString(R.string.rename);
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

        static boolean isOnRemovableStorage(String path) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                File file = new File(path);
                if (file.exists() && Environment.isExternalStorageRemovable(file)) {
                    return true;
                }
            }
            return false;
        }

        static String[] getAllChildPaths(ArrayList<String> paths, String path) {
            File file = new File(path);
            if (file.exists()) {
                if (file.isDirectory()) {
                    File[] children = file.listFiles();
                    for (int i = 0; i < children.length; i++) {
                        getAllChildPaths(paths, children[i].getPath());
                    }
                } else {
                    paths.add(path);
                }
            }
            String[] pathsArray = new String[paths.size()];
            paths.toArray(pathsArray);
            return pathsArray;
        }

        interface MediaScannerCallback {
            void onAllPathsScanned();
        }

        static void scanPaths(final Context context, final String[] paths,
                              final long[] dateTakenTimeStamps, final MediaScannerCallback callback) {
            Log.d("FileOperation", "scanPaths(), paths = [" + Arrays.toString(paths) + "]");
            MediaScannerConnection.scanFile(context.getApplicationContext(),
                    paths,
                    null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        int pathsScanned;

                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            pathsScanned++;

                            if (MediaType.isMedia(path)) {
                                ContentResolver resolver = context.getContentResolver();
                                if (new File(path).exists()) {
                                    //add File to media store
                                    Log.d("FileOperation", "add File to media store: " + path);

                                    //trying to transfer dateTakenTimeStamp from old media (when file was copied or moved)
                                    int index = -1;
                                    for (int i = 0; i < paths.length; i++) {
                                        if (path.equals(paths[i])) {
                                            index = i;
                                            break;
                                        }
                                    }

                                    long dateTaken = -1;
                                    if (index != -1) {
                                        dateTaken = dateTakenTimeStamps[index];
                                    }

                                    ContentValues values = new ContentValues();
                                    values.put(MediaStore.MediaColumns.DATA, path);
                                    if (MediaType.isImage(path)) {
                                        //add image
                                        values.put(MediaStore.Images.Media.MIME_TYPE, MediaType.getMimeType(context, path));
                                        if (dateTaken != -1) {
                                            values.put(MediaStore.Images.Media.DATE_TAKEN, dateTaken);
                                        }
                                        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                                    } else if (MediaType.isVideo(path)) {
                                        //add video
                                        values.put(MediaStore.Video.Media.MIME_TYPE, MediaType.getMimeType(context, path));
                                        if (dateTaken != -1) {
                                            values.put(MediaStore.Video.Media.DATE_TAKEN, dateTaken);
                                        }
                                        resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
                                    }
                                } else {
                                    Log.d("FileOperation", "remove File to media store: " + path);
                                    //remove file from media store
                                    String where = MediaStore.MediaColumns.DATA + "=?";
                                    String[] selectionArgs = new String[]{path};

                                    resolver.delete(MediaStore.Files.getContentUri("external"), where, selectionArgs);
                                }
                            }

                            if (callback != null && pathsScanned == paths.length) {
                                callback.onAllPathsScanned();
                            }
                        }
                    });
        }
    }
}
