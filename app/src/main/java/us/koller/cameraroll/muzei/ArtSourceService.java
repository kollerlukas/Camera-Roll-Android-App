package us.koller.cameraroll.muzei;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.media.ExifInterface;
import android.util.Log;

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
import us.koller.cameraroll.data.provider.itemLoader.AlbumLoader;
import us.koller.cameraroll.data.provider.itemLoader.ItemLoader;
import us.koller.cameraroll.data.provider.retriever.StorageRetriever;
import us.koller.cameraroll.util.ExifUtil;
import us.koller.cameraroll.util.InfoUtil;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.Util;

@SuppressLint("Registered")
public class ArtSourceService extends MuzeiArtSource {

    private static final String NAME = "us.koller.cameraroll.muzei.ArtSourceService";

    private static final long SCHEDULE_TIME_INTERVAL = 6 * 60 * 60 * 1000; //every 6 hours

    private Album album;

    public ArtSourceService() {
        super(NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //set UsedCommand Next Artwork
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onDisabled() {
        super.onDisabled();

        unscheduleUpdate();
        removeAllUserCommands();
    }

    @Override
    protected void onUpdate(int reason) {
        loadCameraImages();

        //schedule next update
        scheduleUpdate(System.currentTimeMillis() + SCHEDULE_TIME_INTERVAL);
    }


    private void loadCameraImages() {
        final File[] files = {Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)};
        StorageRetriever.Thread.Callback callback =
                new StorageRetriever.Thread.Callback() {
                    @Override
                    public void done(StorageRetriever.Thread thread, ItemLoader.Result result) {
                        thread.cancel();

                        album = new Album();
                        ArrayList<Album> albums = result.albums;
                        for (int i = 0; i < albums.size(); i++) {
                            album.getAlbumItems().addAll(albums.get(i).getAlbumItems());
                        }

                        publishArtwork();
                    }
                };

        StorageRetriever.Thread thread =
                new StorageRetriever.Thread(getApplicationContext(), files, new AlbumLoader())
                        .setCallback(callback);
        thread.start();
    }

    private void publishArtwork() {
        Random r = new Random(System.currentTimeMillis());
        int index = r.nextInt(album.getAlbumItems().size());

        AlbumItem albumItem = album.getAlbumItems().get(index);

        Uri imageUri = albumItem.getUri(getApplicationContext());
        String mimeType = MediaType.getMimeType(getApplicationContext(), imageUri);
        String imageTitle = albumItem.getName();
        String imageData = getImageData(getApplicationContext(), albumItem);

        Log.d("ArtSourceService", "publishArtwork: " + imageUri);

        publishArtwork(new Artwork.Builder()
                .imageUri(imageUri)
                .title(imageTitle)
                .byline(imageData)
                .viewIntent(new Intent(Intent.ACTION_VIEW).setDataAndType(imageUri, mimeType))
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

        /*Location*/
        String location = null;
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
                    location = address.getLocality() + ", " + address.getAdminArea();
                }
            }
        }
        if (location != null) {
            return location;
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
        return formattedDate;
    }
}
