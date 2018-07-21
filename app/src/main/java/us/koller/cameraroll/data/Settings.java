package us.koller.cameraroll.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import us.koller.cameraroll.R;
import us.koller.cameraroll.styles.Cards;
import us.koller.cameraroll.styles.Cards2;
import us.koller.cameraroll.styles.List;
import us.koller.cameraroll.styles.NestedRecyclerView;
import us.koller.cameraroll.styles.Parallax;
import us.koller.cameraroll.styles.Style;
import us.koller.cameraroll.themes.BlackTheme;
import us.koller.cameraroll.themes.DarkTheme;
import us.koller.cameraroll.themes.LightTheme;
import us.koller.cameraroll.themes.Theme;
import us.koller.cameraroll.util.SortUtil;

public class Settings {

    public static final int DEFAULT_COLUMN_COUNT = 4;
    private static final String PREF_KEY_HIDDEN_FOLDERS = "HIDDEN_FOLDERS";

    private String theme;
    private boolean storageRetriever;
    private int style;
    private int columnCount;
    private int sort_albums_by;
    private int sort_album_by;
    private boolean hiddenFolders;
    private boolean showVideos;
    private boolean use8BitColor;
    private boolean cameraShortcut;
    private Uri removableStorageTreeUri;
    private boolean virtualDirectories;
    @SuppressWarnings("FieldCanBeLocal")
    private boolean fadeImages = false;
    private boolean showAnimations;
    private boolean maxBrightness;
    private float prevBrightness;

    private static Settings instance;

    private Settings(Context c) {
        SharedPreferences sP = PreferenceManager.getDefaultSharedPreferences(c);

        theme = sP.getString(c.getString(R.string.pref_key_theme), c.getString(R.string.DARK_THEME_VALUE));

        storageRetriever = sP.getBoolean(c.getString(R.string.pref_key_media_retriever), false);

        style = sP.getInt(c.getString(R.string.pref_key_style),
                c.getResources().getInteger(R.integer.STYLE_PARALLAX_VALUE));

        columnCount = sP.getInt(c.getString(R.string.pref_key_column_count), DEFAULT_COLUMN_COUNT);

        sort_albums_by = sP.getInt(c.getString(R.string.pref_key_sort_albums), SortUtil.BY_NAME);

        sort_album_by = sP.getInt(c.getString(R.string.pref_key_sort_album), SortUtil.BY_DATE);

        showVideos = sP.getBoolean(c.getString(R.string.pref_key_show_videos), true);

        hiddenFolders = sP.getBoolean(PREF_KEY_HIDDEN_FOLDERS, false);

        use8BitColor = sP.getBoolean(c.getString(R.string.pref_key_8_bit_color), false);

        cameraShortcut = sP.getBoolean(c.getString(R.string.pref_key_camera_shortcut), false);

        removableStorageTreeUri = Uri.parse(sP.getString(
                c.getString(R.string.pref_key_removable_storage_treeUri), ""));

        virtualDirectories = sP.getBoolean(c.getString(R.string.pref_key_virtual_directories), true);

        showAnimations = sP.getBoolean(c.getString(R.string.pref_key_animations), true);

        maxBrightness = sP.getBoolean(c.getString(R.string.pref_key_max_brightness), false);
    }

    public static Settings getInstance(Context c) {
        if (instance == null) {
            instance = new Settings(c);
        }
        return instance;
    }

    public static void saveInt(Context c, String key, int value) {
        SharedPreferences sP = PreferenceManager.getDefaultSharedPreferences(c);
        sP
                .edit()
                .putInt(key, value)
                .apply();
    }

    private static void saveBoolean(Context c, String key, boolean value) {
        SharedPreferences sP = PreferenceManager.getDefaultSharedPreferences(c);
        sP
                .edit()
                .putBoolean(key, value)
                .apply();
    }

    private static void saveString(Context c, String key, String value) {
        SharedPreferences sP = PreferenceManager.getDefaultSharedPreferences(c);
        sP
                .edit()
                .putString(key, value)
                .apply();
    }

    /*Getter & Setter*/
    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public Theme getThemeInstance(Context c) {
        String theme = getTheme();
        Resources res = c.getResources();
        if (theme.equals(res.getString(R.string.LIGHT_THEME_VALUE))) {
            return new LightTheme();
        } else if (theme.equals(res.getString(R.string.BLACK_THEME_VALUE))) {
            return new BlackTheme();
        } else {
            return new DarkTheme();
        }
    }

    public boolean useStorageRetriever() {
        return storageRetriever;
    }

    public void useStorageRetriever(boolean storageRetriever) {
        this.storageRetriever = storageRetriever;
    }

    public int getStyle(Context context, boolean pickPhotos) {
        if (pickPhotos && style == NestedRecyclerView.getValue(context)) {
            return Cards2.getValue(context);
        }
        return style;
    }

    public void setStyle(int style) {
        this.style = style;
    }

    public Style getStyleInstance(Context c, boolean pickPhotos) {
        int style = getStyle(c, pickPhotos);
        return getStyleInstance(c, style);
    }

    public Style getStyleInstance(Context c, int style) {
        if (style == Parallax.getValue(c)) {
            return new Parallax();
        } else if (style == Cards.getValue(c)) {
            return new Cards();
        } else if (style == Cards2.getValue(c)) {
            return new Cards2();
        } else if (style == NestedRecyclerView.getValue(c)) {
            return new NestedRecyclerView();
        } else if (style == List.getValue(c)) {
            return new List();
        }
        return null;
    }

    public int getColumnCount(Context c) {
        if (columnCount == 0) {
            columnCount = DEFAULT_COLUMN_COUNT;
        }

        boolean landscape = c.getResources().getBoolean(R.bool.landscape);
        if (landscape) {
            return columnCount + 1;
        }
        return columnCount;
    }

    public int getRealColumnCount() {
        if (columnCount == 0) {
            columnCount = DEFAULT_COLUMN_COUNT;
        }
        return columnCount;
    }

    public void setColumnCount(int columnCount) {
        this.columnCount = columnCount;
    }

    public int sortAlbumsBy() {
        return sort_albums_by;
    }

    public void sortAlbumsBy(Context c, int sort_albums_by) {
        this.sort_albums_by = sort_albums_by;
        String key = c.getString(R.string.pref_key_sort_albums);
        saveInt(c, key, sort_albums_by);
    }

    public int sortAlbumBy() {
        return sort_album_by;
    }

    public void sortAlbumBy(Context c, int sort_album_by) {
        this.sort_album_by = sort_album_by;
        String key = c.getString(R.string.pref_key_sort_album);
        saveInt(c, key, sort_album_by);
    }

    public boolean getHiddenFolders() {
        return hiddenFolders;
    }

    public boolean setHiddenFolders(Context c, boolean hiddenFolders) {
        this.hiddenFolders = hiddenFolders;
        saveBoolean(c, PREF_KEY_HIDDEN_FOLDERS, hiddenFolders);
        return hiddenFolders;
    }

    public boolean use8BitColor() {
        return use8BitColor;
    }

    public void use8BitColor(boolean use8BitColor) {
        this.use8BitColor = use8BitColor;
    }

    public boolean getCameraShortcut() {
        return cameraShortcut;
    }
    public Uri getRemovableStorageTreeUri() {
        Log.d("Settings", "getRemovableStorageTreeUri: " + removableStorageTreeUri);
        return removableStorageTreeUri;
    }

    public void setCameraShortcut(boolean cameraShortcut) {
        this.cameraShortcut = cameraShortcut;
    }

    public boolean showVideos() {
        return showVideos;
    }

    public void showVideos(boolean hideVideos) {
        this.showVideos = hideVideos;
    }

    public boolean getVirtualDirectories() {
        return virtualDirectories;
    }

    @SuppressWarnings("unused")
    public void setVirtualDirectories(Context c, boolean virtualDirectories) {
        this.virtualDirectories = virtualDirectories;
        saveBoolean(c, c.getString(R.string.pref_key_virtual_directories), virtualDirectories);
    }

    public boolean fadeImages() {
        return fadeImages;
    }

    public boolean noFolderMode() {
        return false;
    }

    public boolean showAnimations() {
        return showAnimations;
    }

    public void showAnimations(boolean showAnimations) {
        this.showAnimations = showAnimations;
    }

    public boolean isMaxBrightness() {
        return maxBrightness;
    }

    public void setMaxBrightness(boolean maxBrightness) {
        this.maxBrightness = maxBrightness;
    }

    public float getPrevBrightness() {
        return prevBrightness;
    }

    public void setPrevBrightness(float prevBrightness) {
        this.prevBrightness = prevBrightness;
    }

    public void setRemovableStorageTreeUri(Context c, Uri removableStorageTreeUri) {
        this.removableStorageTreeUri = removableStorageTreeUri;
        saveString(c, c.getString(R.string.pref_key_removable_storage_treeUri), removableStorageTreeUri.toString());
    }

    public static class Utils {
        public static String getThemeName(Context c, String themeValue) {
            int valuesRes = R.array.theme_values;
            int namesRes = R.array.theme_names;
            return getValueName(c, themeValue, valuesRes, namesRes);
        }

        public static String getStyleName(Context c, int styleValue) {
            int valuesRes = R.array.style_values;
            int namesRes = R.array.style_names;
            return getValueName(c, styleValue, valuesRes, namesRes);
        }


        private static String getValueName(Context c, String value, int valuesRes, int namesRes) {
            String[] values = c.getResources().getStringArray(valuesRes);
            String[] names = c.getResources().getStringArray(namesRes);

            int index = -1;
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(value)) {
                    index = i;
                    break;
                }
            }
            if (index >= 0) {
                return names[index];
            }
            return "Error";
        }

        private static String getValueName(Context c, int value, int valuesRes, int namesRes) {
            int[] values = c.getResources().getIntArray(valuesRes);
            String[] names = c.getResources().getStringArray(namesRes);

            int index = -1;
            for (int i = 0; i < values.length; i++) {
                if (values[i] == value) {
                    index = i;
                    break;
                }
            }
            if (index >= 0) {
                return names[index];
            }
            return "Error";
        }
    }
}
