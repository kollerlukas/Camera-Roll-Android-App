package us.koller.cameraroll.styles;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.main.viewHolder.SimpleAlbumHolder;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.themes.Theme;

public abstract class Style {

    private int columnCount = -1;
    private float gridSpacing = -1;

    public abstract int getViewHolderLayoutRes();

    View inflateView(@NonNull ViewGroup parent) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        return inf.inflate(getViewHolderLayoutRes(), parent, false);
    }

    public RecyclerView.ViewHolder createViewHolderInstance(@NonNull ViewGroup parent) {
        return new SimpleAlbumHolder(inflateView(parent));
    }

    public int getColumnCount(Context c) {
        if (columnCount == -1) {
            columnCount = retrieveColumnCount(c);
        }
        Resources res = c.getResources();
        boolean landscape = res.getBoolean(R.bool.landscape);
        if (landscape && columnCountIncreasesInLandscape()) {
            return columnCount + 1;
        }
        return columnCount;
    }

    boolean columnCountChangeable() {
        return true;
    }

    private void setColumnCount(Context c, int columnCount) {
        if (!columnCountChangeable()) {
            return;
        }
        this.columnCount = columnCount;
        Settings.saveInt(c, getColumnCountPrefKey(c), columnCount);
    }

    abstract String getColumnCountPrefKey(Context c);

    private int retrieveColumnCount(Context c) {
        SharedPreferences sP = PreferenceManager.getDefaultSharedPreferences(c);
        return sP.getInt(getColumnCountPrefKey(c), getDefaultColumnCount());
    }

    abstract int getDefaultColumnCount();

    public boolean columnCountIncreasesInLandscape() {
        return false;
    }

    public float getGridSpacing(Context c) {
        if (gridSpacing == -1) {
            gridSpacing = retrieveGridSpacing(c);
        }
        return gridSpacing;
    }

    private float retrieveGridSpacing(Context c) {
        return c.getResources().getDimension(getGridSpacingRes());
    }

    abstract int getGridSpacingRes();

    /*For the Preference Dialog in the Settings*/
    public abstract View createPrefDialogView(@NonNull ViewGroup container);

    ViewGroup inflatePrefDialogItemView(@NonNull ViewGroup container) {
        return (ViewGroup) LayoutInflater.from(container.getContext())
                .inflate(R.layout.pref_dialog_style_item, container, false);
    }

    int getAccentColor(Context c) {
        Settings settings = Settings.getInstance(c);
        Theme t = settings.getThemeInstance(c);
        return t.getAccentColor(c);
    }

    void disableColumnCountButtons(View v) {
        v.findViewById(R.id.column_count_buttons).setVisibility(View.GONE);
    }

    void setColumnCountButtonsClickListener(final View v) {
        final TextView columnCountTV = v.findViewById(R.id.column_count);
        columnCountTV.setText(String.valueOf(getColumnCount(v.getContext())));

        View.OnClickListener clickListener = (View V) -> {
            int columnCount = getColumnCount(V.getContext());
            switch (V.getId()) {
                case R.id.minus:
                    if (columnCount > 1) {
                        columnCount--;
                    }
                    break;
                case R.id.plus:
                    columnCount++;
                    break;
                default:
                    break;
            }
            setColumnCount(V.getContext(), columnCount);
            columnCountTV.setText(String.valueOf(columnCount));
        };

        Settings s = Settings.getInstance(v.getContext());
        Theme t = s.getThemeInstance(v.getContext());
        int textColor = t.getTextColorSecondary(v.getContext());

        ImageButton minus = v.findViewById(R.id.minus);
        minus.setOnClickListener(clickListener);
        minus.setColorFilter(textColor);

        ImageButton plus = v.findViewById(R.id.plus);
        plus.setOnClickListener(clickListener);
        plus.setColorFilter(textColor);
    }
}
