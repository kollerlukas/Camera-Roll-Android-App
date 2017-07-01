package us.koller.cameraroll.ui.widget;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;

import com.pluscubed.recyclerfastscroll.RecyclerFastScroller;

import us.koller.cameraroll.R;

public class FastScrollerRecyclerView extends RecyclerView {

    private RecyclerFastScroller fastScroller;

    public FastScrollerRecyclerView(Context context) {
        super(context);
    }

    public FastScrollerRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FastScrollerRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        ViewParent parent = getParent();
        if (parent instanceof View) {
            View parentView = (View) parent;
            fastScroller = parentView.findViewById(R.id.fastScroller);
            if (fastScroller != null) {
                fastScroller.attachRecyclerView(this);
            }
        }
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);

        //handle fastScroller padding
        if (fastScroller != null) {
            fastScroller.setPadding(
                    fastScroller.getPaddingStart(), getPaddingTop(),
                    fastScroller.getPaddingEnd(), getPaddingBottom());

            //pass padding top to Handle as translationY
            View mHandle = fastScroller.getChildAt(1);
            if (mHandle != null) {
                mHandle.setTranslationY(fastScroller.getPaddingTop());
            }

            fastScroller.requestLayout();
        }
    }
}
