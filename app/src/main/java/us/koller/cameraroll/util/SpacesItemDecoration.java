package us.koller.cameraroll.util;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

//simple ItemDecoration to get even spacing around items with the GridLayoutManager
public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
    private int space;
    private int spanCount;

    public SpacesItemDecoration(int space, int spanCount) {
        this.space = space;
        this.spanCount = spanCount;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view,
                               RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildLayoutPosition(view);
        int itemCount = parent.getAdapter().getItemCount();

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
        if (position >= itemCount - spanCount) {
            //item is on the bottom edge
            outRect.bottom = space;
        } else {
            outRect.bottom = space / 2;
        }
    }
}
