package us.koller.cameraroll.util;

import android.app.Activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.File_POJO;

public class SortUtil {

    //interface, implemented by Album & AlbumItem, to sort them
    public interface Sortable {
        String getName();

        long getDate(Activity context);

        String getPath();
    }

    public static final int BY_DATE = 1;
    public static final int BY_NAME = 2;

    public static ArrayList<? extends Sortable> sortAlbums(Activity context, ArrayList<? extends Sortable> albums, int by) {
        //sort each individual album
        if (albums.size() > 0 && albums.get(0) instanceof Album) {
            for (int i = 0; i < albums.size(); i++) {
                sort(context, ((Album) albums.get(i)).getAlbumItems(), BY_DATE);
            }
        }

        //sort albums arrayList
        sort(context, albums, by);

        return albums;
    }

    public static File_POJO sortFiles(Activity context, File_POJO files) {
        sort(context, files.getChildren(), BY_NAME);

        return files;
    }

    private static ArrayList<? extends Sortable> sort(Activity context, ArrayList<? extends Sortable> sortables, int by) {
        switch (by) {
            case BY_DATE:
                return sortByDate(context, sortables);
            case BY_NAME:
                return sortByName(sortables);
        }
        return sortables;
    }

    public static ArrayList<? extends Sortable> sortByDate(final Activity context, ArrayList<? extends Sortable> sortables) {
        // Sorting
        Collections.sort(sortables, new Comparator<Sortable>() {
            @Override
            public int compare(Sortable s1, Sortable s2) {
                if (s1 != null && s2 != null) {
                    Long l1 = s1.getDate(context);
                    Long l2 = s2.getDate(context);
                    return l2.compareTo(l1);
                }
                return 0;
            }
        });
        return sortables;
    }

    public static ArrayList<? extends Sortable> sortByName(ArrayList<? extends Sortable> sortables) {
        // Sorting
        Collections.sort(sortables, new Comparator<Sortable>() {
            @Override
            public int compare(Sortable s1, Sortable s2) {
                if (s1 != null && s2 != null) {
                    return s1.getName().compareTo(s2.getName());
                }
                return 0;
            }
        });
        return sortables;
    }
}
