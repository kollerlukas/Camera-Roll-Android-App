package us.koller.cameraroll.data;

import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.FileNotFoundException;
import java.io.IOException;

import us.koller.cameraroll.util.StorageUtil;
import us.koller.cameraroll.util.Util;

public class Video extends AlbumItem implements Parcelable {

    Video() {

    }

    Video(Parcel parcel) {
        super(parcel);
    }

    @Override
    public int[] retrieveImageDimens(Context context) {
        try {
            return Util.getVideoDimensions(getPath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return new int[]{1, 1};
    }

    public int retrieveFrameRate() {
        MediaExtractor extractor = new MediaExtractor();
        int frameRate = -1;
        try {
            //Adjust data source as per the requirement if file, URI, etc.
            extractor.setDataSource(getPath());
            int numTracks = extractor.getTrackCount();
            for (int i = 0; i < numTracks; ++i) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //Release stuff
            extractor.release();
        }
        return frameRate;
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
