package us.koller.cameraroll.data.Provider;

import android.app.Activity;

import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.data.Provider.Retriever.Retriever;
import us.koller.cameraroll.data.Provider.Retriever.StorageRetriever;

public class FilesProvider extends Provider {

    /*public interface Callback {
        void onFilesLoaded(File_POJO files);

        void timeout();

        void needPermission();
    }*/

    public interface Callback {
        void onDirLoaded(File_POJO dir);

        void timeout();

        void needPermission();
    }

    private Retriever retriever;

    public FilesProvider() {
        retriever = new StorageRetriever();
    }

    /*public void loadFiles(Activity context, final Callback callback) {

        if (!MediaProvider.checkPermission(context)) {
            callback.needPermission();
            return;
        }

        retriever = new StorageRetriever();
        ((StorageRetriever) retriever).loadFiles(context, callback);
    }*/

    public File_POJO[] getRoots() {
        return ((StorageRetriever) retriever).loadRoots();
    }

    public void loadDir(Activity context, String dirPath,
                        final FilesProvider.Callback callback) {
        ((StorageRetriever) retriever).loadDir(context, dirPath, callback);
    }
}
