package us.koller.cameraroll.data.Provider.ItemLoader;

import android.app.Activity;

import java.io.File;
import java.util.ArrayList;

import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.SortUtil;

public class AlbumLoader extends ItemLoader {

    private ArrayList<Album> albums;

    private Album currentAlbum;

    AlbumLoader() {
        albums = new ArrayList<>();
    }

    @Override
    public void onNewDir(Activity context, File dir) {
        currentAlbum = new Album().setPath(dir.getPath());
    }

    @Override
    public void onFile(Activity context, File file) {
        if (MediaType.isMedia(file.getPath())) {
            AlbumItem albumItem
                    = AlbumItem.getInstance(context, file.getPath());
            if (albumItem != null) {
                currentAlbum.getAlbumItems()
                        .add(albumItem);
            }
        }
    }

    @Override
    public void onDirDone(Activity context) {
        if (currentAlbum != null && currentAlbum.getAlbumItems().size() > 0) {
            SortUtil.sortByDate(context, currentAlbum.getAlbumItems());
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
