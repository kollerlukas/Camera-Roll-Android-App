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

import com.rd.PageIndicatorView;
import com.rd.animation.type.AnimationType;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.styles.Style;

public class StylePreferenceDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    private int[] styles;
    int selectedStyle;
    private int whichButtonClicked;
    private Preference preference;

    public static StylePreferenceDialogFragment newInstance(Preference pref) {
        StylePreferenceDialogFragment frag = new StylePreferenceDialogFragment();
        frag.setPreference(pref);
        return frag;
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

        ViewPager vP = view.findViewById(R.id.view_pager);
        vP.setAdapter(new ViewPagerAdapter(getContext()));
        vP.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
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
        vP.setCurrentItem(currentItem);

        PageIndicatorView ind = view.findViewById(R.id.indicator);
        ind.setAnimationType(AnimationType.WORM);

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
    public void onDismiss(DialogInterface d) {
        super.onDismiss(d);
        if (whichButtonClicked == DialogInterface.BUTTON_POSITIVE
                && preference instanceof StylePreference) {
            StylePreference sP = ((StylePreference) preference);
            sP.setStyle(selectedStyle);
            Settings.getInstance(getActivity()).setStyle(selectedStyle);
        }
    }

    public void setPreference(Preference pref) {
        this.preference = pref;
    }

    public static class ViewPagerAdapter extends PagerAdapter {
        private Style[] styles;

        ViewPagerAdapter(Context c) {
            Settings s = Settings.getInstance(c);
            int[] styleValues = c.getResources().getIntArray(R.array.style_values);
            styles = new Style[styleValues.length];
            for (int i = 0; i < styles.length; i++) {
                styles[i] = s.getStyleInstance(c, styleValues[i]);
            }
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            Style style = styles[position];
            View v = style.createPrefDialogView(container);
            container.addView(v);
            return v;
        }

        @Override
        public int getCount() {
            return styles != null ? styles.length : 0;
        }

        @Override
        public boolean isViewFromObject(@NonNull View v, @NonNull Object object) {
            return v.equals(object);
        }

        @Override
        public void destroyItem(@NonNull ViewGroup collection, int position, @NonNull Object v) {
            collection.removeView((View) v);
        }
    }
}
