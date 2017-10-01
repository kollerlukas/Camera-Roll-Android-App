package us.koller.cameraroll.data.fileOperations;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.ContentObserver;
import us.koller.cameraroll.data.models.AlbumItem;
import us.koller.cameraroll.data.models.File_POJO;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.data.models.Video;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.StorageUtil;

public abstract class FileOperation extends IntentService implements Parcelable {

    private static final int NOTIFICATION_ID = 6;

    public static final String RESULT_DONE = "us.koller.cameraroll.data.FileOperations.FileOperation.RESULT_DONE";
    public static final String FAILED = "us.koller.cameraroll.data.FileOperations.FileOperation.FAILED";
    public static final String NEED_REMOVABLE_STORAGE_PERMISSION = "us.koller.cameraroll.data.FileOperations.FileOperation.NEED_REMOVABLE_STORAGE_PERMISSION";
    public static final String TYPE = "TYPE";

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

    private NotificationCompat.Builder notifBuilder;

    private ArrayList<String> pathsToScan;

    public FileOperation() {
        super("");

        pathsToScan = new ArrayList<>();
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        notifBuilder = createNotificationBuilder();
        notifBuilder.setProgress(1, 0, false);
        Notification notification = notifBuilder.build();
        startForeground(NOTIFICATION_ID, notification);
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, notification);

        ContentObserver.selfChange = true;

        execute(workIntent);

        if (autoSendDoneBroadcast()) {
            if (pathsToScan.size() > 0) {
                onProgress(-1, -1);
                scanPaths(getApplicationContext(), new Util.MediaScannerCallback() {
                    @Override
                    public void onAllPathsScanned() {
                        sendDoneBroadcast();
                        stopForeground(true);

                    }
                });
            } else {
                sendDoneBroadcast();
                stopForeground(true);
            }
        } else {
            ContentObserver.selfChange = false;
            stopForeground(true);
        }
    }

    private NotificationCompat.Builder createNotificationBuilder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
        return new NotificationCompat.Builder(this, getString(R.string.file_op_channel_id))
                .setContentTitle(getNotificationTitle())
                .setSmallIcon(getNotificationSmallIconRes());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel mChannel = new NotificationChannel(
                getString(R.string.file_op_channel_id),
                getString(R.string.file_op_channel_name),
                NotificationManager.IMPORTANCE_LOW);
        mChannel.setDescription(getString(R.string.file_op_channel_description));
        mNotificationManager.createNotificationChannel(mChannel);
    }

    private NotificationCompat.Builder getNotificationBuilder() {
        return notifBuilder;
    }

    abstract String getNotificationTitle();

    public abstract int getNotificationSmallIconRes();

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
        ContentObserver.selfChange = false;
        showToast(getString(R.string.done));
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
        Intent intent = new Intent(RESULT_DONE);
        intent.putExtra(TYPE, this.getType());
        return intent;
    }

    public void sendLocalBroadcast(Intent intent) {
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    public abstract int getType();

    @Override
    public int describeContents() {
        return 0;
    }

    @SuppressWarnings("unused")
    public void onProgress(final int progress, final int totalNumber) {
        NotificationCompat.Builder notifBuilder = getNotificationBuilder();
        /*if (progress >= 0) {
            notifBuilder.setProgress(totalNumber, progress, false);
        } else {
            notifBuilder.setProgress(0, 0, true);
        }*/
        notifBuilder.setProgress(0, 0, true);
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, notifBuilder.build());
    }

    public void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
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
            DocumentFile file = StorageUtil.parseDocumentFile(getApplicationContext(), treeUri, new File(path));
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

        public static ArrayList<String> getAllChildPaths(ArrayList<String> paths, String path) {
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
            scanPaths(context, paths, callback, false);
        }

        public static void scanPathsWithToast(final Context context, final String[] paths) {
            scanPaths(context, paths, null, true);
        }

        @SuppressLint("ShowToast")
        private static void scanPaths(final Context context, final String[] paths, final MediaScannerCallback callback, final boolean showToast) {
            if (paths == null) {
                if (callback != null) {
                    callback.onAllPathsScanned();
                }
                return;
            }

            final WeakReference<Toast> toastWeakReference;
            if (showToast) {
                toastWeakReference = new WeakReference<>(
                        Toast.makeText(context, R.string.scanning, Toast.LENGTH_SHORT));
            } else {
                toastWeakReference = null;
            }

            String[] mimeTypes = new String[paths.length];
            for (int i = 0; i < paths.length; i++) {
                mimeTypes[i] = MediaType.getMimeType(paths[i]);
            }

            MediaScannerConnection.scanFile(context.getApplicationContext(),
                    paths, mimeTypes,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        int pathsScanned;

                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            if (uri == null) {
                                Log.i("FileOperation", "MediaScannerConnection.scanFile() !FAILED! path = [" + path + "]");
                                if (new File(path).exists()) {
                                    AlbumItem albumItem = AlbumItem.getInstance(path);
                                    ContentValues values = new ContentValues();
                                    if (albumItem instanceof Video) {
                                        values.put(MediaStore.Video.Media.DATA, path);
                                        values.put(MediaStore.Video.Media.MIME_TYPE, MediaType.getMimeType(path));
                                    } else {
                                        values.put(MediaStore.Images.Media.DATA, path);
                                        values.put(MediaStore.Images.Media.MIME_TYPE, MediaType.getMimeType(path));
                                    }
                                    Uri contentUri = MediaStore.Files.getContentUri("external");
                                    ContentResolver resolver = context.getContentResolver();
                                    resolver.insert(contentUri, values);
                                }
                            } else {
                                Log.i("FileOperation", "MediaScannerConnection.scanFile() path = [" + path + "]");
                            }

                            if (showToast) {
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast toast = toastWeakReference.get();
                                        if (toast != null) {
                                            toastWeakReference.get().show();
                                        }
                                    }
                                });
                            }

                            pathsScanned++;
                            if (callback != null && pathsScanned == paths.length) {
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onAllPathsScanned();
                                    }
                                });
                            }
                        }
                    });
        }

        public static String getParentPath(String path) {
            return new File(path).getParent();
        }
    }
}
