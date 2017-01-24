package us.koller.cameraroll.ui.widget;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import us.koller.cameraroll.R;

public class ParallaxImageView extends ImageView {

    public static final String RECYCLER_VIEW_TAG = "RECYCLER_VIEW_TAG";

    private final int MAX_PARALLAX_OFFSET = (int) getContext().getResources().getDimension(R.dimen.parallax_image_view_offset);

    private int recyclerView_height = -1;
    private int[] recyclerView_location = {-1, -1};

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

        setParallaxTranslation();

        View view = getRootView().findViewWithTag(RECYCLER_VIEW_TAG);
        if (view instanceof RecyclerView) {
            ((RecyclerView) view).addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    if (viewDetached) {
                        recyclerView.removeOnScrollListener(this);
                        return;
                    }

                    if (recyclerView_height == -1) {
                        recyclerView_height = recyclerView.getHeight();
                        recyclerView.getLocationOnScreen(recyclerView_location);
                    }

                    setParallaxTranslation();
                }
            });
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        viewDetached = true;
    }

    public void setParallaxTranslation() {
        if (recyclerView_height == -1) {
            return;
        }

        int[] location = new int[2];
        getLocationOnScreen(location);

        boolean visible = location[1] + getHeight() > recyclerView_location[1]
                || location[1] < recyclerView_location[1] + recyclerView_height;

        if (!visible) {
            return;
        }

        float dy = (float) (location[1] - recyclerView_location[1]);

        float translationY = MAX_PARALLAX_OFFSET * dy / ((float) recyclerView_height);

        setTranslationY(-translationY);
    }
}
