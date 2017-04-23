package us.koller.cameraroll.adapter.main.ViewHolder;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.ui.ThemeableActivity;
import us.koller.cameraroll.ui.widget.GridMarginDecoration;

public class NestedRecyclerViewAlbumHolder extends AlbumHolder {

    private static int SINGLE_LINE_MAX_ITEM_COUNT = 10;

    public RecyclerView recyclerView;

    public Album album;

    public int sharedElementReturnPosition = -1;

    public NestedRecyclerViewAlbumHolder(View itemView) {
        super(itemView);

        recyclerView = (RecyclerView) itemView.findViewById(R.id.recyclerView);
        if (recyclerView != null) {
            recyclerView.addItemDecoration(new GridMarginDecoration(
                    (int) getContext().getResources().getDimension(R.dimen.album_grid_spacing)));
        }

        ((TextView) itemView.findViewById(R.id.name))
                .setTextColor(ContextCompat.getColor(getContext(),
                        ThemeableActivity.text_color_res));

        ((TextView) itemView.findViewById(R.id.count))
                .setTextColor(ContextCompat.getColor(getContext(),
                        ThemeableActivity.text_color_secondary_res));
    }

    @Override
    public void setAlbum(Album album) {
        super.setAlbum(album);

        this.album = album;

        if (!excluded) {
            //album not excluded
            String count = album.getAlbumItems().size()
                    + (album.getAlbumItems().size() > 1 ?
                    getContext().getString(R.string.items) :
                    getContext().getString(R.string.item));
            ((TextView) itemView.findViewById(R.id.count)).setText(Html.fromHtml(count));

            itemView.findViewById(R.id.hidden_folder_indicator)
                    .setVisibility(album.isHidden() ? View.VISIBLE : View.GONE);

            //make RecyclerView either single ore double lined, depending on the album size
            int lineCount = album.getAlbumItems().size() > SINGLE_LINE_MAX_ITEM_COUNT ? 2 : 1;
            int lineHeight = (int) getContext().getResources()
                    .getDimension(R.dimen.nested_recyclerView_line_height) * lineCount;

            LinearLayout.LayoutParams params
                    = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, lineHeight);
            recyclerView.setLayoutParams(params);

            RecyclerView.LayoutManager manager;
            if (album.getAlbumItems().size() > SINGLE_LINE_MAX_ITEM_COUNT) {
                manager = new GridLayoutManager(getContext(), lineCount,
                        GridLayoutManager.HORIZONTAL, false);
            } else {
                manager = new LinearLayoutManager(getContext(),
                        LinearLayoutManager.HORIZONTAL, false);
            }
            recyclerView.setLayoutManager(manager);

            recyclerView.setAdapter(new RecyclerViewAdapter(null,
                    recyclerView, album, false));
        } else {
            //album excluded
        }
    }

    public interface StartSharedElementTransitionCallback {
        void startPostponedEnterTransition();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onSharedElement(final int sharedElementReturnPosition,
                                final StartSharedElementTransitionCallback callback) {
        this.sharedElementReturnPosition = sharedElementReturnPosition;

        //to prevent: requestLayout() improperly called [...] during layout: running second layout pass
        recyclerView.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        recyclerView.scrollToPosition(sharedElementReturnPosition);
                    }
                });

        recyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b,
                                       int oL, int oT, int oR, int oB) {
                recyclerView.removeOnLayoutChangeListener(this);
                callback.startPostponedEnterTransition();
            }
        });
    }

    static class RecyclerViewAdapter
            extends us.koller.cameraroll.adapter.album.RecyclerViewAdapter {

        RecyclerViewAdapter(Callback callback, RecyclerView recyclerView, Album album,
                            boolean pick_photos) {
            super(callback, recyclerView, album, pick_photos);
        }
    }
}
