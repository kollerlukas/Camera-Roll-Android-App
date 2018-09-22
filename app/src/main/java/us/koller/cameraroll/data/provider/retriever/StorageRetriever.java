package us.koller.cameraroll.data.provider.retriever;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import us.koller.cameraroll.data.models.Album;
import us.koller.cameraroll.data.models.File_POJO;
import us.koller.cameraroll.data.provider.FilesProvider;
import us.koller.cameraroll.data.provider.MediaProvider;
import us.koller.cameraroll.data.provider.Provider;
import us.koller.cameraroll.data.provider.itemLoader.AlbumLoader;
import us.koller.cameraroll.data.provider.itemLoader.FileLoader;
import us.koller.cameraroll.data.provider.itemLoader.ItemLoader;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.SortUtil;

//loading media by searching through Storage
//advantage: all items, disadvantage: slower than MediaStore
public class StorageRetriever extends Retriever {

    //option to set thread count;
    private static final int THREAD_COUNT = 16;

    private ArrayList<AbstractThread> threads;

    //for timeout
    private Handler handler;
    private Runnable timeout;

    interface StorageSearchCallback {
        void onThreadResult(ItemLoader.Result result);

        void done();
    }

    @Override
    void loadAlbums(final Activity context, final boolean hiddenFolders) {
        final long startTime = System.currentTimeMillis();

        final ArrayList<Album> albums = new ArrayList<>();

        //handle timeout after 10 seconds
        handler = new Handler();
        timeout = () -> {
            Toast.makeText(context, "timeout", Toast.LENGTH_SHORT).show();
            MediaProvider.OnMediaLoadedCallback callback = getCallback();
            if (callback != null) {
                callback.timeout();
            }
        };
        handler.postDelayed(timeout, 10000);

        //load media from storage
        AsyncTask.execute(() -> searchStorage(context,
                new StorageSearchCallback() {

                    @Override
                    public void onThreadResult(ItemLoader.Result result) {
                        if (result != null) {
                            albums.addAll(result.albums);
                        }
                    }

                    @Override
                    public void done() {
                        if (!hiddenFolders) {
                            for (int i = albums.size() - 1; i >= 0; i--) {
                                if (albums.get(i) == null || albums.get(i).isHidden()) {
                                    albums.remove(i);
                                }
                            }
                        }

                        //done loading media from storage
                        MediaProvider.OnMediaLoadedCallback callback = getCallback();
                        if (callback != null) {
                            callback.onMediaLoaded(albums);
                        }
                        cancelTimeout();
                        Log.d("StorageRetriever", "onMediaLoaded(" + String.valueOf(THREAD_COUNT)
                                + "): " + String.valueOf(System.currentTimeMillis() - startTime) + " ms");
                    }
                }));
    }

    public void loadFilesForDir(final Activity context, String dirPath,
                                final FilesProvider.Callback callback) {

        if (new File(dirPath).isFile()) {
            callback.onDirLoaded(null);
            return;
        }

        threads = new ArrayList<>();

        Thread.Callback threadCallback = (thread, result) -> {
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

    private void searchStorage(final Activity context, final StorageSearchCallback callback) {
        File[] dirs = Provider.getDirectoriesToSearch(context);

        threads = new ArrayList<>();

        Thread.Callback threadCallback = (thread, result) -> {
            callback.onThreadResult(result);
            threads.remove(thread);
            thread.cancel();
            if (threads.size() == 0) {
                callback.done();
                threads = null;
            }
        };

        final File[][] threadDirs = divideDirs(dirs);

        /*DateTakenRetriever dateRetriever = new DateTakenRetriever();*/

        for (int i = 0; i < THREAD_COUNT; i++) {
            final File[] files = threadDirs[i];
            if (files.length > 0) {
                ItemLoader itemLoader = new AlbumLoader()/*.setDateRetriever(dateRetriever)*/;
                Thread thread = new Thread(context, files, itemLoader)
                        .setCallback(threadCallback);
                threads.add(thread);
                thread.start();
            }
        }
    }

    private File[][] divideDirs(File[] dirs) {
        ArrayList<File> dirsList = new ArrayList<>();
        for (File dir : dirs) {
            if (dir.listFiles() != null) {
                dirsList.add(dir);
            }
        }
        dirs = new File[dirsList.size()];
        dirsList.toArray(dirs);

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

        Context context;
        File[] dirs;
        ItemLoader itemLoader;

        Callback callback;

        boolean searchSubDirs = true;

        public interface Callback {
            void done(Thread thread, ItemLoader.Result result);
        }

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

        Thread(Context context, File[] dirs, ItemLoader itemLoader) {
            super(context, dirs, itemLoader);
        }

        @Override
        public void run() {
            super.run();
            if (dirs != null) {
                for (File dir : dirs) {
                    recursivelySearchStorage(context, dir);
                }
            }
            done();
        }

        public void done() {
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
                for (File file1 : files) {
                    itemLoader.onFile(context, file1);
                }
                itemLoader.onDirDone(context);

                if (searchSubDirs) {
                    //search sub-directories
                    for (File file1 : files) {
                        if (file1.isDirectory()) {
                            recursivelySearchStorage(context, file1);
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
