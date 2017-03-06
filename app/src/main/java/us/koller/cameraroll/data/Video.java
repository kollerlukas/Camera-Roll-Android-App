package us.koller.cameraroll.data;

import android.os.Parcel;
import android.os.Parcelable;

public class Video extends AlbumItem implements Parcelable {

    private int playedTime = 0;

    Video() {

    }

    public int getPlayedTime() {
        return playedTime;
    }

    public void setPlayedTime(int playedTime) {
        this.playedTime = playedTime;
    }

    Video(Parcel parcel) {
        super(parcel);
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
