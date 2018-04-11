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
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return inflater.inflate(getViewHolderLayoutRes(), parent, false);
    }

    public RecyclerView.ViewHolder createViewHolderInstance(@NonNull ViewGroup parent) {
        return new SimpleAlbumHolder(inflateView(parent));
    }

    public int getColumnCount(Context context) {
        if (columnCount == -1) {
            columnCount = retrieveColumnCount(context);
        }
        Resources res = context.getResources();
        boolean landscape = res.getBoolean(R.bool.landscape);
        if (landscape && columnCountIncreasesInLandscape()) {
            return columnCount + 1;
        }
        return columnCount;
    }

    boolean columnCountChangeable() {
        return true;
    }

    private void setColumnCount(Context context, int columnCount) {
        if (!columnCountChangeable()) {
            return;
        }
        this.columnCount = columnCount;
        Settings.saveInt(context, getColumnCountPrefKey(context), columnCount);
    }

    abstract String getColumnCountPrefKey(Context context);

    private int retrieveColumnCount(Context context) {
        SharedPreferences sharedPreferences
                = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt(getColumnCountPrefKey(context), getDefaultColumnCount());
    }

    abstract int getDefaultColumnCount();

    public boolean columnCountIncreasesInLandscape() {
        return false;
    }

    public float getGridSpacing(Context context) {
        if (gridSpacing == -1) {
            gridSpacing = retrieveGridSpacing(context);
        }
        return gridSpacing;
    }

    private float retrieveGridSpacing(Context context) {
        return context.getResources().getDimension(getGridSpacingRes());
    }

    abstract int getGridSpacingRes();

    /*For the Preference Dialog in the Settings*/
    public abstract View createPrefDialogView(@NonNull ViewGroup container);

    ViewGroup inflatePrefDialogItemView(@NonNull ViewGroup container) {
        return (ViewGroup) LayoutInflater.from(container.getContext())
                .inflate(R.layout.pref_dialog_style_item, container, false);
    }

    int getAccentColor(Context context) {
        Settings settings = Settings.getInstance(context);
        Theme theme = settings.getThemeInstance(context);
        return theme.getAccentColor(context);
    }

    void disableColumnCountButtons(View view) {
        view.findViewById(R.id.column_count_buttons).setVisibility(View.GONE);
    }

    void setColumnCountButtonsClickListener(final View view) {
        final TextView columnCountTV = view.findViewById(R.id.column_count);
        columnCountTV.setText(String.valueOf(getColumnCount(view.getContext())));

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int columnCount = getColumnCount(v.getContext());
                switch (v.getId()) {
                    case R.id.minus:
                        columnCount--;
                        break;
                    case R.id.plus:
                        columnCount++;
                        break;
                    default:
                        break;
                }
                setColumnCount(v.getContext(), columnCount);
                columnCountTV.setText(String.valueOf(columnCount));
            }
        };

        Settings s = Settings.getInstance(view.getContext());
        Theme theme = s.getThemeInstance(view.getContext());
        int textColor = theme.getTextColorSecondary(view.getContext());

        ImageButton minus = view.findViewById(R.id.minus);
        minus.setOnClickListener(clickListener);
        minus.setColorFilter(textColor);

        ImageButton plus = view.findViewById(R.id.plus);
        plus.setOnClickListener(clickListener);
        plus.setColorFilter(textColor);
    }
}
