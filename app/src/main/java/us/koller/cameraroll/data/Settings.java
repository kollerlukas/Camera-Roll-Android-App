package us.koller.cameraroll.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import us.koller.cameraroll.R;
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
    private boolean use8BitColor;
    private boolean cameraShortcut;
    private Uri removableStorageTreeUri;

    private static Settings instance;

    public static Settings getInstance(Context context) {
        if (instance == null) {
            instance = new Settings(context);
        }
        return instance;
    }

    private Settings(Context context) {
        SharedPreferences sharedPreferences
                = PreferenceManager.getDefaultSharedPreferences(context);

        theme = sharedPreferences.getString(
                context.getString(R.string.pref_key_theme),
                context.getString(R.string.DARK_THEME_VALUE));

        storageRetriever = sharedPreferences.getBoolean(
                context.getString(R.string.pref_key_media_retriever),
                false);

        style = sharedPreferences.getInt(
                context.getString(R.string.pref_key_style),
                context.getResources().getInteger(R.integer.STYLE_PARALLAX_VALUE));

        columnCount = sharedPreferences.getInt(
                context.getString(R.string.pref_key_column_count),
                DEFAULT_COLUMN_COUNT);

        sort_albums_by = sharedPreferences.getInt(
                context.getString(R.string.pref_key_sort_albums),
                SortUtil.BY_NAME);

        sort_album_by = sharedPreferences.getInt(
                context.getString(R.string.pref_key_sort_album),
                SortUtil.BY_DATE);


        hiddenFolders = sharedPreferences.getBoolean
                (PREF_KEY_HIDDEN_FOLDERS, false);

        use8BitColor = sharedPreferences.getBoolean(
                context.getString(R.string.pref_key_8_bit_color),
                false);

        cameraShortcut = sharedPreferences.getBoolean(
                context.getString(R.string.pref_key_camera_shortcut),
                false);

        removableStorageTreeUri = Uri.parse(sharedPreferences.getString(
                context.getString(R.string.pref_key_removable_storage_treeUri),
                ""));
    }

    /*Getter & Setter*/
    public String getTheme() {
        return theme;
    }

    public Theme getThemeInstance(Context context) {
        String theme = getTheme();
        Resources res = context.getResources();
        if (theme.equals(res.getString(R.string.LIGHT_THEME_VALUE))) {
            return new LightTheme();
        } else if (theme.equals(res.getString(R.string.BLACK_THEME_VALUE))) {
            return new BlackTheme();
        } else {
            return new DarkTheme();
        }
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public boolean useStorageRetriever() {
        return storageRetriever;
    }

    public void useStorageRetriever(boolean storageRetriever) {
        this.storageRetriever = storageRetriever;
    }

    public int getStyle(Context context, boolean pickPhotos) {
        Resources res = context.getResources();
        if (pickPhotos && style == res.getInteger(R.integer.STYLE_NESTED_RECYCLER_VIEW_VALUE)) {
            return res.getInteger(R.integer.STYLE_CARDS_VALUE);
        }
        return style;
    }

    public void setStyle(int style) {
        this.style = style;
    }

    public int getStyleColumnCount(Context context, int style) {
        Resources res = context.getResources();
        boolean landscape = res.getBoolean(R.bool.landscape);
        int styleColumnCount = getDefaultStyleColumnCount(context, style);
        if (landscape &&
                (style == res.getInteger(R.integer.STYLE_CARDS_VALUE) ||
                        style == res.getInteger(R.integer.STYLE_CARDS_2_VALUE))) {
            return styleColumnCount + 1;
        }
        return styleColumnCount;
    }

    public int getStyleGridSpacing(Context context, int style) {
        Resources res = context.getResources();
        if (style == res.getInteger(R.integer.STYLE_CARDS_VALUE)) {
            return (int) res.getDimension(R.dimen.cards_style_grid_spacing);
        }
        return 0;
    }

    public int getColumnCount(Context context) {
        if (columnCount == 0) {
            columnCount = DEFAULT_COLUMN_COUNT;
        }

        boolean landscape = context.getResources().getBoolean(R.bool.landscape);
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

    public static int getDefaultStyleColumnCount(Context context, int style) {
        Resources res = context.getResources();
        if (style == res.getInteger(R.integer.STYLE_PARALLAX_VALUE)) {
            return res.getInteger(R.integer.STYLE_PARALLAX_COLUMN_COUNT);
        } else if (style == res.getInteger(R.integer.STYLE_CARDS_VALUE)) {
            return res.getInteger(R.integer.STYLE_CARDS_COLUMN_COUNT);
        } else if (style == res.getInteger(R.integer.STYLE_CARDS_2_VALUE)) {
            return res.getInteger(R.integer.STYLE_CARDS_2_COLUMN_COUNT);
        } else if (style == res.getInteger(R.integer.STYLE_NESTED_RECYCLER_VIEW_VALUE)) {
            return res.getInteger(R.integer.STYLE_NESTED_RECYCLER_VIEW_COLUMN_COUNT);
        }
        return 1;
    }

    public int sortAlbumsBy() {
        return sort_albums_by;
    }

    public void sortAlbumsBy(Context context, int sort_albums_by) {
        this.sort_albums_by = sort_albums_by;

        String key = context.getString(R.string.pref_key_sort_albums);
        saveInt(context, key, sort_albums_by);
    }

    public int sortAlbumBy() {
        return sort_album_by;
    }

    public void sortAlbumBy(Context context, int sort_album_by) {
        this.sort_album_by = sort_album_by;

        String key = context.getString(R.string.pref_key_sort_album);
        saveInt(context, key, sort_album_by);
    }

    public boolean getHiddenFolders() {
        return hiddenFolders;
    }

    public boolean setHiddenFolders(Context context, boolean hiddenFolders) {
        this.hiddenFolders = hiddenFolders;
        saveBoolean(context, PREF_KEY_HIDDEN_FOLDERS, hiddenFolders);
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

    public void setCameraShortcut(boolean cameraShortcut) {
        this.cameraShortcut = cameraShortcut;
    }

    public Uri getRemovableStorageTreeUri() {
        Log.d("Settings", "getRemovableStorageTreeUri: " + removableStorageTreeUri);
        return removableStorageTreeUri;
    }

    public void setRemovableStorageTreeUri(Context context, Uri removableStorageTreeUri) {
        this.removableStorageTreeUri = removableStorageTreeUri;
        saveString(context,
                context.getString(R.string.pref_key_removable_storage_treeUri),
                removableStorageTreeUri.toString());
    }

    private static void saveInt(Context context, String key, int value) {
        SharedPreferences sharedPreferences
                = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences
                .edit()
                .putInt(key, value)
                .apply();
    }

    private static void saveBoolean(Context context, String key, boolean value) {
        SharedPreferences sharedPreferences
                = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences
                .edit()
                .putBoolean(key, value)
                .apply();
    }

    private static void saveString(Context context, String key, String value) {
        SharedPreferences sharedPreferences
                = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences
                .edit()
                .putString(key, value)
                .apply();
    }

    public static class Utils {
        public static String getThemeName(Context context, String themeValue) {
            int valuesRes = R.array.theme_values;
            int namesRes = R.array.theme_names;
            return getValueName(context, themeValue, valuesRes, namesRes);
        }

        public static String getStyleName(Context context, int styleValue) {
            int valuesRes = R.array.style_values;
            int namesRes = R.array.style_names;
            return getValueName(context, styleValue, valuesRes, namesRes);
        }


        private static String getValueName(Context context, String value,
                                           int valuesRes, int namesRes) {
            String[] values = context.getResources().getStringArray(valuesRes);
            String[] names = context.getResources().getStringArray(namesRes);

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

        private static String getValueName(Context context, int value,
                                           int valuesRes, int namesRes) {
            int[] values = context.getResources().getIntArray(valuesRes);
            String[] names = context.getResources().getStringArray(namesRes);

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
