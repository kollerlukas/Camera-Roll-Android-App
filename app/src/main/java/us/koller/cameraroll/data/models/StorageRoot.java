package us.koller.cameraroll.data.models;

import android.os.Parcel;
import android.os.Parcelable;

public class StorageRoot extends File_POJO {

    private String name;

    public StorageRoot(String path) {
        super(path, false);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        if (name == null) {
            return super.getName();
        }
        return name;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeString(name);
    }

    private StorageRoot(Parcel parcel) {
        super(parcel);
        name = parcel.readString();
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public StorageRoot createFromParcel(Parcel parcel) {
            return new StorageRoot(parcel);
        }

        @Override
        public StorageRoot[] newArray(int i) {
            return new StorageRoot[i];
        }
    };
}
