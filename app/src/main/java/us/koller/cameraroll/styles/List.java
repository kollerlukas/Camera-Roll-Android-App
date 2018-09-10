package us.koller.cameraroll.styles;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import us.koller.cameraroll.R;

public class List extends Style {

    public static int getValue(Context c) {
        return c.getResources().getInteger(R.integer.STYLE_LIST_VALUE);
    }

    @Override
    public int getViewHolderLayoutRes() {
        return R.layout.album_cover_list;
    }

    @Override
    String getColumnCountPrefKey(Context c) {
        return c.getString(R.string.STYLE_LIST_COLUMN_COUNT_PREF_KEY);
    }

    @Override
    int getDefaultColumnCount() {
        return 1;
    }

    @Override
    boolean columnCountChangeable() {
        return false;
    }

    @Override
    int getGridSpacingRes() {
        return R.dimen.list_style_grid_spacing;
    }

    @Override
    public View createPrefDialogView(@NonNull ViewGroup container) {
        View v = inflatePrefDialogItemView(container);

        Context c = container.getContext();

        TextView name = v.findViewById(R.id.name);
        name.setText(c.getString(R.string.STYLE_LIST_NAME));

        ImageView iV = v.findViewById(R.id.image);
        iV.setImageResource(R.drawable.style_list);
        iV.setColorFilter(getAccentColor(c));
        disableColumnCountButtons(v);
        return v;
    }
}
