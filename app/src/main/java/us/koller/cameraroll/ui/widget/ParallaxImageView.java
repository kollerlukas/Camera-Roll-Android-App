package us.koller.cameraroll.ui.widget;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import us.koller.cameraroll.R;

public class ParallaxImageView extends ImageView {

    public static final String RECYCLER_VIEW_TAG = "RECYCLER_VIEW_TAG";

    public ParallaxImageView(Context context) {
        super(context);
    }

    public ParallaxImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ParallaxImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ParallaxImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec
                + (int) getContext().getResources().getDimension(R.dimen.parallax_image_view_offset));
    }

    boolean viewDetached = true;

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        viewDetached = false;

        final int MAX_PARALLAX_OFFSET = (int) getContext().getResources().getDimension(R.dimen.parallax_image_view_offset);

        View view = getRootView().findViewWithTag(RECYCLER_VIEW_TAG);

        if (view instanceof RecyclerView) {
            RecyclerView recyclerView = (RecyclerView) view;
            final int recyclerViewHeight = recyclerView.getHeight();
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    float translationY;
                    if (recyclerViewHeight < 0) {
                        translationY = getTranslationY() - MAX_PARALLAX_OFFSET * (dy / recyclerViewHeight);
                    } else {
                        translationY = getTranslationY() - dy / 5;
                    }

                    if (-translationY > MAX_PARALLAX_OFFSET) {
                        translationY = -MAX_PARALLAX_OFFSET;
                    } else if (translationY > 0) {
                        translationY = 0;
                    }
                    setTranslationY(translationY);

                    if (viewDetached) {
                        recyclerView.removeOnScrollListener(this);
                    }
                }
            });
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        viewDetached = true;
    }
}
