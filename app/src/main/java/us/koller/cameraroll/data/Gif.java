package us.koller.cameraroll.data;

import android.os.Parcel;
import android.os.Parcelable;

public class Gif extends AlbumItem implements Parcelable {
    Gif() {

    }

    Gif(Parcel parcel) {
        super(parcel);
    }

    @Override
    public String toString() {
        return "Gif: " + super.toString();
    }
}
