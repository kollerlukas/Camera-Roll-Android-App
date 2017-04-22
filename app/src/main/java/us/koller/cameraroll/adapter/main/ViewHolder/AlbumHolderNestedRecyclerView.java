package us.koller.cameraroll.adapter.main.ViewHolder;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.SharedElementCallback;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.ui.widget.GridMarginDecoration;

public class AlbumHolderNestedRecyclerView extends AlbumHolder {

    private RecyclerView recyclerView;

    private int sharedElementReturnPosition = -1;

    private final SharedElementCallback mCallback
            = new SharedElementCallback() {
        @Override
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (sharedElementReturnPosition != -1) {
                String newTransitionName = album.getAlbumItems().get(sharedElementReturnPosition).getPath();
                View layout = recyclerView.findViewWithTag(newTransitionName);
                View newSharedElement = layout != null ? layout.findViewById(R.id.image) : null;
                if (newSharedElement != null) {
                    names.clear();
                    names.add(newTransitionName);
                    sharedElements.clear();
                    sharedElements.put(newTransitionName, newSharedElement);
                }
                sharedElementReturnPosition = -1;
            } else {
                View v = itemView.getRootView();
                View navigationBar = v.findViewById(android.R.id.navigationBarBackground);
                View statusBar = v.findViewById(android.R.id.statusBarBackground);
                if (navigationBar != null) {
                    names.add(navigationBar.getTransitionName());
                    sharedElements.put(navigationBar.getTransitionName(), navigationBar);
                }
                if (statusBar != null) {
                    names.add(statusBar.getTransitionName());
                    sharedElements.put(statusBar.getTransitionName(), statusBar);
                }
            }
        }
    };

    public AlbumHolderNestedRecyclerView(View itemView) {
        super(itemView);

        recyclerView = (RecyclerView) itemView.findViewById(R.id.recyclerView);
        if (recyclerView != null) {
            recyclerView.addItemDecoration(new GridMarginDecoration(
                    (int) getContext().getResources().getDimension(R.dimen.album_grid_spacing)));
        }
    }

    @Override
    public void setAlbum(Album album) {
        super.setAlbum(album);

        ((TextView) itemView.findViewById(R.id.name)).setText(album.getName());

        if (!excluded) {
            //album not excluded
            String count = album.getAlbumItems().size()
                    + (album.getAlbumItems().size() > 1 ?
                    getContext().getString(R.string.items) :
                    getContext().getString(R.string.item));
            ((TextView) itemView.findViewById(R.id.count)).setText(Html.fromHtml(count));

            itemView.findViewById(R.id.hidden_folder_indicator)
                    .setVisibility(album.isHidden() ? View.VISIBLE : View.GONE);

            recyclerView.setAdapter(new RecyclerViewAdapter(null,
                    recyclerView, album, false));
            int spanCount = album.getAlbumItems().size() > 10 ? 2 : 1;
            spanCount = 2;
            recyclerView.setLayoutManager(
                    new GridLayoutManager(getContext(), spanCount,
                            GridLayoutManager.HORIZONTAL, false));
        } else {
            //album excluded
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void setSharedElementReturnPosition(int sharedElementReturnPosition) {
        AppCompatActivity a;

        Context c = getContext();
        if (c instanceof AppCompatActivity) {
            a = (AppCompatActivity) c;
        } else {
            return;
        }

        a.setExitSharedElementCallback(mCallback);

        this.sharedElementReturnPosition = sharedElementReturnPosition;

        album.getAlbumItems().get(sharedElementReturnPosition).isSharedElement = true;
        if (recyclerView.getAdapter().getItemCount() > 0) {
            a.postponeEnterTransition();
            final AppCompatActivity a_final = a;
            recyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int l, int t, int r, int b,
                                           int oL, int oT, int oR, int oB) {
                    recyclerView.removeOnLayoutChangeListener(this);
                    a_final.startPostponedEnterTransition();
                }
            });
            recyclerView.scrollToPosition(sharedElementReturnPosition);
        }
    }

    public static class RecyclerViewAdapter
            extends us.koller.cameraroll.adapter.album.RecyclerViewAdapter {

        RecyclerViewAdapter(Callback callback, RecyclerView recyclerView, Album album,
                            boolean pick_photos) {
            super(callback, recyclerView, album, pick_photos);
        }
    }
}
