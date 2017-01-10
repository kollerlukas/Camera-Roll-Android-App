package us.koller.cameraroll.data;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

import us.koller.cameraroll.util.MediaType;

public class Album implements Parcelable {

    private ArrayList<AlbumItem> albumItems;

    private String name;

    public boolean hiddenAlbum = false;

    public Album() {
        albumItems = new ArrayList<>();
    }

    public Album setName(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

    public ArrayList<AlbumItem> getAlbumItems() {
        return albumItems;
    }

    private Album(Parcel parcel) {
        name = parcel.readString();
        hiddenAlbum = Boolean.parseBoolean(parcel.readString());
        albumItems = new ArrayList<>();
        albumItems = parcel.createTypedArrayList(AlbumItem.CREATOR);
    }

    @Override
    public String toString() {
        return getName() + ": " + getAlbumItems().toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeString(String.valueOf(hiddenAlbum));
        AlbumItem[] albumItems = new AlbumItem[this.albumItems.size()];
        for (int k = 0; k < albumItems.length; k++) {
            albumItems[k] = this.albumItems.get(k);
        }
        parcel.writeTypedArray(albumItems, 0);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Album createFromParcel(Parcel parcel) {
            return new Album(parcel);
        }

        @Override
        public Album[] newArray(int i) {
            return new Album[i];
        }
    };

    public static abstract class AlbumItem implements Parcelable {
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

        AlbumItem setPath(String path) {
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
                /*if (parcel.readInt() == PHOTO) {
                    return new Photo(parcel);
                }
                return new Video(parcel);*/
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

    public static class Photo extends AlbumItem implements Parcelable {
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
        public String toString() {
            return "Photo: " + super.toString();
        }
    }

    public static class Gif extends AlbumItem implements Parcelable {
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

    public static class Video extends AlbumItem implements Parcelable {

        Video() {

        }

        Video(Parcel parcel) {
            super(parcel);
        }

        @Override
        public String toString() {
            return "Video: " + super.toString();
        }
    }
}
