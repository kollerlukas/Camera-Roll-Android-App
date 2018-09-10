package us.koller.cameraroll.preferences;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.themes.Theme;

public class ColumnCountPreferenceDialogFragment
        extends DialogFragment implements DialogInterface.OnClickListener {

    private int columnCount = Settings.DEFAULT_COLUMN_COUNT;
    private int whichButtonClicked;
    private Preference preference;

    public static ColumnCountPreferenceDialogFragment newInstance(Preference pref) {
        ColumnCountPreferenceDialogFragment frag = new ColumnCountPreferenceDialogFragment();
        frag.setPreference(pref);
        return frag;
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

        @SuppressLint("InflateParams") View v = LayoutInflater.from(getContext())
                .inflate(R.layout.pref_dialog_column_count, null);

        final TextView textView = v.findViewById(R.id.column_count);
        textView.setText(String.valueOf(columnCount));

        View.OnClickListener onClickListener = (View V) -> {
            if (V.getId() == R.id.minus) {
                if (columnCount > 1) {
                    columnCount--;
                }
            } else {
                columnCount++;
            }
            textView.setText(String.valueOf(columnCount));
        };

        Theme theme = Settings.getInstance(getContext()).getThemeInstance(getContext());
        int textColorSec = theme.getTextColorSecondary(getContext());

        ImageButton minus = v.findViewById(R.id.minus);
        minus.setColorFilter(textColorSec);
        minus.setOnClickListener(onClickListener);

        ImageButton plus = v.findViewById(R.id.plus);
        plus.setColorFilter(textColorSec);
        plus.setOnClickListener(onClickListener);

        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.column_count)
                .setView(v)
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
                && preference instanceof ColumnCountPreference) {
            ColumnCountPreference cCP = ((ColumnCountPreference) preference);
            cCP.setColumnCount(columnCount);
            Settings.getInstance(getActivity()).setColumnCount(columnCount);
        }
    }

    public void setPreference(Preference pref) {
        this.preference = pref;
    }
}
