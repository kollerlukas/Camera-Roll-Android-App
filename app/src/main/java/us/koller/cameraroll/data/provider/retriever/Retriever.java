package us.koller.cameraroll.data.provider.retriever;

import android.app.Activity;

import us.koller.cameraroll.data.provider.MediaProvider;

public abstract class Retriever {

    private MediaProvider.OnMediaLoadedCallback callback;

    public void loadAlbums(final Activity context,
                           final boolean hiddenFolders,
                           final MediaProvider.OnMediaLoadedCallback callback) {
        setCallback(callback);
        loadAlbums(context, hiddenFolders);
    }

    abstract void loadAlbums(final Activity context,
                             final boolean hiddenFolders);

    public void onDestroy() {
        setCallback(null);
    }

    public void setCallback(MediaProvider.OnMediaLoadedCallback callback) {
        this.callback = callback;
    }

    public MediaProvider.OnMediaLoadedCallback getCallback() {
        return callback;
    }
}
