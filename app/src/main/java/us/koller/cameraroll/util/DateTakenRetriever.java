package us.koller.cameraroll.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.media.ExifInterface;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import us.koller.cameraroll.data.models.AlbumItem;

public class DateTakenRetriever {

    private ArrayList<AlbumItem> queue;

    private boolean running;

    private Callback callback;

    public interface Callback {
        void done();
    }

    public DateTakenRetriever() {
        running = false;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public Callback getCallback() {
        return callback;
    }

    public void retrieveDate(Context context, AlbumItem albumItem) {
        if (queue == null) {
            queue = new ArrayList<>();
        }
        queue.add(albumItem);
        if (!running) {
            startRetrieving(context);
        }
    }

    private void startRetrieving(final Context context) {
        AsyncTask.execute(() -> {
            running = true;
            while (queue.size() > 0) {
                AlbumItem albumItem = queue.get(0);
                Log.d("DateTakenRetriever", "tryToRetrieveDateTaken: " + albumItem.getName());
                tryToRetrieveDateTaken(context, albumItem);
                queue.remove(albumItem);
            }
            running = false;

            if (getCallback() != null) {
                getCallback().done();
            }
        });
    }

    //synchronous
    private static void tryToRetrieveDateTaken(final Context context, final AlbumItem albumItem) {
        long dateTaken = getExifDateTaken(context, albumItem);
        if (dateTaken != -1) {
            albumItem.setDate(dateTaken);
            return;
        }

        //exif didn't work try MediaStore
        tryToLoadDateTakenFromMediaStore(context, albumItem);
    }

    private static long getExifDateTaken(Context context, AlbumItem albumItem) {
        String mimeType = MediaType.getMimeType(context, albumItem.getUri(context));
        if (MediaType.doesSupportExifMimeType(mimeType)) {
            ExifInterface exif = ExifUtil.getExifInterface(context, albumItem);

            if (exif != null) {
                String dateTakenString = String.valueOf(ExifUtil.getCastValue(exif, ExifInterface.TAG_DATETIME));
                if (dateTakenString != null && !dateTakenString.equals("null")) {
                    Locale locale = Util.getLocale(context);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", locale);
                    try {
                        Date dateTaken = sdf.parse(dateTakenString);
                        return dateTaken.getTime();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return -1;
    }

    private static void tryToLoadDateTakenFromMediaStore(final Context context, final AlbumItem albumItem) {
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
                albumItem.setDate(dateTaken);
            }
            cursor.close();
        }
    }
}
