package us.koller.cameraroll.data.models;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import us.koller.cameraroll.R;
import us.koller.cameraroll.util.Util;

public class Gif extends AlbumItem implements Parcelable {
    Gif() {
    }

    Gif(Parcel parcel) {
        super(parcel);
    }

    @Override
    public int[] retrieveImageDimens(Context c) {
        return Util.getImageDimensions(c, getUri(c));
    }

    @Override
    public String toString() {
        return "Gif: " + super.toString();
    }

    @Override
    public String getType(Context c) {
        return c.getString(R.string.gif);
    }
}
