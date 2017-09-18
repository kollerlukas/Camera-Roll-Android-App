package us.koller.cameraroll.ui.widget;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.view.View;

import us.koller.cameraroll.util.Util;

@SuppressWarnings("unused")
public class FabSnackbarBehaviour extends CoordinatorLayout.Behavior<FloatingActionButton> {

    private float fabTranslationY = -1;
    private float fabBottom = -1;

    public FabSnackbarBehaviour(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, FloatingActionButton fab, View dependency) {
        return Util.SNACKBAR.equals(dependency.getTag());
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionButton fab, View dependency) {
        if (Util.SNACKBAR.equals(dependency.getTag())) {
            if (fabTranslationY == -1) {
                fabTranslationY = fab.getTranslationY();
                CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) fab.getLayoutParams();
                int fabBottomMargin = lp.bottomMargin;
                fabBottom = fab.getY() + fab.getHeight() + fabBottomMargin;
            }

            if (dependency.getVisibility() == View.INVISIBLE || dependency.getVisibility() == View.GONE) {
                fab.animate()
                        .translationY(fabTranslationY)
                        .start();
            } else if (dependency.getY() < fabBottom) {
                float delta = fabBottom - dependency.getY();
                float translationY = fabTranslationY - delta;
                fab.setTranslationY(translationY);
            } else {
                fab.setTranslationY(fabTranslationY);
            }
        }
        return true;
    }
}