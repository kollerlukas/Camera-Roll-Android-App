package us.koller.cameraroll.util;

import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;

public class ParallaxTransformer implements ViewPager.PageTransformer {

    private static final float PARALLAX_OFFSET = 0.5f;

    @Override
    public void transformPage(View page, float position) {
        View parallaxView = findParallaxView(page);
        if (parallaxView == null) {
            return;
        }

        int translationX = 0;
        if (position >= -1 && position <= 1) {
            int direction = position > 0 ? -1 : 1;
            position = Math.abs(position * PARALLAX_OFFSET);
            translationX = (int) (page.getWidth() * direction * position);
        }
        parallaxView.setTranslationX(translationX);
    }

    private View findParallaxView(View page) {
        if (page instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) page;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View v = viewGroup.getChildAt(i);
                if (v.getVisibility() == View.VISIBLE) {
                    return v;
                }
            }
        }
        return null;
    }
}
