package us.koller.cameraroll.data.Provider.Retriever;

import android.app.Activity;

import us.koller.cameraroll.data.Provider.MediaProvider;
import us.koller.cameraroll.data.Provider.Provider;

public abstract class Retriever {

    private MediaProvider.Callback callback;

    public void loadAlbums(final Activity context, final boolean hiddenFolders, final MediaProvider.Callback callback) {
        setCallback(callback);
        loadAlbums(context, hiddenFolders);
    }

    abstract void loadAlbums(final Activity context, final boolean hiddenFolders);

    public void onDestroy() {
        setCallback(null);
    }

    public void setCallback(MediaProvider.Callback callback) {
        this.callback = callback;
    }

    public MediaProvider.Callback getCallback() {
        return callback;
    }
}
