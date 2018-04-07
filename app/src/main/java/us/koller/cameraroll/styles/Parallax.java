package us.koller.cameraroll.styles;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import us.koller.cameraroll.R;

public class Parallax extends Style {

    public static int getValue(Context context) {
        return context.getResources().getInteger(R.integer.STYLE_PARALLAX_VALUE);
    }

    @Override
    public int getViewHolderLayoutRes() {
        return R.layout.album_cover_parallax;
    }

    @Override
    String getColumnCountPrefKey(Context context) {
        return context.getString(R.string.STYLE_PARALLAX_COLUMN_COUNT_PREF_KEY);
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
        ViewGroup viewGroup = inflatePrefDialogItemView(container);

        Context context = container.getContext();

        TextView name = viewGroup.findViewById(R.id.name);
        name.setText(context.getString(R.string.STYLE_PARALLAX_NAME));

        ImageView imageView = viewGroup.findViewById(R.id.image);
        imageView.setImageResource(R.drawable.style_parallax);
        imageView.setColorFilter(getAccentColor(context));

        setColumnCountButtonsClickListener(viewGroup);

        return viewGroup;
    }
}
