package us.koller.cameraroll.ui.widget;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;

public class SquareCardView extends CardView {

    public SquareCardView(Context context) {
        super(context);
    }

    public SquareCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Set a square layout.
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int measureSpec = width < height ? widthMeasureSpec : heightMeasureSpec;
        super.onMeasure(measureSpec, measureSpec);
    }
}
