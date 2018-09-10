package us.koller.cameraroll.data.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.util.ArrayList;

import us.koller.cameraroll.data.provider.Provider;
import us.koller.cameraroll.util.SortUtil;

//simple POJO class
public class File_POJO implements Parcelable, SortUtil.Sortable {
    private String path;
    private String name;
    private ArrayList<File_POJO> children;
    public boolean isMedia;
    public boolean excluded;
    @SuppressWarnings("WeakerAccess")
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public File_POJO createFromParcel(Parcel p) {
            return new File_POJO(p);
        }

        @Override
        public File_POJO[] newArray(int i) {
            return new File_POJO[i];
        }
    };

    public File_POJO(String path, boolean isMedia) {
        this.path = path;
        this.isMedia = isMedia;
        children = new ArrayList<>();
        excluded = Provider.isDirExcluded(getPath(), Provider.getExcludedPaths());
    }

    public File_POJO setName(String name) {
        this.name = name;
        return this;
    }

    @SuppressWarnings("unchecked")
    public File_POJO(Parcel p) {
        path = p.readString();
        name = p.readString();
        isMedia = Boolean.valueOf(p.readString());
        children = p.createTypedArrayList(CREATOR);
        excluded = Boolean.valueOf(p.readString());
    }

    @Override
    public long getDate() {
        //not needed
        return new File(getPath()).lastModified();
    }

    public static File_POJO[] generateArray(Parcelable[] ps) {
        File_POJO[] files = new File_POJO[ps.length];
        for (int i = 0; i < ps.length; i++) {
            files[i] = (File_POJO) ps[i];
        }
        return files;
    }

    public void addChild(File_POJO file) {
        children.add(file);
    }

    @Override
    public String getName() {
        if (name != null) {
            return name;
        }
        String[] s = getPath().split("/");
        return s[s.length - 1];
    }

    @Override
    public String toString() {
        return getPath();
    }

    public String getPath() {
        return path;
    }

    @Override
    public boolean pinned() {
        return false;
    }

    public ArrayList<File_POJO> getChildren() {
        return children;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel p, int i) {
        p.writeString(path);
        p.writeString(name);
        p.writeString(String.valueOf(isMedia));
        File_POJO[] children = new File_POJO[this.children.size()];
        for (int k = 0; k < children.length; k++) {
            children[k] = this.children.get(k);
        }
        p.writeTypedArray(children, 0);
        p.writeString(String.valueOf(excluded));
    }
}
