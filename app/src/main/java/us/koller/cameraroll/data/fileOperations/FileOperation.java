package us.koller.cameraroll.data.fileOperations;

import android.annotation.SuppressLint;
import android.app.IntentService;
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
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.data.Settings;
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

    private ArrayList<String> pathsToScan;

    public FileOperation() {
        super("");

        pathsToScan = new ArrayList<>();

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
            if (pathsToScan.size() > 0) {
                scanPaths(getApplicationContext(), new Util.MediaScannerCallback() {
                    @Override
                    public void onAllPathsScanned() {
                        sendDoneBroadcast();
                    }
                });
            } else {
                sendDoneBroadcast();
            }
        }
    }

    public abstract void execute(Intent workIntent);

    public void addPathToScan(String path) {
        pathsToScan.add(path);
    }

    public void addPathsToScan(List<String> paths) {
        pathsToScan.addAll(paths);
    }

    public boolean autoSendDoneBroadcast() {
        return true;
    }

    public void sendDoneBroadcast() {
        onProgress("", -1, -1);
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
            default:
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
                case DELETE:
                    return new Delete();
                case NEW_DIR:
                    return new NewDirectory();
                case RENAME:
                    return new Rename();
                default:
                    return null;
            }
        }

        public AlbumItem[] newArray(int i) {
            return new AlbumItem[i];
        }
    };


    public Uri getTreeUri(Intent workIntent, String path) {
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
        public void onProgress(String action, final int progress, final int totalNumber) {
            final String text = action + String.valueOf(progress) + "/"
                    + String.valueOf(totalNumber);
            if (toast != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (progress == totalNumber) {
                            toast.setText(R.string.done);
                        } else {
                            toast.setText(text);
                        }
                        toast.show();
                    }
                });
            }
        }
    }

    void scanPaths(Context context, Util.MediaScannerCallback callback) {
        String[] paths = new String[pathsToScan.size()];
        pathsToScan.toArray(paths);
        Util.scanPaths(context, paths, callback);
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
                default:
                    break;
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

        public static boolean isOnRemovableStorage(String path) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                File file = new File(path);
                try {
                    if (file.exists() && Environment.isExternalStorageRemovable(file)) {
                        return true;
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }

        static ArrayList<String> getAllChildPaths(ArrayList<String> paths, String path) {
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
            return paths;
        }

        public interface MediaScannerCallback {
            void onAllPathsScanned();
        }

        public static void scanPaths(final Context context, final String[] paths, final MediaScannerCallback callback) {
            MediaScannerConnection.scanFile(context.getApplicationContext(),
                    paths,
                    null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        int pathsScanned;

                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            Log.d("FileOperation", "onScanCompleted() called with: path = [" + path + "]");

                            pathsScanned++;
                            if (callback != null && pathsScanned == paths.length) {
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onAllPathsScanned();
                                    }
                                }, 100);
                            }
                        }
                    });
        }
    }
}
