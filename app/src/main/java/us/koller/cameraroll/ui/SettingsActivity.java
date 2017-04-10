package us.koller.cameraroll.ui;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.support.v7.preference.TwoStatePreference;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;

import java.util.Arrays;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.preferences.StylePreference;
import us.koller.cameraroll.preferences.StylePreferenceDialogFragment;
import us.koller.cameraroll.util.Util;

public class SettingsActivity extends ThemeableActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        //setting window insets manually
        final View rootView = findViewById(R.id.root_view);
        final View container = findViewById(R.id.preference_fragment_container);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            rootView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
                public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                    toolbar.setPadding(toolbar.getPaddingStart() /*+ insets.getSystemWindowInsetLeft()*/,
                            toolbar.getPaddingTop() + insets.getSystemWindowInsetTop(),
                            toolbar.getPaddingEnd() /*+ insets.getSystemWindowInsetRight()*/,
                            toolbar.getPaddingBottom());

                    ViewGroup.MarginLayoutParams toolbarParams
                            = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
                    toolbarParams.leftMargin += insets.getSystemWindowInsetLeft();
                    toolbarParams.rightMargin += insets.getSystemWindowInsetRight();
                    toolbar.setLayoutParams(toolbarParams);

                    container.setPadding(container.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                            container.getPaddingTop(),
                            container.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                            container.getPaddingBottom() + insets.getSystemWindowInsetBottom());

                    // clear this listener so insets aren't re-applied
                    rootView.setOnApplyWindowInsetsListener(null);
                    return insets.consumeSystemWindowInsets();
                }
            });
        } else {
            rootView.getViewTreeObserver()
                    .addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    // hacky way of getting window insets on pre-Lollipop
                                    // somewhat works...
                                    int[] screenSize = Util.getScreenSize(SettingsActivity.this);

                                    int[] windowInsets = new int[]{
                                            Math.abs(screenSize[0] - rootView.getLeft()),
                                            Math.abs(screenSize[1] - rootView.getTop()),
                                            Math.abs(screenSize[2] - rootView.getRight()),
                                            Math.abs(screenSize[3] - rootView.getBottom())};

                                    Log.d("MainActivity", "windowInsets: " + Arrays.toString(windowInsets));

                                    toolbar.setPadding(toolbar.getPaddingStart(),
                                            toolbar.getPaddingTop() + windowInsets[1],
                                            toolbar.getPaddingEnd(),
                                            toolbar.getPaddingBottom());

                                    ViewGroup.MarginLayoutParams toolbarParams
                                            = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
                                    toolbarParams.leftMargin += windowInsets[0];
                                    toolbarParams.rightMargin += windowInsets[2];
                                    toolbar.setLayoutParams(toolbarParams);

                                    container.setPadding(container.getPaddingStart() + windowInsets[0],
                                            container.getPaddingTop(),
                                            container.getPaddingEnd() + windowInsets[2],
                                            container.getPaddingBottom() + windowInsets[3]);

                                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                }
                            });
        }

        SettingsFragment fragment = new SettingsFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.preference_fragment_container, fragment)
                .commit();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        //so other activities are recreated
        ThemeableActivity.THEME = ThemeableActivity.UNDEFINED;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public int getThemeRes(int style) {
        if (style == DARK) {
            return R.style.Theme_CameraRoll_Settings;
        } else {
            return R.style.Theme_CameraRoll_Light_Settings;
        }
    }

    @Override
    public void onThemeApplied(int theme) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(ContextCompat.getColor(this, toolbar_color_res));
        toolbar.setTitleTextColor(ContextCompat.getColor(this, text_color_res));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int statusBarColorRes = theme == DARK ? R.color.black : R.color.grey_300;
            getWindow().setStatusBarColor(ContextCompat.getColor(this, statusBarColorRes));
        }

        if (theme == LIGHT) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Util.setDarkStatusBarIcons(findViewById(R.id.root_view));
            }
        }
    }


    public static class SettingsFragment extends PreferenceFragmentCompat
            implements Preference.OnPreferenceChangeListener {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.preferences);

            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(getActivity());

            initThemePref(sharedPreferences);

            initStylePref(sharedPreferences);

            initMediaRetrieverPref(sharedPreferences);
        }

        private void initThemePref(SharedPreferences sharedPreferences) {
            ListPreference themePref = (ListPreference)
                    findPreference(getString(R.string.pref_key_theme));

            String theme = sharedPreferences.getString(
                    getString(R.string.pref_key_theme),
                    getString(R.string.DARK_THEME_VALUE));

            String theme_name = Settings.Utils.getThemeName(getActivity(), theme);
            themePref.setSummary(theme_name);
            themePref.setOnPreferenceChangeListener(this);
        }

        private void initStylePref(SharedPreferences sharedPreferences) {
            StylePreference stylePref = (StylePreference)
                    findPreference(getString(R.string.pref_key_style));

            int style = sharedPreferences.getInt(
                    getString(R.string.pref_key_style),
                    getActivity().getResources().getInteger(R.integer.STYLE_PARALLAX_VALUE));

            String style_name = Settings.Utils.getStyleName(getActivity(), style);
            stylePref.setSummary(style_name);
            stylePref.setOnPreferenceChangeListener(this);
        }

        private void initMediaRetrieverPref(SharedPreferences sharedPreferences) {
            TwoStatePreference mediaRetrieverPref =
                    (TwoStatePreference) findPreference(getString(R.string.pref_key_media_retriever));

            boolean storageRetriever = sharedPreferences.getBoolean(
                    getString(R.string.pref_key_media_retriever),
                    false);

            Log.d("SettingsFragment", "storageRetriever: " + String.valueOf(storageRetriever));

            mediaRetrieverPref.setChecked(storageRetriever);

            Log.d("SettingsFragment", "mediaRetrieverPref.isChecked(): " + String.valueOf(mediaRetrieverPref.isChecked()));
            mediaRetrieverPref.setOnPreferenceChangeListener(this);
        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            if (preference instanceof StylePreference) {
                DialogFragment dialogFragment
                        = StylePreferenceDialogFragment
                        .newInstance(preference);
                dialogFragment.setTargetFragment(this, 0);
                dialogFragment.show(this.getFragmentManager(), "android.support.v7.preference.PreferenceFragment.DIALOG");
                return;
            }
            super.onDisplayPreferenceDialog(preference);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            Log.d("SettingsFragment", "onPreferenceChange() called with: preference = [" + preference + "], o = [" + o + "]");
            Settings settings = Settings.getInstance(getActivity());
            if (preference.getKey().equals(getString(R.string.pref_key_theme))) {
                settings.setTheme((String) o);

                String theme_name = Settings.Utils.getThemeName(getActivity(), (String) o);
                preference.setSummary(theme_name);

                //update Activities
                ThemeableActivity.THEME = ThemeableActivity.UNDEFINED;
                getActivity().recreate();
            } else if (preference.getKey().equals(getString(R.string.pref_key_style))) {
                settings.setStyle((int) o);

                String style_name = Settings.Utils.getStyleName(getActivity(), (int) o);
                preference.setSummary(style_name);

                //update Style column count
                int columnCount = Settings.getDefaultStyleColumnCount(getActivity(), (int) o);
                Settings.getInstance(getActivity()).setStyleColumnCount(columnCount);

            } else if (preference.getKey().equals(getString(R.string.pref_key_media_retriever))) {
                settings.useStorageRetriever((boolean) o);
            }
            return true;
        }
    }
}
