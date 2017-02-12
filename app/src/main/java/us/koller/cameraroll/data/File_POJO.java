package us.koller.cameraroll.data;

import android.app.Activity;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

import us.koller.cameraroll.util.SortUtil;

//simple POJO class
public class File_POJO
        implements Parcelable, SortUtil.Sortable {

    private String path;
    private ArrayList<File_POJO> children;
    public boolean isMedia;

    public File_POJO(String path, boolean isMedia) {
        this.path = path;
        this.isMedia = isMedia;

        children = new ArrayList<>();
    }

    public void addChild(File_POJO file) {
        children.add(file);
    }

    @Override
    public String getName() {
        String[] s = getPath().split("/");
        return s[s.length - 1];
    }

    @Override
    public long getDate(Activity context) {
        //not needed
        return 0;
    }

    public String getPath() {
        return path;
    }

    public ArrayList<File_POJO> getChildren() {
        return children;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(path);
        parcel.writeString(String.valueOf(isMedia));
        File_POJO[] children = new File_POJO[this.children.size()];
        for (int k = 0; k < children.length; k++) {
            children[k] = this.children.get(k);
        }
        parcel.writeTypedArray(children, 0);
    }

    private File_POJO(Parcel parcel) {
        path = parcel.readString();
        isMedia = Boolean.valueOf(parcel.readString());
        children = parcel.createTypedArrayList(CREATOR);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public File_POJO createFromParcel(Parcel parcel) {
            return new File_POJO(parcel);
        }

        @Override
        public File_POJO[] newArray(int i) {
            return new File_POJO[i];
        }
    };
}
