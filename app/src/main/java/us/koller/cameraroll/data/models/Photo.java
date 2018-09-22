package us.koller.cameraroll.data.models;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

import us.koller.cameraroll.R;
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
    public String getType(Context context) {
        return context.getString(R.string.photo);
    }
}
