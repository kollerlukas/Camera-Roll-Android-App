package us.koller.cameraroll.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;

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
        private static final int VIDEO = 2;

        private String name;
        private String path;
        public boolean error = false;
        public boolean isSharedElement = false;

        AlbumItem() {
            name = "";
            path = "";
        }

        public abstract boolean isPhoto();

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
            parcel.writeInt(this.isPhoto() ? PHOTO : VIDEO);
            parcel.writeString(name);
            parcel.writeString(path);
            parcel.writeString(String.valueOf(error));
        }

        public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
            @Override
            public AlbumItem createFromParcel(Parcel parcel) {
                if (parcel.readInt() == PHOTO) {
                    return new Photo(parcel);
                }
                return new Video(parcel);
            }

            public AlbumItem[] newArray(int i) {
                return new AlbumItem[i];
            }
        };
    }

    public static class Photo extends AlbumItem implements Parcelable {
        private Serializable imageViewSavedState;

        public boolean contentUri = false;

        Photo() {

        }

        Photo(Parcel parcel) {
            super(parcel);
            this.contentUri = Boolean.parseBoolean(parcel.readString());
        }

        @Override
        public boolean isPhoto() {
            return true;
        }

        public boolean isGif() {
            return getPath().endsWith(".gif");
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

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeString(String.valueOf(contentUri));
        }
    }


    static class Video extends AlbumItem implements Parcelable {

        Video() {

        }

        Video(Parcel parcel) {
            super(parcel);
        }

        @Override
        public boolean isPhoto() {
            return false;
        }


        @Override
        public String toString() {
            return "Video: " + super.toString();
        }
    }
}
