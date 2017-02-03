package us.koller.cameraroll.data.MediaLoader.Loader;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.MediaLoader.MediaLoader;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.SortUtil;

public class StorageLoader implements MediaLoader.Loader {

    private ArrayList<Thread> mediaLoaderThreads;

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
                                if (albums.get(i).hiddenAlbum) {
                                    albums.remove(i);
                                }
                            }
                        }

                        //done loading media from storage
                        SortUtil.sortAlbums(context, albums, SortUtil.BY_NAME);
                        callback.onMediaLoaded(albums);
                        handler.removeCallbacks(timeout);
                        Log.d("StorageLoader", "onMediaLoaded(): " + String.valueOf(System.currentTimeMillis() - startTime));
                    }
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        //cancel all mediaLoaderThreads when Activity is being destroyed
        if (mediaLoaderThreads != null) {
            for (int i = 0; i < mediaLoaderThreads.size(); i++) {
                mediaLoaderThreads.get(i).cancel();
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

        mediaLoaderThreads = new ArrayList<>();

        for (int i = 0; i < dirs.length; i++) {
            final File file = dirs[i];
            Thread mediaLoaderThread
                    = new Thread(context, file, new Thread.Callback() {
                @Override
                public void done(Thread thread, ArrayList<Album> albumsToAdd) {
                    mergeAlbums(albums, albumsToAdd);
                    mediaLoaderThreads.remove(thread);
                    thread.cancel();
                    if (mediaLoaderThreads.size() == 0) {
                        callback.callback(albums);
                        mediaLoaderThreads = null;
                    }
                }
            });
            mediaLoaderThread.start();
            mediaLoaderThreads.add(mediaLoaderThread);
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

    public static class Thread extends java.lang.Thread {

        interface Callback {
            void done(Thread thread, ArrayList<Album> albums);
        }

        private Activity context;
        private Callback callback;

        private File dir;

        Thread(Activity context, File dir, Callback callback) {
            this.context = context;
            this.callback = callback;
            this.dir = dir;
        }

        @Override
        public void run() {
            super.run();

            Looper.prepare();

            ArrayList<Album> albums = new ArrayList<>();

            if (dir != null) {
                recursivelySearchStorage(context, dir, albums);
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

            if (!file.isDirectory()) {
                return;
            }

            final Album album = new Album().setPath(file.getPath());

            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (MediaType.isMedia(context, files[i].getPath())) {
                    AlbumItem albumItem = AlbumItem
                            .getInstance(context, files[i].getPath());
                    album.getAlbumItems().add(albumItem);
                } else if (file.isDirectory()) {
                    recursivelySearchStorage(context, files[i], albums);
                }
            }

            if (album.getAlbumItems().size() > 0) {
                SortUtil.sort(context, album.getAlbumItems(), SortUtil.BY_DATE);
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
