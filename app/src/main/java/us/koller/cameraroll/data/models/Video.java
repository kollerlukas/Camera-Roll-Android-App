package us.koller.cameraroll.data.models;

import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.FileNotFoundException;
import java.io.IOException;

import us.koller.cameraroll.R;
import us.koller.cameraroll.util.Util;

public class Video extends AlbumItem implements Parcelable {
    Video() {
    }

    Video(Parcel p) {
        super(p);
    }

    @Override
    public int[] retrieveImageDimens(Context c) {
        try {
            return Util.getVideoDimensions(getPath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return new int[]{1, 1};
    }

    public int retrieveFrameRate() {
        MediaExtractor me = new MediaExtractor();
        int frameRate = -1;
        try {
            //Adjust data source as per the requirement if file, URI, etc.
            me.setDataSource(getPath());
            int numTracks = me.getTrackCount();
            for (int i = 0; i < numTracks; i++) {
                MediaFormat format = me.getTrackFormat(i);
                if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                    frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //Release stuff
            me.release();
        }
        return frameRate;
    }

    @Override
    public String toString() {
        return "Video: " + super.toString();
    }

    @Override
    public String getType(Context c) {
        return c.getString(R.string.video);
    }
}
