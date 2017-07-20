package us.koller.cameraroll.data.Provider.ItemLoader;

import android.app.Activity;
import android.content.Context;

import java.io.File;
import java.util.ArrayList;

import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.File_POJO;

public abstract class ItemLoader {

    public class Result {
        public ArrayList<Album> albums;
        public File_POJO files;
    }

    public static ItemLoader getInstance(Class c) {
        if (c.equals(AlbumLoader.class)) {
            return new AlbumLoader();
        } else if (c.equals(FileLoader.class)) {
            return new FileLoader();
        }

        return null;
    }

    ItemLoader() {

    }

    public abstract void onNewDir(Context context, File dir);

    public abstract void onFile(Context context, File file);

    public abstract void onDirDone(Context context);

    public abstract Result getResult();
}
