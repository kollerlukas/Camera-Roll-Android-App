package us.koller.cameraroll.styles;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import us.koller.cameraroll.R;

public class Parallax extends Style {

    public static int getValue(Context c) {
        return c.getResources().getInteger(R.integer.STYLE_PARALLAX_VALUE);
    }

    @Override
    public int getViewHolderLayoutRes() {
        return R.layout.album_cover_parallax;
    }

    @Override
    String getColumnCountPrefKey(Context c) {
        return c.getString(R.string.STYLE_PARALLAX_COLUMN_COUNT_PREF_KEY);
    }

    @Override
    int getDefaultColumnCount() {
        return 1;
    }

    @Override
    int getGridSpacingRes() {
        return R.dimen.parallax_style_grid_spacing;
    }

    @Override
    public View createPrefDialogView(@NonNull ViewGroup container) {
        ViewGroup vG = inflatePrefDialogItemView(container);

        Context c = container.getContext();

        TextView name = vG.findViewById(R.id.name);
        name.setText(c.getString(R.string.STYLE_PARALLAX_NAME));

        ImageView iV = vG.findViewById(R.id.image);
        iV.setImageResource(R.drawable.style_parallax);
        iV.setColorFilter(getAccentColor(c));
        setColumnCountButtonsClickListener(vG);
        return vG;
    }
}
