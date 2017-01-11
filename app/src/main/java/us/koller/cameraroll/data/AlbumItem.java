package us.koller.cameraroll.data;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

import us.koller.cameraroll.util.MediaType;

public abstract class AlbumItem implements Parcelable {
    private static final int PHOTO = 1;
    private static final int GIF = 2;
    private static final int VIDEO = 3;

    private String name;
    private String path;
    public boolean error = false;
    public boolean contentUri = false;
    public boolean isSharedElement = false;

    public static AlbumItem getInstance(Context context, String path) {
        if (path == null) {
            return null;
        }
        AlbumItem albumItem = null;
        if (MediaType.isImage(context, path)) {
            if (!MediaType.isGif(context, path)) {
                albumItem = new Photo();
            } else {
                albumItem = new Gif();
            }
        } else if (MediaType.isVideo(context, path)) {
            albumItem = new Video();
        }

        if (albumItem != null) {
            albumItem
                    .setPath(path)
                    .setName(new File(path).getName());

            if (path.startsWith("content")) {
                albumItem.contentUri = true;
            }
        }

        return albumItem;
    }

    AlbumItem() {
        name = "";
        path = "";
    }

    public AlbumItem setName(String name) {
        this.name = name;
        return this;
    }

    private AlbumItem setPath(String path) {
        this.path = path;
        return this;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    AlbumItem(Parcel parcel) {
        this.name = parcel.readString();
        this.path = parcel.readString();
        this.error = Boolean.parseBoolean(parcel.readString());
        this.contentUri = Boolean.parseBoolean(parcel.readString());
    }

    @Override
    public String toString() {
        return getName() + ", " + getPath();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        int k;
        if (this instanceof Photo) {
            k = PHOTO;
        } else if (this instanceof Gif) {
            k = GIF;
        } else {
            k = VIDEO;
        }
        parcel.writeInt(k);
        parcel.writeString(name);
        parcel.writeString(path);
        parcel.writeString(String.valueOf(error));
        parcel.writeString(String.valueOf(contentUri));
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public AlbumItem createFromParcel(Parcel parcel) {
            switch (parcel.readInt()) {
                case PHOTO:
                    return new Photo(parcel);
                case GIF:
                    return new Gif(parcel);
                default:
                    return new Video(parcel);
            }
        }

        public AlbumItem[] newArray(int i) {
            return new AlbumItem[i];
        }
    };
}
