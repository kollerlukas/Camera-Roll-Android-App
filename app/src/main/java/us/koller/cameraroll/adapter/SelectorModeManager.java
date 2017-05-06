package us.koller.cameraroll.adapter;

import android.content.Context;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Arrays;

import us.koller.cameraroll.data.AlbumItem;

//simple wrapper class to handle the Selector Mode and selected items
public class SelectorModeManager {

    public static final String SELECTOR_MODE_ACTIVE = "SELECTOR_MODE_ACTIVE";
    public static final String SELECTED_ITEMS_PATHS = "SELECTED_ITEMS_PATHS";

    private boolean selectorModeActive = false;
    private ArrayList<String> selected_items_paths;

    public SelectorModeManager(Bundle savedState) {
        if (savedState.containsKey(SELECTOR_MODE_ACTIVE)) {
            setSelectorMode(Boolean.parseBoolean(savedState.getString(SELECTOR_MODE_ACTIVE)));
        }

        if (isSelectorModeActive() && savedState.containsKey(SELECTED_ITEMS_PATHS)) {
            selected_items_paths = savedState.getStringArrayList(SELECTED_ITEMS_PATHS);
        } else {
            selected_items_paths = new ArrayList<>();
        }
    }

    public SelectorModeManager() {
        selected_items_paths = new ArrayList<>();
    }

    public boolean isItemSelected(String path) {
        return selected_items_paths.contains(path);
    }

    public void setSelectorMode(boolean selectorMode) {
        this.selectorModeActive = selectorMode;
    }

    public boolean isSelectorModeActive() {
        return selectorModeActive;
    }

    public boolean onItemSelect(String path) {
        boolean selected = addOrRemovePathFromList(selected_items_paths, path);
        return selected;
    }

    public int getSelectedItemCount() {
        return selected_items_paths.size();
    }

    public AlbumItem[] createAlbumItemArray(Context c) {
        return createAlbumItemArray(c, selected_items_paths);
    }

    public String[] createStringArray() {
        return createStringArray(selected_items_paths);
    }

    public void clearList() {
        this.selected_items_paths = new ArrayList<>();
    }

    public void saveInstanceState(Bundle outState) {
        boolean active = isSelectorModeActive();
        outState.putString(SELECTOR_MODE_ACTIVE, String.valueOf(active));
        if (active) {
            outState.putStringArrayList(SELECTED_ITEMS_PATHS, selected_items_paths);
        }
    }

    //Util methods
    private static boolean addOrRemovePathFromList(ArrayList<String> arr, String item) {
        //find out if item is in arr
        boolean itemIsInList = false;
        for (int i = 0; i < arr.size(); i++) {
            if (arr.get(i).equals(item)) {
                itemIsInList = true;
                break;
            }
        }

        if (itemIsInList) {
            //remove item
            arr.remove(item);
            return false;
        } else {
            //add item
            arr.add(item);
            return true;
        }
    }

    public static AlbumItem[] createAlbumItemArray(Context c, String[] arr) {
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.addAll(Arrays.asList(arr));
        return createAlbumItemArray(c, arrayList);
    }

    public static AlbumItem[] createAlbumItemArray(Context c, ArrayList<String> arr) {
        AlbumItem[] albumItems = new AlbumItem[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            albumItems[i] = AlbumItem.getInstance(c, arr.get(i));
        }
        return albumItems;
    }

    public static String[] createStringArray(ArrayList<String> arr) {
        String[] paths = new String[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            paths[i] = arr.get(i);
        }
        return paths;
    }
}
