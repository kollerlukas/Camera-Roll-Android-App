package us.koller.cameraroll.data;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import us.koller.cameraroll.util.Util;

public class Gif extends AlbumItem implements Parcelable {
    Gif() {

    }

    Gif(Parcel parcel) {
        super(parcel);
    }

    @Override
    public int[] retrieveImageDimens(Context context) {
        return Util.getImageDimensions(context, getUri(context));
    }

    @Override
    public String toString() {
        return "Gif: " + super.toString();
    }

    @Override
    public String getType() {
        return "Gif";
    }
}
