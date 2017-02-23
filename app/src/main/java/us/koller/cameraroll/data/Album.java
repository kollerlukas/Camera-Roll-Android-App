package us.koller.cameraroll.data;

import android.app.Activity;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import us.koller.cameraroll.data.Provider.MediaProvider;
import us.koller.cameraroll.data.Provider.Provider;
import us.koller.cameraroll.util.SortUtil;

public class Album
        implements Parcelable, SortUtil.Sortable {

    private static final int NOT_HIDDEN = 1;
    private static final int HIDDEN = 2;

    private ArrayList<AlbumItem> albumItems;
    private String path;

    private int hidden = -1;
    public boolean excluded;

    public Album() {
        albumItems = new ArrayList<>();

        excluded = false;
    }

    public Album setPath(String path) {
        this.path = path;

        excluded = Provider.isDirExcluded(getPath(),
                Provider.getExcludedPaths());

        return this;
    }

    public boolean isHidden() {
        if (hidden != -1) {
            return hidden == HIDDEN;
        }

        if (getName().startsWith(".")) {
            hidden = HIDDEN;
            return true;
        } else {
            File dir = new File(getPath());
            File[] files = dir.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].getName().equals(MediaProvider.FILE_TYPE_NO_MEDIA)) {
                        hidden = HIDDEN;
                        return true;
                    }
                }
            }
        }
        hidden = NOT_HIDDEN;
        return false;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String getName() {
        String name = new File(getPath()).getName();
        return name != null ? name : "ERROR";
    }

    @Override
    public long getDate(Activity context) {
        long newestItem = -1;
        for (int i = 0; i < albumItems.size(); i++) {
            if (albumItems.get(i).getDate(context) > newestItem) {
                newestItem = albumItems.get(i).getDate(context);
            }
        }
        return newestItem;
    }

    public ArrayList<AlbumItem> getAlbumItems() {
        return albumItems;
    }

    private Album(Parcel parcel) {
        path = parcel.readString();
        hidden = parcel.readInt();
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
        parcel.writeString(path);
        parcel.writeInt(hidden);
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
}
