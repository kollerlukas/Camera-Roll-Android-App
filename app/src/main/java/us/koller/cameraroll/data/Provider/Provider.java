package us.koller.cameraroll.data.Provider;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import us.koller.cameraroll.data.Provider.Retriever.Retriever;

public abstract class Provider {

    public interface Callback {
        void timeout();

        void needPermission();
    }

    //prevent StorageRetriever from querying Android-Folder
    private static final String[] permanentlyExcludedPaths
            = {Environment.getExternalStorageDirectory().getPath() + "/Android"}; // "/storage/emulated/0/Android"

    // by default excluded folders:
    // not expecting relevant media in alarms, music or ringtone folder
    private static final String[] defaultExcludedPaths = {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS).getPath(),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath(),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES).getPath()};

    Retriever retriever;

    private Callback callback;

    Provider(Context context) {
        if (excludedPaths == null) {
            loadExcludedPaths(context);
        }

        if (pinnedPaths == null) {
            loadPinnedPaths(context);
        }
    }

    void setCallback(Callback callback) {
        this.callback = callback;
    }

    @SuppressWarnings("unchecked")
    public <T extends Callback> T getCallback() {
        if (callback != null) {
            return (T) callback;
        }
        return null;
    }

    public void onDestroy() {
        setCallback(null);

        if (retriever != null) {
            retriever.onDestroy();
        }
    }

    //handle pinned albums
    private static final String PINNED_PATHS_NAME = "pinned_paths.txt";

    private static ArrayList<String> pinnedPaths;

    public static ArrayList<String> getPinnedPaths() {
        return pinnedPaths;
    }

    public static boolean isAlbumPinned(String albumPath, ArrayList<String> pinnedPaths) {
        if (pinnedPaths == null) {
            return false;
        }
        return pinnedPaths.contains(albumPath);
    }

    public static void pinPath(Context context, String path) {
        if (pinnedPaths == null) {
            pinnedPaths = loadPinnedPaths(context);
        }

        if (!pinnedPaths.contains(path)) {
            pinnedPaths.add(path);
        }
    }

    public static void unpinPath(Context context, String path) {
        if (pinnedPaths == null) {
            pinnedPaths = loadPinnedPaths(context);
        }

        pinnedPaths.remove(path);
    }

    public static ArrayList<String> loadPinnedPaths(Context context) {
        pinnedPaths = new ArrayList<>();

        try {
            pinnedPaths = loadPathsArrayList(context, PINNED_PATHS_NAME);
        } catch (IOException e) {
            // no file found
        }

        return excludedPaths;
    }

    public static void savePinnedPaths(Context context) {
        try {
            savePathsArrayList(context, pinnedPaths, PINNED_PATHS_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //handle excluded paths
    private static final String EXCLUDED_PATHS_NAME = "excluded_paths.txt";

    private static ArrayList<String> excludedPaths;

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

    public static boolean searchDir(String path) {
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
        /*if (search && excludedPaths != null) {
            for (int i = 0; i < Provider.excludedPaths.size(); i++) {
                if (path.contains(Provider.excludedPaths.get(i))) {
                    search = false;
                    break;
                }
            }
        }*/

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

    public static ArrayList<String> getExcludedPaths() {
        return excludedPaths;
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

    public static void saveExcludedPaths(Context context) {
        try {
            savePathsArrayList(context, excludedPaths, EXCLUDED_PATHS_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*public static ArrayList<String> loadExcludedPaths(Context context) {
        excludedPaths = new ArrayList<>();

        //read file
        try {
            FileInputStream fis = context.openFileInput(EXCLUDED_PATHS_NAME);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = reader.readLine()) != null) {
                excludedPaths.add(line);
            }
            fis.close();
        } catch (IOException e) {
            // no file found
            excludedPaths.addAll(Arrays.asList(defaultExcludedPaths));
        }

        return excludedPaths;
    }

    public static void saveExcludedPaths(Context context) {
        if (excludedPaths == null) {
            return;
        }

        //write to file
        try {
            FileOutputStream fos
                    = context.openFileOutput(EXCLUDED_PATHS_NAME, Context.MODE_PRIVATE);
            for (int i = 0; i < excludedPaths.size(); i++) {
                fos.write((excludedPaths.get(i) + '\n').getBytes());
            }
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    private static ArrayList<String> loadPathsArrayList(Context context, String filename) throws IOException {
        ArrayList<String> paths = new ArrayList<>();

        //read file
        ArrayList<String> lines = readFromFile(context, filename);
        paths.addAll(lines);

        return paths;
    }

    private static void savePathsArrayList(Context context, ArrayList<String> paths, String filename) throws IOException {
        if (paths == null) {
            return;
        }

        //write to file
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paths.size(); i++) {
            sb.append(paths.get(i)).append('\n');
        }

        writeToFile(context, filename, sb.toString());
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

    private static void writeToFile(Context context, String filename, String data) throws IOException {
        //write to file
        FileOutputStream fos
                = context.openFileOutput(filename, Context.MODE_PRIVATE);
        fos.write(data.getBytes());
        fos.close();
    }
}
