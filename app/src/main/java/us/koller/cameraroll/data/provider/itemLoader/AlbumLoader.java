package us.koller.cameraroll.data.provider.itemLoader;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.util.ArrayList;

import us.koller.cameraroll.data.models.Album;
import us.koller.cameraroll.data.models.AlbumItem;
import us.koller.cameraroll.ui.MainActivity;
import us.koller.cameraroll.util.DateTakenRetriever;

public class AlbumLoader extends ItemLoader {
    private DateTakenRetriever dateRetriever;
    private ArrayList<Album> albums;
    private Album currentAlbum;

    public AlbumLoader() {
        albums = new ArrayList<>();
    }

    @Override
    public ItemLoader newInstance() {
        DateTakenRetriever dR = this.dateRetriever != null ? new DateTakenRetriever() : null;
        return new AlbumLoader().setDateRetriever(dR);
    }

    @SuppressWarnings("WeakerAccess")
    public AlbumLoader setDateRetriever(DateTakenRetriever dR) {
        this.dateRetriever = dR;
        return this;
    }

    @Override
    public void onNewDir(final Context c, File dir) {
        currentAlbum = new Album().setPath(dir.getPath());

        //loading dateTaken timeStamps asynchronously
        if (dateRetriever != null && dateRetriever.getCallback() == null) {
            dateRetriever.setCallback(() -> {
                Intent i = new Intent(MainActivity.RESORT);
                LocalBroadcastManager.getInstance(c).sendBroadcast(i);
            });
        }
    }

    @Override
    public void onFile(final Context c, File f) {
        final AlbumItem aI = AlbumItem.getInstance(c, f.getPath());
        if (aI != null) {
            if (dateRetriever != null) {
                dateRetriever.retrieveDate(c, aI);
            }
            //preload uri
            //albumItem.preloadUri(context);
            currentAlbum.getAlbumItems().add(aI);
        }
    }

    @Override
    public void onDirDone(Context c) {
        if (currentAlbum != null && currentAlbum.getAlbumItems().size() > 0) {
            albums.add(currentAlbum);
            currentAlbum = null;
        }
    }

    @Override
    public Result getResult() {
        Result r = new Result();
        r.albums = albums;
        albums = new ArrayList<>();
        return r;
    }
}
