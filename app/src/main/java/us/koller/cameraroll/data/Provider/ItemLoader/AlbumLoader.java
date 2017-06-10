package us.koller.cameraroll.data.Provider.ItemLoader;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.util.MediaType;

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
    public void onFile(final Activity context, File file) {
        if (MediaType.isMedia(file.getPath())) {
            final AlbumItem albumItem
                    = AlbumItem.getInstance(context, file.getPath());
            if (albumItem != null) {
                //tryToLoadDateTakenFromMediaStore(context, albumItem);
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

    //synchronous
    public static void tryToLoadDateTakenFromMediaStore(final Context context, final AlbumItem albumItem) {
        String[] projection = {MediaStore.Images.ImageColumns.DATE_TAKEN};

        String selection = MediaStore.Images.Media.DATA + " = ?";

        Uri queryUri = MediaStore.Files.getContentUri("external");

        final Cursor cursor = context.getContentResolver()
                .query(queryUri,
                        projection,
                        selection,
                        new String[]{albumItem.getPath()},
                        null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int dateAddedColumn = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN);
                long dateTaken = cursor.getLong(dateAddedColumn);
                Log.d("AlbumLoader", "dateTaken = " + String.valueOf(dateTaken));
                albumItem.setDate(dateTaken);
            } else {
                Log.d("AlbumLoader", "cursor.moveToFirst() = false");
            }
            cursor.close();
        } else {
            Log.d("AlbumLoader", "cursor = null");
        }
    }
}
