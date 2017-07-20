package us.koller.cameraroll.muzei;

import android.content.Context;
import android.location.Address;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.media.ExifInterface;
import android.util.Log;
import android.widget.Toast;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.MuzeiArtSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.Provider.ItemLoader.AlbumLoader;
import us.koller.cameraroll.data.Provider.ItemLoader.ItemLoader;
import us.koller.cameraroll.data.Provider.Retriever.StorageRetriever;
import us.koller.cameraroll.util.ExifUtil;
import us.koller.cameraroll.util.InfoUtil;
import us.koller.cameraroll.util.Util;

public class ArtSourceService extends MuzeiArtSource {

    private static final String NAME = "us.koller.cameraroll.muzei.ArtSourceService";

    private static final long SCHEDULE_TIME_INTERVALL = 24 * 60 * 60 * 1000; //every day

    private Album album;

    public ArtSourceService() {
        super(NAME);
    }

    @Override
    protected void onEnabled() {
        super.onEnabled();

        scheduleUpdate(100);
    }

    @Override
    protected void onDisabled() {
        super.onDisabled();

        unscheduleUpdate();
        removeAllUserCommands();
    }

    @Override
    protected void onUpdate(int reason) {
        Log.d("ArtSourceService", "onUpdate() called with: reason = [" + reason + "]");

        //set UsedCommand Next Artwork
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);

        if (reason == UPDATE_REASON_USER_NEXT) {
            Toast.makeText(this, "Next Picture", Toast.LENGTH_SHORT).show();
        }

        loadCameraImages();

        //schedule next update
        scheduleUpdate(SCHEDULE_TIME_INTERVALL);
    }


    private void loadCameraImages() {
        final File[] files = {Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)};
        StorageRetriever.Thread.Callback callback =
                new StorageRetriever.Thread.Callback() {
                    @Override
                    public void done(StorageRetriever.Thread thread, ItemLoader.Result result) {
                        thread.interrupt();

                        album = new Album();
                        ArrayList<Album> albums = result.albums;
                        for (int i = 0; i < albums.size(); i++) {
                            album.getAlbumItems().addAll(albums.get(i).getAlbumItems());
                        }

                        publishArtwork();
                    }
                };

        StorageRetriever.Thread thread =
                new StorageRetriever.Thread(getApplicationContext(), files, callback, AlbumLoader.class);
        thread.start();
    }

    private void publishArtwork() {
        Random r = new Random(System.currentTimeMillis());
        int index = r.nextInt(album.getAlbumItems().size());

        AlbumItem albumItem = album.getAlbumItems().get(index);

        Uri image = albumItem.getUri(getApplicationContext());
        String imageTitle = albumItem.getName();
        String imageData = getImageData(getApplicationContext(), albumItem);

        publishArtwork(new Artwork.Builder()
                .imageUri(image)
                .title(imageTitle)
                .byline(imageData)
                .build());
    }

    private String getImageData(Context context, AlbumItem albumItem) {
        ExifInterface exif = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri uri = albumItem.getUri(context);
                InputStream is = context.getContentResolver().openInputStream(uri);
                if (is != null) {
                    exif = new ExifInterface(is);
                }

            } else {
                exif = new ExifInterface(albumItem.getPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String formattedDate;
        Date date = null;
        Locale locale = Util.getLocale(context);
        if (exif != null) {
            try {
                String dateString = String.valueOf(ExifUtil.getCastValue(exif, ExifInterface.TAG_DATETIME));
                date = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", locale).parse(dateString);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        if (date == null) {
            date = new Date(albumItem.getDate());
        }
        formattedDate = new SimpleDateFormat("EEE, d MMM yyyy", locale).format(date);

        /*Location*/
        String location = "";
        if (exif != null) {
            Object latitudeObject = ExifUtil.getCastValue(exif, ExifInterface.TAG_GPS_LATITUDE);
            Object longitudeObject = ExifUtil.getCastValue(exif, ExifInterface.TAG_GPS_LONGITUDE);
            if (latitudeObject != null && longitudeObject != null) {
                boolean positiveLat = ExifUtil.getCastValue(exif, ExifInterface.TAG_GPS_LATITUDE_REF).equals("N");
                double latitude = Double.parseDouble(InfoUtil.parseGPSLongOrLat(String.valueOf(latitudeObject), positiveLat));

                boolean positiveLong = ExifUtil.getCastValue(exif, ExifInterface.TAG_GPS_LONGITUDE_REF).equals("E");
                double longitude = Double.parseDouble(InfoUtil.parseGPSLongOrLat(String.valueOf(longitudeObject), positiveLong));
                Address address = InfoUtil.retrieveAddredd(context, latitude, longitude);
                if (address != null) {
                    location = address.getLocality() + ", " + address.getAdminArea() + ", ";
                }
            }
        }
        return location + formattedDate;
    }
}
