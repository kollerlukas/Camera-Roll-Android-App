package us.koller.cameraroll.data.Provider.Retriever;

import android.app.Activity;

import us.koller.cameraroll.data.Provider.MediaProvider;

public interface Retriever {

    public void loadAlbums(final Activity context, final boolean hiddenFolders, final MediaProvider.Callback callback);

    public void onDestroy();
}
