package us.koller.cameraroll.ui.widget;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

//simple ItemDecoration to get even spacing around items with the GridLayoutManager
public class EqualSpacesItemDecoration extends RecyclerView.ItemDecoration {
    private int space;
    private int spanCount; // lineCount for horizontal

    private boolean horizontal;

    public EqualSpacesItemDecoration(int space, int spanCount, boolean horizontal) {
        this.space = space;
        this.spanCount = spanCount;
        this.horizontal = horizontal;
    }

    public void setSpanCount(int spanCount) {
        this.spanCount = spanCount;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view,
                               RecyclerView parent, RecyclerView.State state) {
        int itemCount = parent.getAdapter().getItemCount();
        int position = parent.getChildLayoutPosition(view);

        if (!horizontal) {
            //left spacing
            if (position % spanCount == 0) {
                //item is on the left edge
                outRect.left = space;
            } else {
                outRect.left = space / 2;
            }

            //top spacing
            if (position < spanCount) {
                //item is on the top edge
                outRect.top = space;
            } else {
                outRect.top = space / 2;
            }

            //right spacing
            if ((position + 1) % spanCount == 0) {
                //item is on the right edge
                outRect.right = space;
            } else {
                outRect.right = space / 2;
            }

            //bottom spacing
            if (position > itemCount - spanCount) {
                //item is on the right edge
                outRect.bottom = space;
            } else {
                outRect.bottom = space / 2;
            }

        } else {
            int lineCount = spanCount;

            //left spacing
            if (position < lineCount) {
                //item is on the left edge
                outRect.left = space;
            } else {
                outRect.left = space / 2;
            }

            //top spacing
            if (position % lineCount == 0) {
                //item is on the top edge
                outRect.top = space;
            } else {
                outRect.top = space / 2;
            }

            //right spacing
            if (position > itemCount - lineCount) {
                //item is on the top edge
                outRect.right = space;
            } else {
                outRect.right = space / 2;
            }

            //bottom spacing
            if ((position + 1) % lineCount == 0) {
                outRect.bottom = space;
            } else {
                outRect.bottom = space / 2;
            }
        }
    }
}
