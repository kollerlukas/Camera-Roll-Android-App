package us.koller.cameraroll.data.provider;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import us.koller.cameraroll.data.models.VirtualAlbum;
import us.koller.cameraroll.data.provider.retriever.Retriever;
import us.koller.cameraroll.util.StorageUtil;

public abstract class Provider {

    //prevent StorageRetriever from querying Android-Folder
    private static final String[] permanentlyExcludedPaths
            = {Environment.getExternalStorageDirectory().getPath() + "/Android"}; // "/storage/emulated/0/Android"

    // by default excluded folders:
    // not expecting relevant media in alarms, music or ringtone folder
    private static final String[] defaultExcludedPaths = {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS).getPath(),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath(),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES).getPath()};

    // by default pinned folders:
    @SuppressWarnings("MismatchedReadAndWriteOfArray")
    private static final String[] defaultPinnedPaths = {};

    // default virtual directories:
    // not expecting relevant media in alarms, music or ringtone folder
    @SuppressWarnings("MismatchedReadAndWriteOfArray")
    private static final VirtualAlbum[] defaultVirtualAlbums = {};

    Retriever retriever;

    private Callback callback;

    //handle pinned albums
    private static final String PINNED_PATHS_NAME = "pinned_paths.txt";
    private static ArrayList<String> pinnedPaths;

    //handle excluded paths
    private static final String EXCLUDED_PATHS_NAME = "excluded_paths.txt";
    private static ArrayList<String> excludedPaths;

    //virtual directories
    private static final String VIRTUAL_DIRECTORIES_NAME = "virtual_directories.txt";
    private static ArrayList<VirtualAlbum> virtualAlbums;

    public interface Callback {
        void timeout();
        void needPermission();
    }

    Provider(Context c) {
        if (excludedPaths == null) {
            loadExcludedPaths(c);
        }
        if (pinnedPaths == null) {
            loadPinnedPaths(c);
        }
    }

    public static ArrayList<VirtualAlbum> getVirtualAlbums(Context c) {
        if (virtualAlbums == null) {
            virtualAlbums = loadVirtualAlbums(c);
        }
        return virtualAlbums;
    }

    private static ArrayList<VirtualAlbum> loadVirtualAlbums(Context c) {
        virtualAlbums = new ArrayList<>();
        try {
            ArrayList<String> temp = loadPathsArrayList(c, VIRTUAL_DIRECTORIES_NAME);
            for (int i = 0; i < temp.size(); i++) {
                virtualAlbums.add(new VirtualAlbum(temp.get(i)));
            }
        } catch (IOException e) {
            // no file found
            virtualAlbums.addAll(Arrays.asList(defaultVirtualAlbums));
        }
        return virtualAlbums;
    }

    public static void addVirtualAlbum(Context c, VirtualAlbum vA) {
        if (virtualAlbums == null) {
            virtualAlbums = loadVirtualAlbums(c);
        }
        virtualAlbums.add(vA);
    }

    public static void removeVirtualAlbum(Context c, VirtualAlbum vA) {
        if (virtualAlbums == null) {
            virtualAlbums = loadVirtualAlbums(c);
        }
        virtualAlbums.remove(vA);
    }

    public static void saveVirtualAlbums(Context c) {
        try {
            ArrayList<String> temp = new ArrayList<>();
            for (int i = 0; i < virtualAlbums.size(); i++) {
                temp.add(virtualAlbums.get(i).toString());
            }
            savePathsArrayList(c, temp, VIRTUAL_DIRECTORIES_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<String> getPinnedPaths() {
        return pinnedPaths;
    }

    public static boolean isAlbumPinned(String albumPath, ArrayList<String> pinnedPaths) {
        if (pinnedPaths == null) {
            return false;
        }
        if (pinnedPaths.contains(albumPath)) {
            return true;
        } else {
            for (int i = 0; i < pinnedPaths.size(); i++) {
                if (albumPath.equals(pinnedPaths.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void pinPath(Context c, String path) {
        if (pinnedPaths == null) {
            pinnedPaths = loadPinnedPaths(c);
        }
        if (!pinnedPaths.contains(path)) {
            pinnedPaths.add(path);
        }
    }

    public static void unpinPath(Context c, String path) {
        if (pinnedPaths == null) {
            pinnedPaths = loadPinnedPaths(c);
        }
        pinnedPaths.remove(path);
    }

    public static ArrayList<String> loadPinnedPaths(Context c) {
        pinnedPaths = new ArrayList<>();
        try {
            pinnedPaths = loadPathsArrayList(c, PINNED_PATHS_NAME);
        } catch (IOException e) {
            // no file found
            pinnedPaths.addAll(Arrays.asList(defaultPinnedPaths));
        }
        return excludedPaths;
    }

    public static void savePinnedPaths(Context c) {
        try {
            savePathsArrayList(c, pinnedPaths, PINNED_PATHS_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean searchDir(String path) {
        if (path == null) {
            return false;
        }
        boolean search = true;
        for (int i = 0; i < Provider.permanentlyExcludedPaths.length; i++) {
            if (path.contains(Provider.permanentlyExcludedPaths[i])) {
                search = false;
                break;
            }
        }

        //Provider are now taking care of excluded folders themselves
        if (search && excludedPaths != null) {
            for (int i = 0; i < Provider.excludedPaths.size(); i++) {
                if (path.contains(Provider.excludedPaths.get(i))) {
                    search = false;
                    break;
                }
            }
        }
        return search;
    }

    public static boolean isDirExcluded(String path, ArrayList<String> excludedPaths) {
        if (isPathPermanentlyExcluded(path)) {
            return true;
        }
        if (excludedPaths == null) {
            return false;
        }
        boolean excluded = false;
        for (int i = 0; i < excludedPaths.size(); i++) {
            if (path.contains(excludedPaths.get(i))) {
                excluded = true;
                break;
            }
        }
        return excluded;
    }

    public static ArrayList<String> getExcludedPaths() {
        return excludedPaths;
    }

    public static boolean isPathPermanentlyExcluded(String path) {
        boolean permanentlyExcluded = false;
        for (int i = 0; i < Provider.permanentlyExcludedPaths.length; i++) {
            if (path.contains(Provider.permanentlyExcludedPaths[i])) {
                permanentlyExcluded = true;
                break;
            }
        }
        return permanentlyExcluded;
    }

    public static void addExcludedPath(Context context, String path) {
        if (excludedPaths == null) {
            excludedPaths = loadExcludedPaths(context);
        }
        if (!excludedPaths.contains(path)) {
            excludedPaths.add(path);
        }
    }

    public static void removeExcludedPath(Context context, String path) {
        if (excludedPaths == null) {
            excludedPaths = loadExcludedPaths(context);
        }
        excludedPaths.remove(path);
    }

    public static boolean isDirExcludedBecauseParentDirIsExcluded
            (String path, ArrayList<String> excludedPaths) {
        if (!isDirExcluded(path, excludedPaths)) {
            return false;
        }

        boolean excludedBecauseParent = true;
        for (int i = 0; i < excludedPaths.size(); i++) {
            if (path.equals(excludedPaths.get(i))) {
                excludedBecauseParent = false;
                break;
            }
        }
        return excludedBecauseParent;
    }

    public static void saveExcludedPaths(Context c) {
        try {
            savePathsArrayList(c, excludedPaths, EXCLUDED_PATHS_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ArrayList<String> loadPathsArrayList(Context c, String filename) throws IOException {
        ArrayList<String> paths = new ArrayList<>();

        //read file
        ArrayList<String> lines = readFromFile(c, filename);
        paths.addAll(lines);
        return paths;
    }

    private static void savePathsArrayList(Context c, ArrayList<String> paths, String filename) throws IOException {
        if (paths == null) {
            return;
        }

        //write to file
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paths.size(); i++) {
            sb.append(paths.get(i)).append('\n');
        }

        writeToFile(c, filename, sb.toString());
    }

    public static ArrayList<String> loadExcludedPaths(Context context) {
        excludedPaths = new ArrayList<>();
        try {
            excludedPaths = loadPathsArrayList(context, EXCLUDED_PATHS_NAME);
        } catch (IOException e) {
            // no file found
            excludedPaths.addAll(Arrays.asList(defaultExcludedPaths));
        }
        return excludedPaths;
    }

    private static ArrayList<String> readFromFile(Context context, String filename) throws IOException {
        ArrayList<String> lines = new ArrayList<>();

        //read file
        FileInputStream fis = context.openFileInput(filename);
        BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        fis.close();
        return lines;
    }

    private static void writeToFile(Context c, String filename, String data) throws IOException {
        //write to file
        FileOutputStream fos = c.openFileOutput(filename, Context.MODE_PRIVATE);
        fos.write(data.getBytes());
        fos.close();
    }

    public static File[] getDirectoriesToSearch(Context c) {
        FileFilter filter = (File f) -> f != null && Provider.searchDir(f.getPath());

        //external Directory
        File dir = Environment.getExternalStorageDirectory();
        File[] dirs = dir.listFiles(filter);
        if (dirs == null) {
            dirs = new File[]{};
        }

        //handle removable storage (e.g. SDCards)
        ArrayList<File> temp = new ArrayList<>();
        temp.addAll(Arrays.asList(dirs));
        File[] removableStorageRoots = StorageUtil.getRemovableStorageRoots(c);
        for (int i = 0; i < removableStorageRoots.length; i++) {
            File root = removableStorageRoots[i];
            File[] files = root.listFiles(filter);
            if (files != null) {
                Collections.addAll(temp, files);
            }
        }

        dirs = new File[temp.size()];
        for (int i = 0; i < dirs.length; i++) {
            dirs[i] = temp.get(i);
        }
        return dirs;
    }

    @SuppressWarnings("unchecked")
    public <T extends Callback> T getCallback() {
        if (callback != null) {
            return (T) callback;
        }
        return null;
    }

    void setCallback(Callback ca) {
        this.callback = ca;
    }

    public void onDestroy() {
        setCallback(null);
        if (retriever != null) {
            retriever.onDestroy();
        }
    }
}
