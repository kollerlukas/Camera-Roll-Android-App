package us.koller.cameraroll.data.models;

import android.content.Context;
import android.os.Parcel;

import us.koller.cameraroll.R;
import us.koller.cameraroll.util.Util;

public class RAWImage extends Photo {
    RAWImage() {
    }

    RAWImage(Parcel p) {
        super(p);
    }

    @Override
    public int[] retrieveImageDimens(Context c) {
        return Util.getImageDimensions(c, getUri(c));
    }

    @Override
    public String getType(Context c) {
        return c.getString(R.string.raw_image);
    }
}
