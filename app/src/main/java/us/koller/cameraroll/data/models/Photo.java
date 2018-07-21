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

    Photo(Parcel p) {
        super(p);
    }

    public void putImageViewSavedState(Serializable imageViewSavedState) {
        this.imageViewSavedState = imageViewSavedState;
    }

    public Serializable getImageViewSavedState() {
        return imageViewSavedState;
    }

    @Override
    public int[] retrieveImageDimens(Context c) {
        return Util.getImageDimensions(c, getUri(c));
    }

    @Override
    public String getType(Context c) {
        return c.getString(R.string.photo);
    }
}
