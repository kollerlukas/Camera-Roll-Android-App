package us.koller.cameraroll.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import us.koller.cameraroll.R;

public class Settings {

    private String theme;
    private boolean storageRetriever;

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


    public static class Utils {
        public static String getThemeName(Context context, String themeValue) {
            int valuesRes = R.array.theme_values;
            int namesRes = R.array.theme_names;
            return getValueName(context, themeValue, valuesRes, namesRes);
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
    }
}
