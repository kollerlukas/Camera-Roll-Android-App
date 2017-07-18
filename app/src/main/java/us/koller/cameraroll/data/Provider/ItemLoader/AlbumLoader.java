package us.koller.cameraroll.data.Provider.ItemLoader;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.ui.MainActivity;
import us.koller.cameraroll.util.DateTakenRetriever;
import us.koller.cameraroll.util.MediaType;

public class AlbumLoader extends ItemLoader {

    private DateTakenRetriever dateRetriever;

    private ArrayList<Album> albums;

    private Album currentAlbum;

    AlbumLoader() {
        albums = new ArrayList<>();
    }

    public void setDateRetriever(DateTakenRetriever dateRetriever) {
        this.dateRetriever = dateRetriever;
    }

    @Override
    public void onNewDir(final Activity context, File dir) {
        currentAlbum = new Album().setPath(dir.getPath());

        //loading dateTaken timeStamps asynchronously
        if (dateRetriever != null && dateRetriever.getCallback() == null) {
            dateRetriever.setCallback(new DateTakenRetriever.Callback() {
                @Override
                public void done() {
                    Log.d("AlbumLoader", "done: ");
                    Intent intent = new Intent(context, MainActivity.class);
                    intent.setAction(MainActivity.RESORT);
                    context.startActivity(intent);
                }
            });
        }
    }

    @Override
    public void onFile(final Activity context, File file) {
        if (MediaType.isMedia(file.getPath())) {
            final AlbumItem albumItem
                    = AlbumItem.getInstance(file.getPath());
            if (albumItem != null) {
                if (dateRetriever != null) {
                    dateRetriever.retrieveDate(context, albumItem);
                }
                currentAlbum.getAlbumItems().add(albumItem);
            }
        }
    }

    @Override
    public void onDirDone(Activity context) {
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
