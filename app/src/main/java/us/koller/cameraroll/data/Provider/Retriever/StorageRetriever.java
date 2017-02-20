package us.koller.cameraroll.data.Provider.Retriever;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;

import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.data.Provider.FilesProvider;
import us.koller.cameraroll.data.Provider.ItemLoader.AlbumLoader;
import us.koller.cameraroll.data.Provider.ItemLoader.FileLoader;
import us.koller.cameraroll.data.Provider.ItemLoader.ItemLoader;
import us.koller.cameraroll.data.Provider.MediaProvider;
import us.koller.cameraroll.util.SortUtil;

//loading media by searching through Storage
//advantage: all items, disadvantage: slower than MediaStore
public class StorageRetriever implements Retriever {

    interface StorageSearchCallback {
        void onPartialResult(ItemLoader.Result result);

        void done();
    }

    //option to set thread count;
    //if set to -1 every dir in home dir get its own thread
    private static final int THREAD_COUNT = 16;

    //use AdaptableThreads
    private static final boolean USE_ADAPTABLE_THREADS = false;

    private ArrayList<AbstractThread> threads;

    //for timeout
    private Handler handler;
    private Runnable timeout;

    private Class itemLoaderClass;

    public StorageRetriever() {

    }

    @Override
    public void loadAlbums(final Activity context, final boolean hiddenFolders,
                           final MediaProvider.Callback callback) {

        itemLoaderClass = AlbumLoader.class;

        final long startTime = System.currentTimeMillis();

        final ArrayList<Album> albums = new ArrayList<>();

        //handle timeout
        handler = new Handler();
        timeout = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "timeout", Toast.LENGTH_SHORT).show();
                callback.timeout();
            }
        };
        handler.postDelayed(timeout, 5000);

        //load media from storage
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                searchStorage(context,
                        new StorageSearchCallback() {

                            @Override
                            public void onPartialResult(ItemLoader.Result result) {
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
                                //SortUtil.sortAlbums(context, albums, SortUtil.BY_NAME);
                                SortUtil.sortByName(albums);
                                callback.onMediaLoaded(albums);
                                cancelTimeout();
                                if (THREAD_COUNT == -1) {
                                    Log.d("StorageRetriever", "onMediaLoaded(): " + String.valueOf(System.currentTimeMillis() - startTime));
                                } else {
                                    Log.d("StorageRetriever", "onMediaLoaded(" + String.valueOf(THREAD_COUNT)
                                            + "): " + String.valueOf(System.currentTimeMillis() - startTime));
                                }
                            }
                        });
            }
        });
    }

    public File_POJO[] loadRoots() {

        ArrayList<File_POJO> temp = new ArrayList<>();

        temp.add(new File_POJO(Environment.getExternalStorageDirectory().getPath(), false));

        File[] removableStorageRoots = getRemovableStorageRoots();
        for (int i = 0; i < removableStorageRoots.length; i++) {
            temp.add(new File_POJO(removableStorageRoots[i].getPath(), false));
        }

        File_POJO[] roots = new File_POJO[temp.size()];
        return temp.toArray(roots);
    }

    public void loadDir(Activity context, String dirPath,
                        final FilesProvider.Callback callback) {

        if (new File(dirPath).isFile()) {
            callback.onDirLoaded(null);
            return;
        }

        itemLoaderClass = FileLoader.class;

        threads = new ArrayList<>();

        AdaptableThread.Callback adaptableThreadCallback = new AdaptableThread.Callback() {
            @Override
            public void done(AdaptableThread thread, ItemLoader.Result result,
                             ArrayList<File> filesToSearch) {
                File_POJO files = result.files;
                SortUtil.sortByName(files.getChildren());
                callback.onDirLoaded(files);
                thread.cancel();
                threads = null;
            }

            @Override
            public File needWork() {
                return null;
            }
        };

        AdaptableThread thread
                = new AdaptableThread(context, new File(dirPath),
                adaptableThreadCallback, itemLoaderClass);
        thread.start();
        threads.add(thread);
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

    private static File getStorageRoot() {
        return new File("/storage");
    }

    private static File[] getDirectoriesToSearch() {
        //external Directory
        File dir = Environment.getExternalStorageDirectory(); //new File("/storage/emulated/0");
        File[] dirs = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return !file.getPath().equals("/storage/emulated/0/Android");
            }
        });


        //handle removable storage (e.g. SDCards)
        ArrayList<File> temp = new ArrayList<>();
        temp.addAll(Arrays.asList(dirs));
        File[] removableStorageRoots = getRemovableStorageRoots();
        for (int i = 0; i < removableStorageRoots.length; i++) {
            Log.d("StorageRetriever", "removableStorageRoot: " + removableStorageRoots[i].getPath());
            File root = removableStorageRoots[i];
            File[] files = root.listFiles();
            if (files != null) {
                for (int k = 0; k < files.length; k++) {
                    temp.add(files[k]);
                }
            }
        }

        dirs = new File[temp.size()];

        for (int i = 0; i < dirs.length; i++) {
            dirs[i] = temp.get(i);
        }

        return dirs;
    }

    private static File[] getRemovableStorageRoots() {
        //look for sd cards
        File root = getStorageRoot();

        return root.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                File[] files1 = file.listFiles();
                return files1 != null && files1.length > 0;
            }
        });
    }

    private void searchStorage(final Activity context, final StorageSearchCallback callback) {
        File[] dirs = getDirectoriesToSearch();

        threads = new ArrayList<>();

        Thread.Callback threadCallback = new Thread.Callback() {
            @Override
            public void done(Thread thread, ItemLoader.Result result) {
                callback.onPartialResult(result);
                threads.remove(thread);
                thread.cancel();
                if (threads.size() == 0) {
                    callback.done();
                    threads = null;
                }
            }
        };

        if (THREAD_COUNT == -1) {
            for (int i = 0; i < dirs.length; i++) {
                final File[] files = {dirs[i]};
                Thread thread = new Thread(context, files,
                        threadCallback, itemLoaderClass);
                thread.start();
                threads.add(thread);
            }
        } else if (!USE_ADAPTABLE_THREADS) {
            final File[][] threadDirs = divideDirs(dirs);

            for (int i = 0; i < THREAD_COUNT; i++) {
                final File[] files = threadDirs[i];
                Thread thread = new Thread(context, files,
                        threadCallback, itemLoaderClass);
                thread.start();
                threads.add(thread);
            }
        } else {
            final ArrayList<File> queue = new ArrayList<>(Arrays.asList(dirs));

            AdaptableThread.Callback adaptableThreadCallback = new AdaptableThread.Callback() {
                @Override
                public void done(AdaptableThread thread, ItemLoader.Result result,
                                 ArrayList<File> filesToSearch) {
                    callback.onPartialResult(result);
                    for (int i = 0; i < filesToSearch.size(); i++) {
                        queue.add(filesToSearch.get(i));
                    }
                }

                @Override
                public File needWork() {
                    File file = nextDir();
                    if (file == null) {
                        //check if done
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                checkIfDone();
                            }
                        });
                    }
                    return file;
                }

                private synchronized File nextDir() {
                    if (queue.size() > 0) {
                        File fileSearch = queue.get(0);
                        queue.remove(0);
                        return fileSearch;
                    }
                    return null;
                }

                boolean alreadyDone = false;

                private synchronized void checkIfDone() {
                    if (alreadyDone) {
                        return;
                    }

                    //check if done with searching
                    boolean done = true;
                    for (int i = 0; i < threads.size(); i++) {
                        if (threads.get(i) != null
                                && ((AdaptableThread) threads.get(i)).searching) {
                            done = false;
                            break;
                        }
                    }

                    if (done) {
                        alreadyDone = true;
                        onDestroy();
                        callback.done();
                    }
                }
            };

            for (int i = 0; i < THREAD_COUNT; i++) {
                if (queue.size() > 0) {
                    AdaptableThread thread
                            = new AdaptableThread(context, queue.get(0),
                            adaptableThreadCallback, itemLoaderClass);
                    //remove file from queue
                    queue.remove(0);
                    thread.start();
                    threads.add(thread);
                }
            }
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

    private static abstract class AbstractThread extends java.lang.Thread {

        ItemLoader itemLoader;

        AbstractThread(Class itemLoaderClass) {
            itemLoader = ItemLoader.getInstance(itemLoaderClass);
        }

        abstract void cancel();
    }

    private static class Thread extends AbstractThread {

        interface Callback {
            void done(Thread thread, ItemLoader.Result result);
        }

        private Activity context;
        private Callback callback;

        private File[] dirs;

        Thread(Activity context, File[] dirs, Callback callback, Class itemLoaderClass) {
            super(itemLoaderClass);
            this.context = context;
            this.callback = callback;
            this.dirs = dirs;
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

        private void recursivelySearchStorage(final Activity context,
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

                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        recursivelySearchStorage(context, files[i]);
                    }
                }
            }
        }

        void cancel() {
            context = null;
            callback = null;
            interrupt();
        }
    }

    //trying to compensate for bigger directories
    private static class AdaptableThread extends AbstractThread {

        interface Callback {
            void done(AdaptableThread thread, ItemLoader.Result result, ArrayList<File> filesToSearch);

            File needWork();
        }

        private Activity context;
        private Callback callback;

        private ArrayList<File> queue;

        boolean searching = false;

        AdaptableThread(Activity context, File dir, Callback callback, Class itemLoaderClass) {
            super(itemLoaderClass);
            this.context = context;
            this.callback = callback;

            queue = new ArrayList<>();
            queue.add(dir);
        }

        @Override
        public void run() {
            super.run();

            while (!interrupted()) {
                if (queue.size() > 0 && queue.get(0) != null) {
                    searchDir(context, queue.get(0));
                    queue.remove(0);
                } else {
                    File dir = callback.needWork();
                    if (dir != null) {
                        searchDir(context, dir);
                    }
                }
            }
        }

        private void searchDir(final Activity context, final File file) {
            if (interrupted() || file == null) {
                return;
            }

            if (file.isFile()) {
                return;
            }

            searching = true;

            itemLoader.onNewDir(context, file);

            final ArrayList<File> filesToSearch = new ArrayList<>();

            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {

                itemLoader.onFile(context, files[i]);

                if (files[i].isDirectory()) {
                    filesToSearch.add(files[i]);
                }
            }

            itemLoader.onDirDone(context);

            callback.done(this, itemLoader.getResult(), filesToSearch);

            searching = false;
        }

        void cancel() {
            interrupt();
            context = null;
            callback = null;
        }
    }
}
