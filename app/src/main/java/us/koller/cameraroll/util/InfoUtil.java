package us.koller.cameraroll.util;

import android.content.Context;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.media.ExifInterface;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.models.AlbumItem;
import us.koller.cameraroll.data.models.Video;

public class InfoUtil {

    public static class InfoItem {
        private String type, value;
        private int iconRes;

        public InfoItem(String type, String value) {
            this.type = type;
            this.value = value;
        }

        public InfoItem setIconRes(int iconRes) {
            this.iconRes = iconRes;
            return this;
        }

        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

        public int getIconRes() {
            return iconRes;
        }
    }

    public static class ColorsItem extends InfoItem {

        public String path;

        public ColorsItem(String path) {
            super("Colors", null);
            this.path = path;
        }
    }

    public static class LocationItem extends InfoItem {

        LocationItem(String type, String value) {
            super(type, value);
        }
    }

    public static String retrieveFileName(Context context, Uri uri) {
        //retrieve file name
        try {
            Cursor cursor = context.getContentResolver().query(uri,
                    new String[]{OpenableColumns.DISPLAY_NAME},
                    null, null, null);
            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                cursor.moveToFirst();
                if (!cursor.isAfterLast()) {
                    String filename = cursor.getString(nameIndex);
                    cursor.close();
                    return filename;
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static InfoItem retrieveFileSize(Context context, Uri uri) {
        //retrieve fileSize form MediaStore
        Cursor cursor = context.getContentResolver().query(
                uri, null, null,
                null, null);
        long size = 0;
        if (cursor != null && !cursor.isAfterLast()) {
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                size = cursor.getLong(sizeIndex);
                cursor.close();
            }
        }
        return new InfoItem(context.getString(R.string.info_size), Parser.parseFileSize(context, size));
    }

    public static InfoItem retrieveDimensions(Context context, ExifInterface exif, AlbumItem albumItem) {
        if (exif != null) {
            String height = String.valueOf(ExifUtil.getCastValue(exif, ExifInterface.TAG_IMAGE_LENGTH));
            String width = String.valueOf(ExifUtil.getCastValue(exif, ExifInterface.TAG_IMAGE_WIDTH));
            return new InfoItem(context.getString(R.string.info_dimensions), width + " x " + height);
        }
        /*Exif not supported/working for this image*/
        int[] imageDimens = albumItem.getImageDimens(context);
        return new InfoItem(context.getString(R.string.info_dimensions),
                String.valueOf(imageDimens[0]) + " x " + String.valueOf(imageDimens[1]));
    }

    public static InfoItem retrieveFormattedDate(Context context, ExifInterface exif, AlbumItem albumItem) {
        Locale locale = Util.getLocale(context);
        if (exif != null) {
            String dateString = String.valueOf(ExifUtil.getCastValue(exif, ExifInterface.TAG_DATETIME));
            try {
                Date date = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", locale).parse(dateString);
                String formattedDate = new SimpleDateFormat("EEE d MMM yyyy HH:mm", locale).format(date);
                return new InfoItem(context.getString(R.string.info_date), formattedDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        String formattedDate = new SimpleDateFormat("EEE d MMM yyyy HH:mm", locale)
                .format(new Date(albumItem.getDate()));
        return new InfoItem(context.getString(R.string.info_date), formattedDate);
    }

    public static LocationItem retrieveLocation(Context context, ExifInterface exif) {
        if (exif != null) {
            Object latitudeObject = ExifUtil.getCastValue(exif, ExifInterface.TAG_GPS_LATITUDE);
            Object longitudeObject = ExifUtil.getCastValue(exif, ExifInterface.TAG_GPS_LONGITUDE);
            if (latitudeObject != null && longitudeObject != null) {
                boolean positiveLat = ExifUtil.getCastValue(exif, ExifInterface.TAG_GPS_LATITUDE_REF).equals("N");
                double latitude = Double.parseDouble(Parser.parseGPSLongOrLat(String.valueOf(latitudeObject), positiveLat));

                boolean positiveLong = ExifUtil.getCastValue(exif, ExifInterface.TAG_GPS_LONGITUDE_REF).equals("E");
                double longitude = Double.parseDouble(Parser.parseGPSLongOrLat(String.valueOf(longitudeObject), positiveLong));
                String locationString = latitude + "," + longitude;

                return new LocationItem(context.getString(R.string.info_location), locationString);
            }
        }
        return new LocationItem(context.getString(R.string.info_location), ExifUtil.NO_DATA);
    }

    public static InfoItem retrieveFocalLength(Context context, ExifInterface exif) {
        Object focalLengthObject = ExifUtil.getCastValue(exif, ExifInterface.TAG_FOCAL_LENGTH);
        String focalLength;
        if (focalLengthObject != null) {
            focalLength = String.valueOf(focalLengthObject);
            Rational r = Rational.parseRational(focalLength);
            if (r != null) {
                focalLength = String.valueOf(r.floatValue()) + " mm";
            }
        } else {
            focalLength = ExifUtil.NO_DATA;
        }
        return new InfoItem(context.getString(R.string.info_focal_length), focalLength);
    }

    public static InfoItem retrieveExposure(Context context, ExifInterface exif) {
        Object exposureObject = String.valueOf(ExifUtil.getCastValue(exif, ExifInterface.TAG_EXPOSURE_TIME));
        String exposure;
        if (exposureObject != null) {
            exposure = Parser.parseExposureTime(String.valueOf(exposureObject));
        } else {
            exposure = ExifUtil.NO_DATA;
        }
        return new InfoItem(context.getString(R.string.info_exposure), exposure);
    }

    public static InfoItem retrieveModelAndMake(Context context, ExifInterface exif) {
        Object makeObject = ExifUtil.getCastValue(exif, ExifInterface.TAG_MAKE);
        Object modelObject = ExifUtil.getCastValue(exif, ExifInterface.TAG_MODEL);
        String model;
        if (makeObject != null && modelObject != null) {
            model = String.valueOf(makeObject) + " " + String.valueOf(modelObject);
        } else {
            model = ExifUtil.NO_DATA;
        }
        return new InfoItem(context.getString(R.string.info_camera_model), model);
    }

    public static InfoItem retrieveAperture(Context context, ExifInterface exif) {
        Object apertureObject = ExifUtil.getCastValue(exif, ExifInterface.TAG_F_NUMBER);
        String aperture;
        if (apertureObject != null) {
            aperture = "f/" + String.valueOf(apertureObject);
        } else {
            aperture = ExifUtil.NO_DATA;
        }
        return new InfoItem(context.getString(R.string.info_aperture), aperture);
    }

    public static InfoItem retrieveISO(Context context, ExifInterface exif) {
        Object isoObject = ExifUtil.getCastValue(exif, ExifInterface.TAG_ISO_SPEED_RATINGS);
        String iso;
        if (isoObject != null) {
            iso = String.valueOf(isoObject);
        } else {
            iso = ExifUtil.NO_DATA;
        }
        return new InfoItem(context.getString(R.string.info_iso), iso);
    }

    public static InfoItem retrieveVideoFrameRate(Context context, AlbumItem albumItem) {
        int frameRate = ((Video) albumItem).retrieveFrameRate();
        String frameRateString;
        if (frameRate != -1) {
            frameRateString = String.valueOf(frameRate) + " fps";
        } else {
            frameRateString = ExifUtil.NO_DATA;
        }
        return new InfoItem(context.getString(R.string.info_frame_rate), frameRateString);
    }

    public static Address retrieveAddress(Context context, double lat, double lng) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses.size() > 0) {
                return addresses.get(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*parsing Methods*/
    private static class Parser {
        private static final String[] byteUnits =
                new String[]{"Bytes", " KB", " MB", " GB"};

        private static String parseFileSize(Context context, long fileBytes) {
            //long file_bytes = fileLength / 1000 * 1000;
            float bytes = fileBytes;
            int i = 0;
            while (bytes > 1000) {
                bytes = bytes / 1000;
                i++;
            }
            i = i >= byteUnits.length ? 0 : i;
            String unit = byteUnits[i];
            //round to two decimal digits
            String size = String.format(Util.getLocale(context), "%.3f", bytes);
            return size + unit;
        }

        private static String parseExposureTime(String input) {
            if (input == null || input.equals("null")) {
                return ExifUtil.NO_DATA;
            }
            float f = Float.valueOf(input);
            try {
                int i = Math.round(1 / f);
                return String.valueOf(1 + "/" + i) + " sec";
            } catch (NumberFormatException e) {
                return input;
            }
        }

        private static String parseGPSLongOrLat(String input, boolean positive) {
            if (input == null || input.equals("null")) {
                return ExifUtil.NO_DATA;
            }

            float value = 0;
            String[] parts = input.split(",");
            for (int i = 0; i < parts.length; i++) {
                Rational r = Rational.parseRational(parts[i]);
                int factor = 1;
                for (int k = 0; k < i; k++) {
                    factor *= 60;
                }
                //noinspection ConstantConditions
                r.setDenominator(r.getDenominator() * factor);
                value += r.floatValue();
            }
            if (!positive) {
                value *= -1.0f;
            }
            return String.valueOf(value);
        }
    }
}
