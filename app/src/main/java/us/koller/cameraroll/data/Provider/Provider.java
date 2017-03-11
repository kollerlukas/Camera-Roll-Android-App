package us.koller.cameraroll.data.Provider;

import android.app.Activity;
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
import us.koller.cameraroll.data.Provider.Retriever.StorageRetriever;
import us.koller.cameraroll.data.StorageRoot;

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

    Retriever retriever;

    Provider(Context context) {
        if (excludedPaths == null) {
            loadExcludedPaths(context);
        }
    }

    public void onDestroy() {
        if (retriever != null) {
            retriever.onDestroy();
        }
    }

    public static String getStorageRoot(Activity context, String path) {
        StorageRoot[] roots = StorageRetriever.loadRoots(context);
        for (int i = 0; i < roots.length; i++) {
            if (path.contains(roots[i].getPath())) {
                return roots[i].getPath();
            }
        }
        return Environment.getExternalStorageDirectory().getPath();
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

    public static ArrayList<String> addExcludedPath(Context context, String path) {
        if (excludedPaths == null) {
            excludedPaths = loadExcludedPaths(context);
        }

        if (!excludedPaths.contains(path)) {
            excludedPaths.add(path);
        }

        return excludedPaths;
    }

    public static ArrayList<String> removeExcludedPath(Context context, String path) {
        if (excludedPaths == null) {
            excludedPaths = loadExcludedPaths(context);
        }

        excludedPaths.remove(path);

        return excludedPaths;
    }

    public static ArrayList<String> loadExcludedPaths(Context context) {
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
    }
}
