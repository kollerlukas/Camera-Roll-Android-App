package us.koller.cameraroll.data.provider;

import android.app.Activity;
import android.content.Context;

import us.koller.cameraroll.data.models.File_POJO;
import us.koller.cameraroll.data.models.StorageRoot;
import us.koller.cameraroll.data.provider.retriever.Retriever;
import us.koller.cameraroll.data.provider.retriever.StorageRetriever;
import us.koller.cameraroll.util.StorageUtil;

public class FilesProvider extends Provider {

    private Retriever retriever;

    public abstract static class Callback implements Provider.Callback {
        public abstract void onDirLoaded(File_POJO dir);
    }

    public FilesProvider(Context c) {
        super(c);
        retriever = new StorageRetriever();
    }

    public static StorageRoot[] getRoots(Activity c) {
        return StorageUtil.loadRoots(c);
    }

    public void loadDir(final Activity c, String dirPath, FilesProvider.Callback ca) {
        setCallback(ca);
        ((StorageRetriever) retriever).loadFilesForDir(c, dirPath, new Callback() {
            @Override
            public void onDirLoaded(File_POJO dir) {
                Callback ca = getCallback();
                if (ca != null) {
                    ca.onDirLoaded(dir);
                }
            }

            @Override
            public void timeout() {
                Callback ca = getCallback();
                if (ca != null) {
                    ca.timeout();
                }
            }

            @Override
            public void needPermission() {
                Callback ca = getCallback();
                if (ca != null) {
                    ca.needPermission();
                }
            }
        });
    }
}
