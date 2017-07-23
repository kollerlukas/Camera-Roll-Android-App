package us.koller.cameraroll.data.provider.retriever;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.data.provider.FilesProvider;
import us.koller.cameraroll.data.provider.itemLoader.AlbumLoader;
import us.koller.cameraroll.data.provider.itemLoader.FileLoader;
import us.koller.cameraroll.data.provider.itemLoader.ItemLoader;
import us.koller.cameraroll.data.provider.MediaProvider;
import us.koller.cameraroll.data.provider.Provider;
import us.koller.cameraroll.data.StorageRoot;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.SortUtil;

//loading media by searching through Storage
//advantage: all items, disadvantage: slower than MediaStore
public class StorageRetriever extends Retriever {

    interface StorageSearchCallback {
        void onThreadResult(ItemLoader.Result result);

        void done();
    }

    //option to set thread count;
    private static final int THREAD_COUNT = 8;

    private ArrayList<AbstractThread> threads;

    //for timeout
    private Handler handler;
    private Runnable timeout;

    public StorageRetriever() {

    }

    @Override
    void loadAlbums(final Activity context, final boolean hiddenFolders) {
        final long startTime = System.currentTimeMillis();

        final ArrayList<Album> albums = new ArrayList<>();

        //handle timeout after 10 seconds
        handler = new Handler();
        timeout = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "timeout", Toast.LENGTH_SHORT).show();
                MediaProvider.Callback callback = getCallback();
                if (callback != null) {
                    callback.timeout();
                }
            }
        };
        handler.postDelayed(timeout, 10000);

        //load media from storage
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                searchStorage(context,
                        new StorageSearchCallback() {

                            @Override
                            public void onThreadResult(ItemLoader.Result result) {
                                albums.addAll(result.albums);
                            }

                            @Override
                            public void done() {
                                if (!hiddenFolders) {
                                    for (int i = albums.size() - 1; i >= 0; i--) {
                                        if (albums.get(i).isHidden()) {
                                            albums.remove(i);
                                        }
                                    }
                                }

                                //done loading media from storage
                                MediaProvider.Callback callback = getCallback();
                                if (callback != null) {
                                    callback.onMediaLoaded(albums);
                                }
                                cancelTimeout();
                                Log.d("StorageRetriever", "onMediaLoaded(" + String.valueOf(THREAD_COUNT)
                                        + "): " + String.valueOf(System.currentTimeMillis() - startTime) + " ms");
                            }
                        });
            }
        });
    }

    public static StorageRoot[] loadRoots(Activity context) {
        ArrayList<StorageRoot> temp = new ArrayList<>();

        StorageRoot externalStorage
                = new StorageRoot(Environment
                .getExternalStorageDirectory().getPath());
        externalStorage.setName(context.getString(R.string.storage));
        temp.add(externalStorage);

        File[] removableStorageRoots = getRemovableStorageRoots(context);
        for (int i = 0; i < removableStorageRoots.length; i++) {
            temp.add(new StorageRoot(removableStorageRoots[i].getPath()));
        }

        StorageRoot[] roots = new StorageRoot[temp.size()];
        return temp.toArray(roots);
    }

    public void loadFilesForDir(final Activity context, String dirPath,
                                final FilesProvider.Callback callback) {

        if (new File(dirPath).isFile()) {
            callback.onDirLoaded(null);
            return;
        }

        threads = new ArrayList<>();

        Thread.Callback threadCallback = new Thread.Callback() {
            @Override
            public void done(Thread thread, ItemLoader.Result result) {
                File_POJO files = result.files;
                boolean filesContainMedia = false;
                for (int i = 0; i < files.getChildren().size(); i++) {
                    if (files.getChildren().get(i) != null &&
                            MediaType.isMedia(
                                    files.getChildren().get(i).getPath())) {
                        filesContainMedia = true;
                        break;
                    }
                }

                if (filesContainMedia) {
                    SortUtil.sortByDate(files.getChildren());
                } else {
                    SortUtil.sortByName(files.getChildren());
                }
                callback.onDirLoaded(files);
                thread.cancel();
                threads = null;
            }
        };

        final File[] files = new File[]{new File(dirPath)};
        Thread thread = new Thread(context, files, new FileLoader())
                .notSearchSubDirs()
                .setCallback(threadCallback);
        threads.add(thread);
        thread.start();
    }

    private void cancelTimeout() {
        if (handler != null && timeout != null) {
            handler.removeCallbacks(timeout);
        }
    }

    @Override
    public void onDestroy() {
        cancelTimeout();
        //cancel all threads when Activity is being destroyed
        if (threads != null) {
            for (int i = 0; i < threads.size(); i++) {
                if (threads.get(i) != null) {
                    threads.get(i).cancel();
                }
            }
        }
    }

    private static File[] getRemovableStorageRoots(Context context) {
        File[] roots = context.getExternalFilesDirs("external");
        ArrayList<File> rootsArrayList = new ArrayList<>();

        for (int i = 0; i < roots.length; i++) {
            if (roots[i] != null) {
                String path = roots[i].getPath();
                int index = path.lastIndexOf("/Android/data/");
                if (index > 0) {
                    path = path.substring(0, index);
                    if (!path.equals(Environment
                            .getExternalStorageDirectory().getPath())) {
                        rootsArrayList.add(new File(path));
                    }
                }
            }
        }

        roots = new File[rootsArrayList.size()];
        rootsArrayList.toArray(roots);
        return roots;
    }

    private static File[] getDirectoriesToSearch(Context context) {
        //external Directory
        File dir = Environment.getExternalStorageDirectory();
        File[] dirs = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file != null
                        && Provider.searchDir(file.getPath());
            }
        });

        //handle removable storage (e.g. SDCards)
        ArrayList<File> temp = new ArrayList<>();
        temp.addAll(Arrays.asList(dirs));
        File[] removableStorageRoots = getRemovableStorageRoots(context);
        for (int i = 0; i < removableStorageRoots.length; i++) {
            Log.d("StorageRetriever", "removableStorageRoot: "
                    + removableStorageRoots[i].getPath());
            File root = removableStorageRoots[i];
            File[] files = root.listFiles();
            if (files != null) {
                Collections.addAll(temp, files);
            }
        }

        dirs = new File[temp.size()];

        for (int i = 0; i < dirs.length; i++) {
            dirs[i] = temp.get(i);
        }

        return dirs;
    }

    private void searchStorage(final Activity context, final StorageSearchCallback callback) {
        File[] dirs = getDirectoriesToSearch(context);

        threads = new ArrayList<>();

        Thread.Callback threadCallback = new Thread.Callback() {
            @Override
            public void done(Thread thread, ItemLoader.Result result) {
                callback.onThreadResult(result);
                threads.remove(thread);
                thread.cancel();
                if (threads.size() == 0) {
                    callback.done();
                    threads = null;
                }
            }
        };

        final File[][] threadDirs = divideDirs(dirs);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final File[] files = threadDirs[i];
            Thread thread = new Thread(context, files, new AlbumLoader())
                    .setCallback(threadCallback);
            threads.add(thread);
            thread.start();
        }
    }

    private File[][] divideDirs(File[] dirs) {
        int[] threadDirs_sizes = new int[THREAD_COUNT];
        int rest = dirs.length % THREAD_COUNT;
        for (int i = 0; i < threadDirs_sizes.length; i++) {
            threadDirs_sizes[i] = dirs.length / THREAD_COUNT;
            if (rest > 0) {
                threadDirs_sizes[i]++;
                rest--;
            }
        }

        Log.d("StorageRetriever", Arrays.toString(threadDirs_sizes));

        File[][] threadDirs = new File[THREAD_COUNT][dirs.length / THREAD_COUNT + 1];
        int index = 0;
        for (int i = 0; i < THREAD_COUNT; i++) {
            File[] threadDir = Arrays.copyOfRange(dirs, index, index + threadDirs_sizes[i]);
            threadDirs[i] = threadDir;
            index = index + threadDirs_sizes[i];
        }

        return threadDirs;
    }

    //Thread classes
    static abstract class AbstractThread extends java.lang.Thread {

        public interface Callback {
            void done(Thread thread, ItemLoader.Result result);
        }

        Context context;
        File[] dirs;
        ItemLoader itemLoader;

        Callback callback;

        boolean searchSubDirs = true;

        AbstractThread(Context context, File[] dirs, ItemLoader itemLoader) {
            this.context = context;
            this.dirs = dirs;
            this.itemLoader = itemLoader;
        }

        @SuppressWarnings("unchecked")
        public <T extends AbstractThread> T setCallback(Callback callback) {
            this.callback = callback;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        <T extends AbstractThread> T notSearchSubDirs() {
            this.searchSubDirs = false;
            return (T) this;
        }

        abstract void cancel();
    }

    public static class Thread extends AbstractThread {

        public Thread(Context context, File[] dirs, ItemLoader itemLoader) {
            super(context, dirs, itemLoader);
        }

        @Override
        public void run() {
            super.run();

            if (dirs != null) {
                for (int i = 0; i < dirs.length; i++) {
                    recursivelySearchStorage(context, dirs[i]);
                }
            }

            if (callback != null) {
                callback.done(this, itemLoader.getResult());
            }
        }

        private void recursivelySearchStorage(final Context context,
                                              final File file) {
            if (interrupted() || file == null) {
                return;
            }

            if (file.isFile()) {
                return;
            }

            itemLoader.onNewDir(context, file);

            File[] files = file.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    itemLoader.onFile(context, files[i]);
                }
                itemLoader.onDirDone(context);

                if (searchSubDirs) {
                    //search sub-directories
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].isDirectory()) {
                            recursivelySearchStorage(context, files[i]);
                        }
                    }
                }
            }
        }

        public void cancel() {
            context = null;
            callback = null;
            interrupt();
        }
    }
}
