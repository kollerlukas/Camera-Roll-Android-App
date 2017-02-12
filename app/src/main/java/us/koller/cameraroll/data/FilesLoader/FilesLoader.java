package us.koller.cameraroll.data.FilesLoader;

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

import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.data.MediaLoader.MediaLoader;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.SortUtil;

public class FilesLoader {

    public interface LoaderCallback {
        void onMediaLoaded(File_POJO files);

        void timeout();

        void needPermission();
    }

    public interface Callback {
        void callback(File_POJO files);
    }

    //option to set thread count;
    //if set to -1 every dir in home dir get its own thread
    private static final int THREAD_COUNT = -1;

    private ArrayList<Thread> threads;

    //for timeout
    private Handler handler;
    private Runnable timeout;

    public void loadFiles(final Activity context,
                          final LoaderCallback callback) {

        if (!MediaLoader.checkPermission(context)) {
            callback.needPermission();
            return;
        }

        final long startTime = System.currentTimeMillis();

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

        //load files from storage
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                searchStorage(context, new FilesLoader.Callback() {
                    @Override
                    public void callback(File_POJO files) {
                        //sort files by name
                        SortUtil.sortFiles(context, files);

                        //done loading files from storage
                        callback.onMediaLoaded(files);
                        cancelTimeout();
                        if (THREAD_COUNT == -1) {
                            Log.d("FilesLoader", "onMediaLoaded(): " + String.valueOf(System.currentTimeMillis() - startTime) + "; " + files.getChildren());
                        } else {
                            Log.d("FilesLoader", "onMediaLoaded(" + String.valueOf(THREAD_COUNT)
                                    + "): " + String.valueOf(System.currentTimeMillis() - startTime) + "; " + files.getChildren());
                        }
                    }
                });
            }
        });
    }

    private void cancelTimeout() {
        if (handler != null && timeout != null) {
            handler.removeCallbacks(timeout);
        }
    }

    public void onDestroy() {
        //cancel all threads when Activity is being destroyed
        if (threads != null) {
            for (int i = 0; i < threads.size(); i++) {
                threads.get(i).cancel();
            }
        }
        cancelTimeout();
    }

    private void searchStorage(final Activity context, final FilesLoader.Callback callback) {
        final File_POJO files = new File_POJO(Environment.getExternalStorageDirectory().toString(), false);
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
                             ArrayList<File_POJO> filesToAdd) {
                for (int i = 0; i < filesToAdd.size(); i++) {
                    files.addChild(filesToAdd.get(i));
                }
                threads.remove(thread);
                thread.cancel();
                if (threads.size() == 0) {
                    callback.callback(files);
                    threads = null;
                }
            }
        };

        if (THREAD_COUNT == -1) {
            for (int i = 0; i < dirs.length; i++) {
                final File[] threadFiles = {dirs[i]};
                Thread thread = new Thread(context, threadFiles, threadCallback);
                thread.start();
                threads.add(thread);
            }
        } else {
            //overhead is to big!!
            final File[][] threadDirs = divideDirs(dirs);

            for (int i = 0; i < THREAD_COUNT; i++) {
                final File[] threadFiles = threadDirs[i];
                Thread thread = new Thread(context, threadFiles, threadCallback);
                thread.start();
                threads.add(thread);
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

        Log.d("FilesLoader", Arrays.toString(threadDirs_sizes));

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
            void done(Thread thread, ArrayList<File_POJO> files);
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

            final ArrayList<File_POJO> files = new ArrayList<>();

            if (dirs != null) {
                for (int i = 0; i < dirs.length; i++) {
                    File_POJO file_pojo = new File_POJO(dirs[i].getPath(),
                            MediaType.isMedia(context, dirs[i].getPath()));
                    recursivelySearchStorage(context, dirs[i], file_pojo);
                    files.add(file_pojo);
                }
            }

            if (callback != null) {
                callback.done(this, files);
            }
        }

        private void recursivelySearchStorage(final Activity context,
                                              final File file,
                                              final File_POJO files) {
            if (interrupted() || file == null) {
                return;
            }

            if (file.isFile()) {
                return;
            }

            java.io.File[] filesToSearch = file.listFiles();

            for (int i = 0; i < filesToSearch.length; i++) {
                boolean isMedia = MediaType.isMedia(context, filesToSearch[i].getPath());
                if (filesToSearch[i].isDirectory() || isMedia) {
                    File_POJO file_pojo = new File_POJO(filesToSearch[i].getPath(), isMedia);
                    files.addChild(file_pojo);
                    if (filesToSearch[i].isDirectory()) {
                        recursivelySearchStorage(context, filesToSearch[i], file_pojo);
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
}
