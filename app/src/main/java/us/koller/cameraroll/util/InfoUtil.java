package us.koller.cameraroll.util;

import android.content.Context;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class InfoUtil {

    public static String retrieveFileName(Context context, Uri uri) {
        //retrieve file name
        Cursor cursor = context.getContentResolver().query(uri,
                null, null, null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            cursor.moveToFirst();
            String filename = cursor.getString(nameIndex);
            cursor.close();
            return filename;
        } else {
            return null;
        }
    }

    public static String retrieveFileSize(Context context, Uri uri) {
        //retrieve fileSize form MediaStore
        Cursor cursor = context.getContentResolver().query(
                uri, null, null,
                null, null);
        if (cursor != null) {
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            cursor.moveToFirst();
            String size = parseFileSize(cursor.getLong(sizeIndex));
            cursor.close();
            return size;
        } else {
            return null;
        }
    }

    public static Address retrieveAddredd(Context context, double lat, double lng) {
        if (Util.hasWifiConnection(context)) {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            try {
                List<Address> addresses = geocoder
                        .getFromLocation(lat, lng, 1);
                return addresses.get(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /*parsing Methods*/
    public static String parseFileSize(long fileLength) {
        long file_bytes = fileLength / 1000 * 1000;
        float size = file_bytes;
        int i = 0;
        while (size > 1000) {
            size = size / 1000;
            i++;
        }
        switch (i) {
            case 1:
                return size + " KB";
            case 2:
                return size + " MB";
            case 3:
                return size + " GB";
        }
        return file_bytes + " Bytes";
    }

    public static String parseExposureTime(String input) {
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

    public static String parseGPSLongOrLat(String input, boolean positive) {
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
            r.setDenominator(r.getDenominator() * factor);
            value += r.floatValue();
        }
        if (!positive) {
            value *= -1.0f;
        }
        return String.valueOf(value);
    }
}
