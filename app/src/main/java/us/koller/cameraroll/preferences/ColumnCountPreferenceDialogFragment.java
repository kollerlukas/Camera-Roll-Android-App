package us.koller.cameraroll.preferences;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.ui.ThemeableActivity;

public class ColumnCountPreferenceDialogFragment
        extends DialogFragment implements DialogInterface.OnClickListener {

    private int columnCount = Settings.DEFAULT_COLUMN_COUNT;
    private int whichButtonClicked;
    private Preference preference;

    public static ColumnCountPreferenceDialogFragment newInstance(Preference preference) {
        ColumnCountPreferenceDialogFragment fragment = new ColumnCountPreferenceDialogFragment();
        fragment.setPreference(preference);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //get initial value from pref
        if (preference instanceof ColumnCountPreference) {
            columnCount = ((ColumnCountPreference) preference).getColumnCount();
            if (columnCount == 0) {
                columnCount = Settings.DEFAULT_COLUMN_COUNT;
            }
        }

        @SuppressLint("InflateParams") View view = LayoutInflater.from(getContext())
                .inflate(R.layout.pref_dialog_column_count, null);

        final TextView textView = (TextView) view.findViewById(R.id.column_count);
        textView.setText(String.valueOf(columnCount));

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view.getId() == R.id.minus) {
                    if (columnCount > 1) {
                        columnCount--;
                    }
                } else {
                    columnCount++;
                }
                textView.setText(String.valueOf(columnCount));
            }
        };

        int secondaryTextColor = ContextCompat.getColor(getContext(),
                ThemeableActivity.text_color_secondary_res);

        ImageButton minus = (ImageButton) view.findViewById(R.id.minus);
        minus.setColorFilter(secondaryTextColor);
        minus.setOnClickListener(onClickListener);

        ImageButton plus = (ImageButton) view.findViewById(R.id.plus);
        plus.setColorFilter(secondaryTextColor);
        plus.setOnClickListener(onClickListener);

        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.column_count)
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
            if (preference instanceof ColumnCountPreference) {
                ColumnCountPreference columnCountPreference =
                        ((ColumnCountPreference) preference);
                columnCountPreference.setColumnCount(columnCount);

                Settings.getInstance(getActivity())
                        .setColumnCount(columnCount);
            }
        }
    }

    public void setPreference(Preference preference) {
        this.preference = preference;
    }
}
