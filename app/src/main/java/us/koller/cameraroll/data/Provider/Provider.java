package us.koller.cameraroll.data.Provider;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import us.koller.cameraroll.data.Provider.Retriever.Retriever;

public abstract class Provider {

    //prevent StorageRetriever from querying Android-Folder
    private static final String[] pathsNotToSearch = {"/storage/emulated/0/Android"};

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

    //handle excluded paths
    private static final String EXCLUDED_PATHS_NAME = "excluded_paths.txt";

    private static ArrayList<String> excludedPaths;

    public static boolean searchDir(String path) {
        boolean search = true;
        for (int i = 0; i < Provider.pathsNotToSearch.length; i++) {
            if (path.contains(Provider.pathsNotToSearch[i])) {
                search = false;
                break;
            }
        }
        return search;
    }

    public static boolean isDirExcluded(String path, ArrayList<String> excludedPaths) {
        if (!searchDir(path)) {
            return false;
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

    public static boolean isDirExcludedBecauseParentDirIsExcluded(String path, ArrayList<String> excludedPaths) {
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
            e.printStackTrace();
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
                fos.write(excludedPaths.get(i).getBytes());
            }
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
