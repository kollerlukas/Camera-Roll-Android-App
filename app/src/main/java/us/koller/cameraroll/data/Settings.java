package us.koller.cameraroll.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import us.koller.cameraroll.R;

public class Settings {

    private String theme;

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
    }

    /*Getter & Setter*/
    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }


    public static class Utils {
        public static String getThemeName(Context context, String themeValue) {
            String[] values = context.getResources().getStringArray(R.array.theme_values);
            String[] names = context.getResources().getStringArray(R.array.theme_names);

            int index = -1;
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(themeValue)) {
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
