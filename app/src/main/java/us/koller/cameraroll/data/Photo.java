package us.koller.cameraroll.data;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

import us.koller.cameraroll.util.Util;

public class Photo extends AlbumItem implements Parcelable {
    private Serializable imageViewSavedState;

    Photo() {

    }

    Photo(Parcel parcel) {
        super(parcel);
    }

    public void putImageViewSavedState(Serializable imageViewSavedState) {
        this.imageViewSavedState = imageViewSavedState;
    }

    public Serializable getImageViewSavedState() {
        return imageViewSavedState;
    }

    @Override
    public int[] retrieveImageDimens(Context context) {
        return Util.getImageDimensions(context, getUri(context));
    }

    @Override
    public String toString() {
        return "Photo: " + super.toString();
    }

    @Override
    public String getType() {
        return "Photo";
    }
}
