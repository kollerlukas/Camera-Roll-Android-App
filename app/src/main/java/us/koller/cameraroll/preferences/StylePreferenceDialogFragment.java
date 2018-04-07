package us.koller.cameraroll.preferences;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pixelcan.inkpageindicator.InkPageIndicator;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.styles.Style;

public class StylePreferenceDialogFragment
        extends DialogFragment implements DialogInterface.OnClickListener {

    private int[] styles;
    int selectedStyle;
    private int whichButtonClicked;
    private Preference preference;

    public static StylePreferenceDialogFragment newInstance(Preference preference) {
        StylePreferenceDialogFragment fragment = new StylePreferenceDialogFragment();
        fragment.setPreference(preference);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //get initial value from pref
        if (preference instanceof StylePreference) {
            selectedStyle = ((StylePreference) preference).getStyle();
        }

        styles = getContext().getResources().getIntArray(R.array.style_values);

        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(getContext()).inflate(R.layout.pref_dialog_style, null);

        ViewPager viewPager = view.findViewById(R.id.view_pager);
        viewPager.setAdapter(new ViewPagerAdapter(getContext()));
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                selectedStyle = styles[position];
            }
        });

        int currentItem = 0;
        for (int i = 0; i < styles.length; i++) {
            if (styles[i] == selectedStyle) {
                currentItem = i;
                break;
            }
        }

        viewPager.setCurrentItem(currentItem);

        InkPageIndicator inkPageIndicator = view.findViewById(R.id.indicator);
        inkPageIndicator.setViewPager(viewPager);

        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.style)
                .setView(view)
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        whichButtonClicked = i;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (whichButtonClicked == DialogInterface.BUTTON_POSITIVE
                && preference instanceof StylePreference) {
            StylePreference stylePreference =
                    ((StylePreference) preference);
            stylePreference.setStyle(selectedStyle);

            Settings.getInstance(getActivity()).setStyle(selectedStyle);
        }
    }

    public void setPreference(Preference preference) {
        this.preference = preference;
    }

    public static class ViewPagerAdapter extends PagerAdapter {

        private Style[] styles;

        ViewPagerAdapter(Context context) {
            Settings settings = Settings.getInstance(context);
            int[] styleValues = context.getResources().getIntArray(R.array.style_values);
            styles = new Style[styleValues.length];
            for (int i = 0; i < styles.length; i++) {
                styles[i] = settings.getStyleInstance(context, styleValues[i]);
            }
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            Style style = styles[position];
            View view = style.createPrefDialogView(container);
            container.addView(view);
            return view;
        }

        @Override
        public int getCount() {
            return styles != null ? styles.length : 0;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view.equals(object);
        }

        @Override
        public void destroyItem(@NonNull ViewGroup collection, int position, @NonNull Object view) {
            collection.removeView((View) view);
        }
    }
}
