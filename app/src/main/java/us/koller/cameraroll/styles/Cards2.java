package us.koller.cameraroll.styles;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import us.koller.cameraroll.R;

public class Cards2 extends Style {

    public static int getValue(Context c) {
        return c.getResources().getInteger(R.integer.STYLE_CARDS_2_VALUE);
    }

    @Override
    public int getViewHolderLayoutRes() {
        return R.layout.album_cover_card_2;
    }

    @Override
    public boolean columnCountIncreasesInLandscape() {
        return true;
    }

    @Override
    String getColumnCountPrefKey(Context c) {
        return c.getString(R.string.STYLE_CARDS_2_COLUMN_COUNT_PREF_KEY);
    }

    @Override
    int getDefaultColumnCount() {
        return 2;
    }

    @Override
    int getGridSpacingRes() {
        return R.dimen.cards_style_grid_spacing;
    }

    @Override
    public View createPrefDialogView(@NonNull ViewGroup container) {
        View v = inflatePrefDialogItemView(container);

        Context c = container.getContext();

        TextView name = v.findViewById(R.id.name);
        name.setText(c.getString(R.string.STYLE_CARDS_2_NAME));

        ImageView iV = v.findViewById(R.id.image);
        iV.setImageResource(R.drawable.style_cards_2);
        iV.setColorFilter(getAccentColor(c));
        setColumnCountButtonsClickListener(v);
        return v;
    }
}
