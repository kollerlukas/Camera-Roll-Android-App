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

    @Override
    void loadAlbums(final Activity c, final boolean hiddenFolders) {
        final long startTime = System.currentTimeMillis();
        final ArrayList<Album> albums = new ArrayList<>();

        //handle timeout after 10 seconds
        handler = new Handler();
        timeout = (() -> {
            Toast.makeText(c, "timeout", Toast.LENGTH_SHORT).show();
            MediaProvider.OnMediaLoadedCallback callback = getCallback();
            if (callback != null) {
                callback.timeout();
            }
        });
        handler.postDelayed(timeout, 10000);

        //load media from storage
        AsyncTask.execute(() -> searchStorage(c, new StorageSearchCallback() {
            @Override
            public void onThreadResult(ItemLoader.Result r) {
                if (r != null) {
                    albums.addAll(r.albums);
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
                MediaProvider.OnMediaLoadedCallback ca = getCallback();
                if (ca != null) {
                    ca.onMediaLoaded(albums);
                }
                cancelTimeout();
                Log.d("StorageRetriever", "onMediaLoaded(" + String.valueOf(THREAD_COUNT)
                        + "): " + String.valueOf(System.currentTimeMillis() - startTime) + " ms");
            }
        }));
    }

    public void loadFilesForDir(final Activity c, String dirPath, final FilesProvider.Callback ca) {
        if (new File(dirPath).isFile()) {
            ca.onDirLoaded(null);
            return;
        }

        threads = new ArrayList<>();

        Thread.Callback tCa = (Thread t, ItemLoader.Result r) -> {
            File_POJO files = r.files;
            boolean filesContainMedia = false;
            for (int i = 0; i < files.getChildren().size(); i++) {
                if (files.getChildren().get(i) != null &&
                        MediaType.isMedia(files.getChildren().get(i).getPath())) {
                    filesContainMedia = true;
                    break;
                }
            }
            if (filesContainMedia) {
                SortUtil.sortByDate(files.getChildren());
            } else {
                SortUtil.sortByName(files.getChildren());
            }
            ca.onDirLoaded(files);
            t.cancel();
            threads = null;
        };

        final File[] files = new File[]{new File(dirPath)};
        Thread t = new Thread(c, files, new FileLoader()).notSearchSubDirs().setCallback(tCa);
        threads.add(t);
        t.start();
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

    private void searchStorage(final Activity c, final StorageSearchCallback ca) {
        File[] dirs = Provider.getDirectoriesToSearch(c);
        threads = new ArrayList<>();
        Thread.Callback threadCallback = (Thread t, ItemLoader.Result r) -> {
            ca.onThreadResult(r);
            threads.remove(t);
            t.cancel();
            if (threads.size() == 0) {
                ca.done();
                threads = null;
            }
        };
        final File[][] threadDirs = divideDirs(dirs);
        /*DateTakenRetriever dateRetriever = new DateTakenRetriever();*/
        for (int i = 0; i < THREAD_COUNT; i++) {
            final File[] files = threadDirs[i];
            if (files.length > 0) {
                ItemLoader iL = new AlbumLoader()/*.setDateRetriever(dateRetriever)*/;
                Thread t = new Thread(c, files, iL)
                        .setCallback(threadCallback);
                threads.add(t);
                t.start();
            }
        }
    }

    private File[][] divideDirs(File[] dirs) {
        ArrayList<File> dirsList = new ArrayList<>();
        for (int i = 0; i < dirs.length; i++) {
            if (dirs[i].listFiles() != null) {
                dirsList.add(dirs[i]);
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
            File[] tD = Arrays.copyOfRange(dirs, index, index + threadDirs_sizes[i]);
            threadDirs[i] = tD;
            index = index + threadDirs_sizes[i];
        }
        return threadDirs;
    }

    interface StorageSearchCallback {
        void onThreadResult(ItemLoader.Result r);

        void done();
    }

    //Thread classes
    static abstract class AbstractThread extends java.lang.Thread {
        Context context;
        File[] dirs;
        ItemLoader itemLoader;
        Callback callback;
        boolean searchSubDirs = true;

        AbstractThread(Context c, File[] dirs, ItemLoader iL) {
            this.context = c;
            this.dirs = dirs;
            this.itemLoader = iL;
        }

        @SuppressWarnings("unchecked")
        public <T extends AbstractThread> T setCallback(Callback ca) {
            this.callback = ca;
            return (T) this;
        }

        public interface Callback {
            void done(Thread thread, ItemLoader.Result r);
        }

        @SuppressWarnings("unchecked")
        <T extends AbstractThread> T notSearchSubDirs() {
            this.searchSubDirs = false;
            return (T) this;
        }
        abstract void cancel();
    }

    public static class Thread extends AbstractThread {
        Thread(Context c, File[] dirs, ItemLoader iL) {
            super(c, dirs, iL);
        }

        @Override
        public void run() {
            super.run();
            if (dirs != null) {
                for (int i = 0; i < dirs.length; i++) {
                    recursivelySearchStorage(context, dirs[i]);
                }
            }
            done();
        }

        public void done() {
            if (callback != null) {
                callback.done(this, itemLoader.getResult());
            }
        }

        private void recursivelySearchStorage(final Context c, final File file) {
            if (interrupted() || file == null) {
                return;
            }
            if (file.isFile()) {
                return;
            }
            itemLoader.onNewDir(c, file);
            File[] files = file.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    itemLoader.onFile(c, files[i]);
                }
                itemLoader.onDirDone(c);
                if (searchSubDirs) {
                    //search sub-directories
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].isDirectory()) {
                            recursivelySearchStorage(c, files[i]);
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
