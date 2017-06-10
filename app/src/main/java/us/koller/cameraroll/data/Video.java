package us.koller.cameraroll.data;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.FileNotFoundException;

import us.koller.cameraroll.util.Util;

public class Video extends AlbumItem implements Parcelable {

    Video() {

    }

    Video(Parcel parcel) {
        super(parcel);
    }

    @Override
    int[] retrieveImageDimens(Context context) {
        try {
            return Util.getVideoDimensions(getPath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return new int[]{1, 1};
    }

    @Override
    public String toString() {
        return "Video: " + super.toString();
    }

    @Override
    public String getType() {
        return "Video";
    }
}
