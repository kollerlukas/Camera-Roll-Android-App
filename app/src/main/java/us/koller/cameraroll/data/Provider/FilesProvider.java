package us.koller.cameraroll.data.Provider;

import android.app.Activity;
import android.content.Context;

import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.data.Provider.Retriever.Retriever;
import us.koller.cameraroll.data.Provider.Retriever.StorageRetriever;
import us.koller.cameraroll.data.StorageRoot;

public class FilesProvider extends Provider {

    public interface Callback {
        void onDirLoaded(File_POJO dir);

        void timeout();

        void needPermission();
    }

    private Retriever retriever;

    public FilesProvider(Context context) {
        super(context);
        retriever = new StorageRetriever();
    }


    public static StorageRoot[] getRoots(Activity context) {
        return StorageRetriever.loadRoots(context);
    }

    public void loadDir(final Activity context, String dirPath,
                        final FilesProvider.Callback callback) {

        ((StorageRetriever) retriever).loadDir(context, dirPath,
                new Callback() {
                    @Override
                    public void onDirLoaded(File_POJO dir) {
                        callback.onDirLoaded(dir);
                    }

                    @Override
                    public void timeout() {
                        callback.timeout();
                    }

                    @Override
                    public void needPermission() {
                        callback.needPermission();
                    }
                });
    }
}
