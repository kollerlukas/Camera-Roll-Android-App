package us.koller.cameraroll.data.MediaLoader;

import android.app.Activity;
import android.os.Looper;
import android.util.Log;

/*import org.lukhnos.nnio.file.DirectoryStream;
import org.lukhnos.nnio.file.FileSystems;
import org.lukhnos.nnio.file.Files;
import org.lukhnos.nnio.file.Path;*/

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.util.MediaType;

class MediaLoaderThread extends java.lang.Thread {

    interface Callback {
        void done(MediaLoaderThread mediaLoaderThread, ArrayList<Album> albums);
    }

    private Activity context;
    private Callback callback;

    private File dir;

    MediaLoaderThread(Activity context, File dir, Callback callback) {
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
        album.hiddenAlbum = file.isHidden();

        File[] files = file.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (file.isDirectory()) {
                recursivelySearchStorage(context, files[i], albums);
            } else if (MediaType.isMedia(context, files[i].getPath())) {
                AlbumItem albumItem = AlbumItem
                        .getInstance(context, files[i].getPath());
                album.getAlbumItems().add(albumItem);
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
