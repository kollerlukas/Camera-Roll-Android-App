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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.media.ExifInterface;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.ContentObserver;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.data.models.AlbumItem;
import us.koller.cameraroll.data.models.File_POJO;
import us.koller.cameraroll.data.models.Video;
import us.koller.cameraroll.util.ExifUtil;
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

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public FileOperation createFromParcel(Parcel p) {
            switch (p.readInt()) {
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void createNotificationChannel(Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel nc = new NotificationChannel(
                c.getString(R.string.file_op_channel_id),
                c.getString(R.string.file_op_channel_name),
                NotificationManager.IMPORTANCE_LOW);
        nc.setDescription(c.getString(R.string.file_op_channel_description));
        if (nm != null) {
            nm.createNotificationChannel(nc);
        }
    }

    public static File_POJO[] getFiles(Intent wI) {
        Parcelable[] p = wI.getParcelableArrayExtra(FILES);
        return File_POJO.generateArray(p);
    }

    public static Intent getDefaultIntent(Context c, int action, File_POJO[] files) {
        String aS = Util.getActionString(c, EMPTY);
        Class service = null;
        switch (action) {
            case COPY:
                service = Copy.class;
                aS = Util.getActionString(c, COPY);
                break;
            case MOVE:
                service = Move.class;
                aS = Util.getActionString(c, MOVE);
                break;
            case DELETE:
                service = Delete.class;
                aS = Util.getActionString(c, DELETE);
                break;
            case NEW_DIR:
                service = NewDirectory.class;
                aS = Util.getActionString(c, NEW_DIR);
                break;
            case RENAME:
                service = Rename.class;
                aS = Util.getActionString(c, RENAME);
                break;
            default:
                break;
        }
        if (service != null) {
            return new Intent(c, service)
                    .setAction(aS)
                    .putExtra(FileOperation.FILES, files);
        }
        return new Intent();
    }

    abstract String getNotificationTitle();

    public abstract int getNotificationSmallIconRes();

    @Override
    protected void onHandleIntent(Intent wI) {
        notifBuilder = createNotificationBuilder();
        notifBuilder.setProgress(1, 0, false);
        Notification n = notifBuilder.build();
        startForeground(NOTIFICATION_ID, n);
        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIFICATION_ID, n);
        }
        ContentObserver.selfChange = true;
        execute(wI);
        if (autoSendDoneBroadcast()) {
            if (pathsToScan.size() > 0) {
                onProgress(-1, -1);
                scanPaths(getApplicationContext(), () -> {
                    sendDoneBroadcast();
                    stopForeground(true);
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
            createNotificationChannel(getApplicationContext());
        }
        NotificationCompat.Builder b = new NotificationCompat.Builder(this,
                getString(R.string.file_op_channel_id))
                .setContentTitle(getNotificationTitle());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            b.setSmallIcon(getNotificationSmallIconRes());
        }
        return b;
    }

    private NotificationCompat.Builder getNotificationBuilder() {
        return notifBuilder;
    }

    public abstract void execute(Intent wI);

    public void addPathToScan(String path) {
        pathsToScan.add(path);
    }

    public void addPathsToScan(List<String> paths) {
        pathsToScan.addAll(paths);
    }

    ArrayList<String> getPathsToScan() {
        return pathsToScan;
    }

    public boolean autoSendDoneBroadcast() {
        return true;
    }

    public void sendDoneBroadcast() {
        ContentObserver.selfChange = false;
        showToast(getString(R.string.done));
        Intent i = getDoneIntent();
        sendLocalBroadcast(i);
    }

    public void sendFailedBroadcast(Intent wI, String path) {
        Intent i = new Intent(FAILED);
        i.putExtra(FILES, path);
        i.putExtra(WORK_INTENT, wI);
        sendLocalBroadcast(i);
    }

    public abstract int getType();

    public void requestPermissionForRemovableStorageBroadcast(Intent wI) {
        Intent i = new Intent(NEED_REMOVABLE_STORAGE_PERMISSION);
        i.putExtra(WORK_INTENT, wI);
        sendLocalBroadcast(i);
    }

    public Intent getDoneIntent() {
        Intent i = new Intent(RESULT_DONE);
        i.putExtra(TYPE, this.getType());
        return i;
    }

    public void sendLocalBroadcast(Intent i) {
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
    }

    public void runOnUiThread(Runnable r) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(r);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @SuppressWarnings("unused")
    public void onProgress(final int progress, final int totalNumber) {
        NotificationCompat.Builder nB = getNotificationBuilder();
        if (progress >= 0) {
            nB.setProgress(totalNumber, progress, false);
        } else {
            nB.setProgress(0, 0, true);
        }
        //nB.setProgress(0, 0, true);
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIFICATION_ID, nB.build());
        }
    }

    public void showToast(final String message) {
        runOnUiThread(() -> Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void writeToParcel(Parcel p, int i) {
        p.writeInt(getType());
    }

    public Uri getTreeUri(Intent wI, String path) {
        Log.d("FileOperation", "getTreeUri");
        Uri treeUri;
        String treeUriExtra = wI.getStringExtra(FileOperation.REMOVABLE_STORAGE_TREE_URI);
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
                requestPermissionForRemovableStorageBroadcast(wI);
            }
        } else {
            return treeUri;
        }
        return null;
    }

    void scanPaths(final Context c, final Util.MediaScannerCallback ca) {
        String[] paths = new String[pathsToScan.size()];
        pathsToScan.toArray(paths);
        Util.scanPaths(c, paths, ca);
    }

    public static class Util {
        public static IntentFilter getIntentFilter(IntentFilter f) {
            f.addAction(FileOperation.RESULT_DONE);
            f.addAction(FileOperation.FAILED);
            return f;
        }

        public static String getActionString(Context c, int type) {
            switch (type) {
                case EMPTY:
                    return "empty";
                case MOVE:
                    return c.getString(R.string.move);
                case COPY:
                    return c.getString(R.string.copy);
                case DELETE:
                    return c.getString(R.string.delete);
                case NEW_DIR:
                    return c.getString(R.string.new_folder);
                case RENAME:
                    return c.getString(R.string.rename);
                default:
                    break;
            }
            return "";
        }

        public static int getActionInt(Context c, String action) {
            if (action.equals(c.getString(R.string.move))) {
                return MOVE;
            } else if (action.equals(c.getString(R.string.copy))) {
                return COPY;
            } else if (action.equals(c.getString(R.string.delete))) {
                return DELETE;
            }
            return EMPTY;
        }

        static boolean isOnRemovableStorage(String path) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                File f = new File(path);
                try {
                    if (f.exists() && Environment.isExternalStorageRemovable(f)) {
                        return true;
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }

        public static ArrayList<String> getAllChildPaths(ArrayList<String> paths, String path) {
            File f = new File(path);
            if (f.exists()) {
                if (f.isDirectory()) {
                    File[] children = f.listFiles();
                    for (int i = 0; i < children.length; i++) {
                        getAllChildPaths(paths, children[i].getPath());
                    }
                } else {
                    paths.add(path);
                }
            }
            return paths;
        }

        public static void scanPaths(final Context c, final String[] paths, final MediaScannerCallback ca) {
            scanPaths(c, paths, ca, false);
        }

        public static void scanPathsWithNotification(final Context c, final String[] paths) {
            scanPaths(c, paths, null, true);
        }

        @SuppressLint("ShowToast")
        private static void scanPaths(final Context c, final String[] paths, final MediaScannerCallback msc, final boolean withNotification) {
            Log.i("FileOperation", "scanPaths(), paths: " + Arrays.toString(paths));
            if (paths == null) {
                if (msc != null) {
                    msc.onAllPathsScanned();
                }
                return;
            }

            //create notification
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(c);
            }
            final NotificationCompat.Builder nB = new NotificationCompat.Builder(c,
                    c.getString(R.string.file_op_channel_id)).setContentTitle("Scanning...");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                nB.setSmallIcon(R.drawable.ic_autorenew_white);
            }
            nB.setProgress(paths.length, 0, false);
            final NotificationManager ma = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);

            AsyncTask.execute(() -> {
                for (int i = 0; i < paths.length; i++) {
                    String path = paths[i];
                    if (MediaType.isMedia(path)) {
                        Uri contentUri = MediaStore.Files.getContentUri("external");
                        ContentResolver cr = c.getContentResolver();
                        if (new File(path).exists()) {
                            AlbumItem albumItem = AlbumItem.getInstance(path);
                            ContentValues cv = new ContentValues();
                            if (albumItem instanceof Video) {
                                cv.put(MediaStore.Video.Media.DATA, path);
                                cv.put(MediaStore.Video.Media.MIME_TYPE, MediaType.getMimeType(path));
                            } else {
                                cv.put(MediaStore.Images.Media.DATA, path);
                                cv.put(MediaStore.Images.Media.MIME_TYPE, MediaType.getMimeType(path));
                                try {
                                    ExifInterface exif = new ExifInterface(path);
                                    Locale l = us.koller.cameraroll.util.Util.getLocale(c);
                                    String dateString = String.valueOf(ExifUtil.getCastValue(exif, ExifInterface.TAG_DATETIME));
                                    try {
                                        Date date = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", l).parse(dateString);
                                        long dateTaken = date.getTime();
                                        cv.put(MediaStore.Images.Media.DATE_TAKEN, dateTaken);
                                    } catch (ParseException ignored) {
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            cr.insert(contentUri, cv);
                        } else {
                            cr.delete(contentUri, MediaStore.MediaColumns.DATA + "='" + path + "'", null);
                        }
                    }
                    if (withNotification) {
                        nB.setProgress(paths.length, i, false);
                        if (ma != null) {
                            ma.notify(NOTIFICATION_ID, nB.build());
                        }
                    }
                }
                if (ma != null) {
                    ma.cancel(NOTIFICATION_ID);
                }
                if (msc != null) {
                    new Handler(Looper.getMainLooper()).post(() -> msc.onAllPathsScanned());
                }
            });
        }

        public static String getParentPath(String path) {
            return new File(path).getParent();
        }

        public interface MediaScannerCallback {
            void onAllPathsScanned();
        }
    }
}
