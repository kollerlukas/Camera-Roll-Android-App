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
        DateTakenRetriever dateRetriever = this.dateRetriever != null ? new DateTakenRetriever() : null;
        return new AlbumLoader().setDateRetriever(dateRetriever);
    }

    @SuppressWarnings("WeakerAccess")
    public AlbumLoader setDateRetriever(DateTakenRetriever dateRetriever) {
        this.dateRetriever = dateRetriever;
        return this;
    }

    @Override
    public void onNewDir(final Context context, File dir) {
        currentAlbum = new Album().setPath(dir.getPath());

        //loading dateTaken timeStamps asynchronously
        if (dateRetriever != null && dateRetriever.getCallback() == null) {
            dateRetriever.setCallback(() -> {
                Intent intent = new Intent(MainActivity.RESORT);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            });
        }
    }

    @Override
    public void onFile(final Context context, File file) {
        final AlbumItem albumItem = AlbumItem.getInstance(context, file.getPath());
        if (albumItem != null) {
            if (dateRetriever != null) {
                dateRetriever.retrieveDate(context, albumItem);
            }
            //preload uri
            //albumItem.preloadUri(context);
            currentAlbum.getAlbumItems().add(albumItem);
        }
    }

    @Override
    public void onDirDone(Context context) {
        if (currentAlbum != null && currentAlbum.getAlbumItems().size() > 0) {
            albums.add(currentAlbum);
            currentAlbum = null;
        }
    }

    @Override
    public Result getResult() {
        Result result = new Result();
        result.albums = albums;
        albums = new ArrayList<>();
        return result;
    }
}
