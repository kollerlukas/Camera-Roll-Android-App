package us.koller.cameraroll.adapter;

import android.app.Activity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Arrays;

import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.util.SortUtil;

//simple wrapper class to handle the Selector Mode and selected items
public class SelectorModeManager {

    private static final String SELECTOR_MODE_ACTIVE = "SELECTOR_MODE_ACTIVE";
    private static final String SELECTED_ITEMS_PATHS = "SELECTED_ITEMS_PATHS";

    private boolean selectorModeActive = false;
    private ArrayList<String> selected_items_paths;

    private ArrayList<Callback> callbacks;

    public void onSelectorModeEnter() {
        if (callbacks != null) {
            for (int i = 0; i < callbacks.size(); i++) {
                callbacks.get(i).onSelectorModeEnter();
            }
        }
    }

    private void onSelectorModeExit() {
        if (callbacks != null) {
            for (int i = 0; i < callbacks.size(); i++) {
                callbacks.get(i).onSelectorModeExit();
            }
        }
    }

    public void onItemSelected(int selectedItemCount) {
        if (callbacks != null) {
            for (int i = 0; i < callbacks.size(); i++) {
                callbacks.get(i).onItemSelected(selectedItemCount);
            }
        }
    }

    //SelectorMode Callback
    public interface Callback {
        void onSelectorModeEnter();

        void onSelectorModeExit();

        void onItemSelected(int selectedItemCount);
    }

    public static class SimpleCallback implements Callback {
        @Override
        public void onSelectorModeEnter() {

        }

        @Override
        public void onSelectorModeExit() {

        }

        @Override
        public void onItemSelected(int selectedItemCount) {

        }
    }

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
        if (selectorMode) {
            onSelectorModeEnter();
        } else {
            onSelectorModeExit();
        }
    }

    public boolean isSelectorModeActive() {
        return selectorModeActive;
    }

    public boolean onItemSelect(String path) {
        boolean selected = addOrRemovePathFromList(selected_items_paths, path);
        onItemSelected(getSelectedItemCount());
        return selected;
    }

    public int getSelectedItemCount() {
        return selected_items_paths.size();
    }

    public String[] createStringArray(Activity context) {
        ArrayList<String> selected_items_paths = sortStringArray(context, this.selected_items_paths);
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

    public void addCallback(Callback callback) {
        if (callbacks == null) {
            callbacks = new ArrayList<>();
        }
        callbacks.add(callback);

        if (isSelectorModeActive()) {
            callback.onSelectorModeEnter();
            callback.onItemSelected(getSelectedItemCount());
        }
    }

    public ArrayList<Callback> getCallbacks() {
        return callbacks;
    }

    public boolean callbacksAttached() {
        return callbacks != null && callbacks.size() > 0;
    }


    //to handle backPressed in SelectorMode
    public interface OnBackPressedCallback {
        void cancelSelectorMode();
    }

    private OnBackPressedCallback onBackPressedCallback;

    public void setOnBackPressedCallback(OnBackPressedCallback onBackPressedCallback) {
        this.onBackPressedCallback = onBackPressedCallback;
    }

    public boolean onBackPressedCallbackAlreadySet() {
        return onBackPressedCallback != null;
    }

    public boolean onBackPressed() {
        if (onBackPressedCallback != null && isSelectorModeActive()) {
            onBackPressedCallback.cancelSelectorMode();
            return true;
        } else {
            return false;
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

    public static AlbumItem[] createAlbumItemArray(String[] arr) {
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.addAll(Arrays.asList(arr));
        return createAlbumItemArray(arrayList);
    }

    private static AlbumItem[] createAlbumItemArray(ArrayList<String> arr) {
        AlbumItem[] albumItems = new AlbumItem[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            albumItems[i] = AlbumItem.getInstance(arr.get(i));
        }
        return albumItems;
    }

    private static String[] createStringArray(ArrayList<String> arr) {
        String[] paths = new String[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            paths[i] = arr.get(i);
        }
        return paths;
    }

    private static ArrayList<String> sortStringArray(Activity context, ArrayList<String> paths) {
        ArrayList<AlbumItem> albumItems = new ArrayList<>();
        for (int i = 0; i < paths.size(); i++) {
            albumItems.add(AlbumItem.getInstance(paths.get(i)));
        }
        int sortBy = Settings.getInstance(context).sortAlbumBy();
        SortUtil.sort(albumItems, sortBy);

        paths = new ArrayList<>();
        for (int i = 0; i < albumItems.size(); i++) {
            paths.add(albumItems.get(i).getPath());
        }

        return paths;
    }
}
