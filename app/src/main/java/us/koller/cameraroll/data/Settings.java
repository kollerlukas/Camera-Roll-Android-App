package us.koller.cameraroll.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import us.koller.cameraroll.R;

public class Settings {

    private String theme;
    private boolean storageRetriever;
    private int style;
    private int styleColumnCount;
    private int columnCount;

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
                context.getResources().getInteger(R.integer.STYLE_CARDS_VALUE));

        styleColumnCount = getDefaultStyleColumnCount(context, style);

        boolean landscape = context.getResources().getBoolean(R.bool.landscape);
        int defaultColumnCount = !landscape ? 3 : 4;
        columnCount = sharedPreferences.getInt(
                context.getString(R.string.pref_key_column_count),
                defaultColumnCount);
    }

    /*Getter & Setter*/
    public String getTheme() {
        return theme;
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

    public int getStyle() {
        return style;
    }

    public void setStyle(int style) {
        this.style = style;
    }

    public int getStyleColumnCount(Context context) {
        boolean landscape = context.getResources().getBoolean(R.bool.landscape);
        if (landscape && getStyle() != context.getResources()
                .getInteger(R.integer.STYLE_PARALLAX_VALUE)) {
            return styleColumnCount + 1;
        }
        return styleColumnCount;
    }

    public void setStyleColumnCount(int styleColumnCount) {
        this.styleColumnCount = styleColumnCount;
    }

    public int getColumnCount(Context context) {
        boolean landscape = context.getResources().getBoolean(R.bool.landscape);
        if (landscape) {
            return columnCount + 1;
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
        }
        return 1;
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
