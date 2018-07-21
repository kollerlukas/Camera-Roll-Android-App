package us.koller.cameraroll.data.models;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.util.InfoUtil;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.SortUtil;
import us.koller.cameraroll.util.StorageUtil;
import us.koller.cameraroll.util.Util;

public abstract class AlbumItem implements Parcelable, SortUtil.Sortable {

    private static final int PHOTO = 1;
    private static final int GIF = 2;
    private static final int VIDEO = 3;
    private static final int RAW = 4;

    private String name;
    private String path;
    private Uri uri;
    private long dateTaken;
    private int[] imageDimens;
    private List<String> tags;

    public boolean error = false;
    public boolean isSharedElement = false;
    public boolean hasFadedIn = false;

    @SuppressWarnings("WeakerAccess")
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public AlbumItem createFromParcel(Parcel p) {
            switch (p.readInt()) {
                case VIDEO:
                    return new Video(p);
                case GIF:
                    return new Gif(p);
                case RAW:
                    return new RAWImage(p);
                default:
                    return new Photo(p);
            }
        }

        public AlbumItem[] newArray(int i) {
            return new AlbumItem[i];
        }
    };

    AlbumItem(Parcel p) {
        this.name = p.readString();
        this.path = p.readString();
        this.error = Boolean.parseBoolean(p.readString());
        this.uri = Uri.parse(p.readString());
    }

    //factory method
    public static AlbumItem getInstance(String path) {
        AlbumItem aI = null;
        if (MediaType.isGif(path)) {
            aI = new Gif();
        } else if (MediaType.isRAWImage(path)) {
            aI = new RAWImage();
        } else if (MediaType.isImage(path)) {
            aI = new Photo();
        } else if (MediaType.isVideo(path)) {
            aI = new Video();
        }

        if (aI != null) {
            aI.setPath(path).setName(new File(path).getName());
        }
        return aI;
    }

    public static AlbumItem getInstance(Context c, String path) {
        if (MediaType.isVideo(path) && !Settings.getInstance(c).showVideos()) {
            return null;
        }
        return getInstance(path);
    }

    public AlbumItem() {
        name = "";
        path = "";
        dateTaken = -1;
    }

    public AlbumItem setName(String name) {
        this.name = name;
        return this;
    }

    private AlbumItem setPath(String path) {
        this.path = path;
        return this;
    }

    public static AlbumItem getInstance(final Context c, Uri uri) {
        if (uri == null) {
            return null;
        }

        String mimeType = MediaType.getMimeType(c, uri);
        return getInstance(c, uri, mimeType);
    }

    public static AlbumItem getInstance(final Context c, Uri uri, String mimeType) {
        if (uri == null) {
            return null;
        }

        AlbumItem aI = null;
        if (MediaType.checkGifMimeType(mimeType)) {
            aI = new Gif();
        } else if (MediaType.checkRAWMimeType(mimeType)) {
            aI = new RAWImage();
        } else if (MediaType.checkImageMimeType(mimeType)) {
            aI = new Photo();
        } else if (MediaType.checkVideoMimeType(mimeType)) {
            aI = new Video();
        }

        if (aI != null) {
            aI.setPath("N/A");
            aI.setUri(uri);

            //retrieve file name
            String filename = InfoUtil.retrieveFileName(c, uri);
            aI.setName(filename != null ? filename : "");
        }
        return aI;
    }

    public static AlbumItem getErrorItem() {
        AlbumItem aI = new Photo();
        aI.setPath("ERROR").setName("ERROR");
        return aI;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    @Override
    public long getDate() {
        if (dateTaken != -1) {
            return dateTaken;
        }
        return new File(getPath()).lastModified();
    }

    public void setDate(long dateTaken) {
        this.dateTaken = dateTaken;
    }

    @Override
    public boolean pinned() {
        return false;
    }

    @SuppressWarnings("unused")
    public void preloadUri(final Context context) {
        AsyncTask.execute(() -> getUri(context));
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public Uri getUri(Context c) {
        if (uri == null) {
            setUri(StorageUtil.getContentUri(c, this));
        }
        return uri;
    }

    public int[] getImageDimens(Context c) {
        if (imageDimens == null) {
            imageDimens = retrieveImageDimens(c);
        }
        return new int[]{this.imageDimens[0], this.imageDimens[1]};
    }

    public abstract int[] retrieveImageDimens(Context c);

    public List<String> getTags(Context c) {
        if (tags == null) {
            retrieveTags(c);
        }
        return tags;
    }

    private void retrieveTags(Context c) {
        // TODO: implement
        tags = new LinkedList<>();
    }

    // returns whether the Operation was successful
    public boolean addTag(Context c, String tag) {
        // TODO: implement
        return false;
    }

    // returns whether the Operation was successful
    public boolean removeTag(Context c, String tag) {
        // TODO: implement
        return false;
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
    public void writeToParcel(Parcel p, int i) {
        int k;
        if (this instanceof RAWImage) {
            k = RAW;
        } else if (this instanceof Gif) {
            k = GIF;
        } else if (this instanceof Video) {
            k = VIDEO;
        } else {
            k = PHOTO;
        }
        p.writeInt(k);
        p.writeString(name);
        p.writeString(path);
        p.writeString(String.valueOf(error));
        p.writeString(String.valueOf(uri));
    }

    public abstract String getType(Context c);

    public Key getGlideSignature() {
        File f = new File(getPath());
        String lastModified = String.valueOf(f.lastModified());
        return new ObjectKey(lastModified);
    }

    public RequestOptions getGlideRequestOptions(Context c) {
        return new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .error(Util.getErrorPlaceholder(c))
                .signature(getGlideSignature());
    }
}
