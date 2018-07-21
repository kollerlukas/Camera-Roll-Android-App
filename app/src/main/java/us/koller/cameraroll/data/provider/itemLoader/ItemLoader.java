package us.koller.cameraroll.data.provider.itemLoader;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;

import us.koller.cameraroll.data.models.Album;
import us.koller.cameraroll.data.models.File_POJO;

public abstract class ItemLoader {
    public class Result {
        public ArrayList<Album> albums;
        public File_POJO files;
    }

    ItemLoader() {
    }

    @SuppressWarnings("unused")
    public abstract ItemLoader newInstance();

    public abstract void onNewDir(Context c, File dir);

    public abstract void onFile(Context c, File f);

    public abstract void onDirDone(Context c);
    public abstract Result getResult();
}