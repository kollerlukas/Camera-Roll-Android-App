package us.koller.cameraroll.preferences;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import com.bumptech.glide.Glide;
import com.pixelcan.inkpageindicator.InkPageIndicator;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Settings;

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

        styles = getContext().getResources()
                .getIntArray(R.array.style_values);

        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.pref_dialog_style, null);

        ViewPager viewPager = (ViewPager) view.findViewById(R.id.view_pager);
        viewPager.setAdapter(new ViewPagerAdapter());
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

        InkPageIndicator inkPageIndicator
                = (InkPageIndicator) view.findViewById(R.id.indicator);
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
        if (whichButtonClicked == DialogInterface.BUTTON_POSITIVE) {
            if (preference instanceof StylePreference) {
                StylePreference stylePreference =
                        ((StylePreference) preference);
                stylePreference.setStyle(selectedStyle);

                Settings.getInstance(getActivity()).setStyle(selectedStyle);
            }
        }
    }

    public void setPreference(Preference preference) {
        this.preference = preference;
    }

    public static class ViewPagerAdapter extends PagerAdapter {

        private static int[] nameRess = {
                R.string.STYLE_PARALLAX_NAME,
                R.string.STYLE_CARDS_NAME,
                R.string.STYLE_NESTED_RECYCLER_VIEW_NAME};

        private static int[] imageRess = {
                R.drawable.style_parallax,
                R.drawable.style_cards,
                R.drawable.style_nested_recycler_view};

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = LayoutInflater.from(container.getContext())
                    .inflate(R.layout.pref_dialog_style_item, container, false);

            TextView textView = (TextView) view.findViewById(R.id.name);
            textView.setText(nameRess[position]);

            ImageView imageView = (ImageView) view.findViewById(R.id.image);
            Glide.with(imageView.getContext())
                    .load(imageRess[position])
                    .into(imageView);

            imageView.setColorFilter(ContextCompat
                    .getColor(container.getContext(), R.color.colorAccent));

            container.addView(view);
            return view;
        }

        @Override
        public int getCount() {
            return nameRess.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((View) view);
        }
    }
}
