package us.koller.cameraroll.data.MediaLoader.Loader;

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
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.MediaLoader.MediaLoader;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.SortUtil;

//loading media by searching through Storage
//advantage: all items, disadvantage: slower than MediaStore
public class StorageLoader implements MediaLoader.Loader {

    //option to set thread count;
    //if set to -1 every dir in home dir get its own thread
    private static final int THREAD_COUNT = -1;

    private ArrayList<Thread> threads;

    @Override
    public void loadAlbums(final Activity context, final boolean hiddenFolders,
                           final MediaLoader.LoaderCallback callback) {

        final long startTime = System.currentTimeMillis();

        final ArrayList<Album> albums = new ArrayList<>();

        //handle timeout
        final Handler handler = new Handler();
        final Runnable timeout = new Runnable() {
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
                searchStorage(context, albums, new MediaLoader.Callback() {
                    @Override
                    public void callback(ArrayList<Album> albums) {
                        if (!hiddenFolders) {
                            for (int i = albums.size() - 1; i >= 0; i--) {
                                if (albums.get(i).isHidden()) {
                                    albums.remove(i);
                                }
                            }
                        }

                        //done loading media from storage
                        SortUtil.sortAlbums(context, albums, SortUtil.BY_NAME);
                        callback.onMediaLoaded(albums);
                        handler.removeCallbacks(timeout);
                        if (THREAD_COUNT == -1) {
                            Log.d("StorageLoader", "onMediaLoaded(): " + String.valueOf(System.currentTimeMillis() - startTime));
                        } else {
                            Log.d("StorageLoader", "onMediaLoaded(" + String.valueOf(THREAD_COUNT)
                                    + "): " + String.valueOf(System.currentTimeMillis() - startTime));
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        //cancel all mediaLoaderThreads when Activity is being destroyed
        if (threads != null) {
            for (int i = 0; i < threads.size(); i++) {
                threads.get(i).cancel();
            }
        }
    }

    private void searchStorage(final Activity context, final ArrayList<Album> albums, final MediaLoader.Callback callback) {
        File dir = Environment.getExternalStorageDirectory(); //new File("/storage/emulated/0");
        File[] dirs = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return !file.getName().equals("Android");
            }
        });

        threads = new ArrayList<>();

        Thread.Callback threadCallback = new Thread.Callback() {
            @Override
            public void done(Thread thread,
                             ArrayList<Album> albumsToAdd) {
                mergeAlbums(albums, albumsToAdd);
                threads.remove(thread);
                thread.cancel();
                if (threads.size() == 0) {
                    callback.callback(albums);
                    threads = null;
                }
            }
        };

        if (THREAD_COUNT == -1) {
            for (int i = 0; i < dirs.length; i++) {
                final File[] files = {dirs[i]};
                Thread mediaLoaderThread = new Thread(context, files, threadCallback);
                mediaLoaderThread.start();
                threads.add(mediaLoaderThread);
            }
        } else {
            //overhead is to big!!
            final File[][] threadDirs = divideDirs(dirs);

            for (int i = 0; i < THREAD_COUNT; i++) {
                final File[] files = threadDirs[i];
                Thread thread = new Thread(context, files, threadCallback);
                thread.start();
                threads.add(thread);
            }
        }
    }

    private void mergeAlbums(ArrayList<Album> albums, ArrayList<Album> albumsToAdd) {
        for (int i = albumsToAdd.size() - 1; i >= 0; i--) {
            for (int k = 0; k < albums.size(); k++) {
                if (albumsToAdd.get(i).getPath()
                        .equals(albums.get(k).getPath())) {
                    albumsToAdd.remove(i);
                    break;
                }
            }
        }
        albums.addAll(albumsToAdd);
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

        Log.d("StorageLoader", Arrays.toString(threadDirs_sizes));

        File[][] threadDirs = new File[THREAD_COUNT][dirs.length / THREAD_COUNT + 1];
        int index = 0;
        for (int i = 0; i < THREAD_COUNT; i++) {
            File[] threadDir = Arrays.copyOfRange(dirs, index, index + threadDirs_sizes[i]);
            threadDirs[i] = threadDir;
            index = index + threadDirs_sizes[i];
        }

        return threadDirs;
    }

    private static class Thread extends java.lang.Thread {

        interface Callback {
            void done(Thread thread, ArrayList<Album> albums);
        }

        private Activity context;
        private Callback callback;

        private File[] dirs;

        Thread(Activity context, File[] dirs, Callback callback) {
            this.context = context;
            this.callback = callback;
            this.dirs = dirs;
        }

        @Override
        public void run() {
            super.run();

            final ArrayList<Album> albums = new ArrayList<>();

            if (dirs != null) {
                for (int i = 0; i < dirs.length; i++) {
                    recursivelySearchStorage(context, dirs[i], albums);
                }
            }

            if (callback != null) {
                callback.done(this, albums);
            }
        }

        private void recursivelySearchStorage(final Activity context,
                                              final File file,
                                              final ArrayList<Album> albums) {
            if (interrupted() || file == null) {
                return;
            }

            if (file.isFile()) {
                return;
            }

            final Album album = new Album().setPath(file.getPath());

            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (MediaType.isMedia(context, files[i].getPath())) {
                    AlbumItem albumItem
                            = AlbumItem.getInstance(context, files[i].getPath());
                    if (albumItem != null) {
                        album.getAlbumItems()
                                .add(albumItem);
                    }
                } else if (file.isDirectory()) {
                    recursivelySearchStorage(context, files[i], albums);
                }
            }

            if (album.getAlbumItems().size() > 0) {
                albums.add(album);
            }
        }

        void cancel() {
            context = null;
            callback = null;
            interrupt();
        }
    }
}
