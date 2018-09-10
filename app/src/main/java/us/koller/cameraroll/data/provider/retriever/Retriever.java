package us.koller.cameraroll.data.provider.retriever;

import android.app.Activity;

import us.koller.cameraroll.data.provider.MediaProvider;

public abstract class Retriever {

    private MediaProvider.OnMediaLoadedCallback callback;

    public void loadAlbums(final Activity c, final boolean hF, final MediaProvider.OnMediaLoadedCallback ca) {
        setCallback(ca);
        loadAlbums(c, hF);
    }

    abstract void loadAlbums(final Activity c, final boolean hF);

    public void onDestroy() {
        setCallback(null);
    }

    public MediaProvider.OnMediaLoadedCallback getCallback() {
        return callback;
    }

    public void setCallback(MediaProvider.OnMediaLoadedCallback ca) {
        this.callback = ca;
    }
}
