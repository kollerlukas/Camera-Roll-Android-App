package us.koller.cameraroll.util;

import android.app.Activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.Settings;

public class SortUtil {

    public static final int BY_DATE = 1;
    public static final int BY_NAME = 2;
    public static final int BY_SIZE = 3;

    //interface, implemented by Album & AlbumItem, to sort them
    public interface Sortable {
        String getName();

        long getDate();

        String getPath();

        boolean pinned();
    }

    public static void sortAlbums(Activity context, ArrayList<Album> albums) {
        Settings settings = Settings.getInstance(context);

        int sortAlbumBy = settings.sortAlbumBy();
        for (int i = 0; i < albums.size(); i++) {
            sort(albums.get(i).getAlbumItems(), sortAlbumBy);
        }

        int sortAlbumsBy = settings.sortAlbumsBy();
        switch (sortAlbumsBy) {
            case BY_NAME:
            case BY_DATE:
                sort(albums, sortAlbumsBy);
                return;
            case BY_SIZE:
                // Sorting
                Collections.sort(albums, new Comparator<Album>() {
                    @Override
                    public int compare(Album a1, Album a2) {
                        if (a1 != null && a2 != null) {
                            if (a1.pinned() ^ a2.pinned()) {
                                return a2.pinned() ? 1 : -1;
                            }
                            Integer a1_size = a1.getAlbumItems().size();
                            Integer a2_size = a2.getAlbumItems().size();
                            return a2_size.compareTo(a1_size);
                        }
                        return 0;
                    }
                });
                break;
            default:
                break;
        }
    }

    public static void sort(ArrayList<? extends Sortable> sortables, int by) {
        switch (by) {
            case BY_NAME:
                sortByName(sortables);
                break;
            case BY_DATE:
                sortByDate(sortables);
                break;
            default:
                break;
        }
    }

    public static void sortByName(ArrayList<? extends Sortable> sortables) {
        // Sorting
        Collections.sort(sortables, new Comparator<Sortable>() {
            @Override
            public int compare(Sortable s1, Sortable s2) {
                return compareNames(s1, s2);
            }
        });
    }

    public static void sortByDate(ArrayList<? extends Sortable> sortables) {
        // Sorting
        Collections.sort(sortables, new Comparator<Sortable>() {
            @Override
            public int compare(Sortable s1, Sortable s2) {
                return compareDate(s1, s2);
            }
        });
    }

    private static int compareNames(Sortable s1, Sortable s2) {
        if (s1 != null && s2 != null) {
            if (s1.pinned() ^ s2.pinned()) {
                return s2.pinned() ? 1 : -1;
            }
            return s1.getName().compareTo(s2.getName());
        }
        return 0;
    }

    private static int compareDate(Sortable s1, Sortable s2) {
        if (s1 != null && s2 != null) {
            if (s1.pinned() ^ s2.pinned()) {
                return s2.pinned() ? 1 : -1;
            }
            Long l1 = s1.getDate();
            Long l2 = s2.getDate();
            if (l1.equals(l2)) {
                //if date is equal --> sort by Name
                return compareNames(s1, s2);
            }
            return l2.compareTo(l1);
        }
        return 0;
    }
}
