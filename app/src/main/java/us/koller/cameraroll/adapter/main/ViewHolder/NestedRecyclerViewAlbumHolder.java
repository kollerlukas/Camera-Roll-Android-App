package us.koller.cameraroll.adapter.main.ViewHolder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.SelectorModeManager;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.FileOperations.FileOperation;
import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.ui.AlbumActivity;
import us.koller.cameraroll.ui.FileOperationDialogActivity;
import us.koller.cameraroll.ui.MainActivity;
import us.koller.cameraroll.ui.ThemeableActivity;
import us.koller.cameraroll.ui.widget.EqualSpacesItemDecoration;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.StorageUtil;
import us.koller.cameraroll.util.Util;
import us.koller.cameraroll.util.animators.ColorFade;

public class NestedRecyclerViewAlbumHolder extends AlbumHolder
        implements Toolbar.OnMenuItemClickListener {

    @SuppressWarnings("FieldCanBeLocal")
    private static int SINGLE_LINE_MAX_ITEM_COUNT = 4;

    public RecyclerView nestedRecyclerView;

    public Album album;

    public int sharedElementReturnPosition = -1;

    private EqualSpacesItemDecoration itemDecoration;

    private SelectorModeManager manager;

    private SelectorModeManager.OnBackPressedCallback onBackPressedCallback
            = new SelectorModeManager.OnBackPressedCallback() {
        @Override
        public void cancelSelectorMode() {
            NestedRecyclerViewAlbumHolder.this.cancelSelectorMode();
        }
    };

    abstract class SelectorCallback implements SelectorModeManager.Callback {
    }

    private SelectorCallback callback
            = new SelectorCallback() {
        @Override
        public void onSelectorModeEnter() {
            final View rootView = ((Activity) nestedRecyclerView.getContext())
                    .findViewById(R.id.root_view);

            final Toolbar toolbar = rootView.findViewById(R.id.toolbar);

            toolbar.setActivated(false);

            if (getContext().getResources().getBoolean(R.bool.colorAccent_dark_icons)) {
                Util.setDarkStatusBarIcons(rootView);
            } else {
                Util.setLightStatusBarIcons(rootView);
            }

            View.OnClickListener onClickListener
                    = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    cancelSelectorMode();
                }
            };

            //create selector-toolbar
            final Toolbar selectorToolbar = SelectorModeUtil.getSelectorModeToolbar(
                    getContext(), onClickListener,
                    NestedRecyclerViewAlbumHolder.this);

            selectorToolbar.setPadding(toolbar.getPaddingLeft(),
                    toolbar.getPaddingTop(),
                    toolbar.getPaddingRight(),
                    toolbar.getPaddingBottom());

            //add selector-toolbar
            ((ViewGroup) toolbar.getParent()).addView(selectorToolbar,
                    toolbar.getLayoutParams());

            selectorToolbar.requestLayout();

            //animate selector-toolbar
            selectorToolbar.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            selectorToolbar.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                            selectorToolbar.setTranslationY(-selectorToolbar.getHeight());
                            selectorToolbar.animate().translationY(0);
                        }
                    });
        }

        @Override
        public void onSelectorModeExit() {
            final View rootView = ((Activity) nestedRecyclerView.getContext())
                    .findViewById(R.id.root_view);

            //find selector-toolbar
            final Toolbar selectorToolbar = rootView
                    .findViewWithTag(SelectorModeUtil.SELECTOR_TOOLBAR_TAG);

            //animate selector-toolbar
            selectorToolbar.animate()
                    .translationY(-selectorToolbar.getHeight())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);

                            Toolbar toolbar = rootView.findViewById(R.id.toolbar);
                            Context c = getContext();
                            if (c instanceof ThemeableActivity) {
                                ThemeableActivity themeableActivity = (ThemeableActivity) c;
                                if (themeableActivity.isLightBaseTheme(ThemeableActivity.THEME)) {
                                    toolbar.setActivated(true);
                                    Util.setDarkStatusBarIcons(rootView);
                                } else {
                                    Util.setLightStatusBarIcons(rootView);
                                }
                            }

                            //remove selector-toolbar
                            ((ViewGroup) rootView).removeView(selectorToolbar);
                        }
                    });
        }

        @Override
        public void onItemSelected(int selectedItemCount) {
            final View rootView = ((Activity) nestedRecyclerView.getContext())
                    .findViewById(R.id.root_view);

            final Toolbar toolbar = rootView
                    .findViewWithTag(SelectorModeUtil.SELECTOR_TOOLBAR_TAG);

            final String title = String.valueOf(selectedItemCount) + (selectedItemCount > 1 ?
                    getContext().getString(R.string.items) : getContext().getString(R.string.item));

            ColorFade.fadeToolbarTitleColor(toolbar,
                    ThemeableActivity.getColorManager().getColor(ThemeableActivity.ColorManager.ACCENT_TEXT_COLOR),
                    new ColorFade.ToolbarTitleFadeCallback() {
                        @Override
                        public void setTitle(Toolbar toolbar) {
                            toolbar.setTitle(title);
                        }
                    });
        }
    };

    public NestedRecyclerViewAlbumHolder(View itemView) {
        super(itemView);

        nestedRecyclerView = itemView.findViewById(R.id.nestedRecyclerView);
        if (nestedRecyclerView != null) {
            itemDecoration = new EqualSpacesItemDecoration(
                    (int) getContext().getResources().getDimension(R.dimen.album_grid_spacing), 2, true);
            nestedRecyclerView.addItemDecoration(itemDecoration);
        }
    }

    public NestedRecyclerViewAlbumHolder setSelectorModeManager(SelectorModeManager manager) {
        this.manager = manager;
        if (!manager.onBackPressedCallbackAlreadySet()) {
            manager.setOnBackPressedCallback(onBackPressedCallback);
        }

        //checking if Callback is already attached, if not attach it
        boolean callbackAttached = false;
        ArrayList<SelectorModeManager.Callback> callbacks = manager.getCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.size(); i++) {
                if (callbacks.get(i) instanceof SelectorCallback) {
                    callbackAttached = true;
                    break;
                }
            }
        }
        if (!callbackAttached) {
            manager.addCallback(callback);
        }
        return this;
    }

    @Override
    public void setAlbum(Album album) {
        super.setAlbum(album);

        this.album = album;

        //album not excluded
        String count = album.getAlbumItems().size()
                + (album.getAlbumItems().size() > 1 ?
                getContext().getString(R.string.items) :
                getContext().getString(R.string.item));
        //noinspection deprecation
        ((TextView) itemView.findViewById(R.id.count)).setText(Html.fromHtml(count));

        int oldHeight = nestedRecyclerView.getHeight();

        //make RecyclerView either single ore double lined, depending on the album size
        int lineCount = album.getAlbumItems().size() > SINGLE_LINE_MAX_ITEM_COUNT ? 2 : 1;
        int height = (int) getContext().getResources()
                .getDimension(R.dimen.nested_recyclerView_line_height) * lineCount;

        if (oldHeight != height) {
            LinearLayout.LayoutParams params
                    = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height);
            nestedRecyclerView.setLayoutParams(params);

            itemDecoration.setSpanCount(lineCount);

            RecyclerView.LayoutManager layoutManager;
            if (album.getAlbumItems().size() > SINGLE_LINE_MAX_ITEM_COUNT) {
                layoutManager = new GridLayoutManager(getContext(), lineCount,
                        GridLayoutManager.HORIZONTAL, false);
            } else {
                layoutManager = new LinearLayoutManager(getContext(),
                        LinearLayoutManager.HORIZONTAL, false);
            }
            nestedRecyclerView.setLayoutManager(layoutManager);
            nestedRecyclerView.setHasFixedSize(true);
        }

        if (nestedRecyclerView.getAdapter() != null) {
            RecyclerViewAdapter adapter = (RecyclerViewAdapter) nestedRecyclerView.getAdapter();
            adapter.setAlbum(album);
            adapter.setSelectorModeManager(manager);
            adapter.notifyDataSetChanged();
        } else {
            RecyclerViewAdapter adapter = new RecyclerViewAdapter(callback,
                    nestedRecyclerView, album, false);
            adapter.setSelectorModeManager(manager);
            nestedRecyclerView.setAdapter(adapter);
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
        nestedRecyclerView.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        nestedRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        nestedRecyclerView.scrollToPosition(sharedElementReturnPosition);
                    }
                });

        nestedRecyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b,
                                       int oL, int oT, int oR, int oB) {
                nestedRecyclerView.removeOnLayoutChangeListener(this);
                callback.startPostponedEnterTransition();
            }
        });
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final String[] paths = ((RecyclerViewAdapter) nestedRecyclerView.getAdapter())
                .cancelSelectorMode((Activity) getContext());

        cancelSelectorMode();

        Activity a;
        if (getContext() instanceof Activity) {
            a = ((Activity) getContext());
        } else {
            Toast.makeText(getContext(), "Error", Toast.LENGTH_SHORT).show();
            return false;
        }
        Intent intent;
        switch (item.getItemId()) {
            case R.id.share:
                //share multiple items
                ArrayList<Uri> uris = new ArrayList<>();
                for (int i = 0; i < paths.length; i++) {
                    uris.add(StorageUtil
                            .getContentUriFromFilePath(getContext(), paths[i]));
                }

                intent = new Intent();
                intent.setAction(Intent.ACTION_SEND_MULTIPLE)
                        .setType(MediaType.getMimeType(getContext(), paths[0]))
                        .putExtra(Intent.EXTRA_STREAM, uris);

                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                    a.startActivity(Intent.createChooser(intent, getContext().getString(R.string.share_photo)));
                }
                break;
            case R.id.copy:
            case R.id.move:
                intent = new Intent(getContext(), FileOperationDialogActivity.class);
                intent.setAction(item.getItemId() == R.id.copy ?
                        FileOperationDialogActivity.ACTION_COPY :
                        FileOperationDialogActivity.ACTION_MOVE);
                intent.putExtra(FileOperationDialogActivity.FILES, paths);

                a.startActivityForResult(intent,
                        AlbumActivity.FILE_OP_DIALOG_REQUEST);
                break;
            case R.id.delete:
                Context c = getContext();

                String title = c.getString(R.string.delete) + " "
                        + String.valueOf(paths.length)
                        + " " + (paths.length > 1 ?
                        c.getString(R.string.files) : c.getString(R.string.file));

                new AlertDialog.Builder(c, MainActivity.getDialogThemeRes())
                        .setTitle(title)
                        .setNegativeButton(c.getString(R.string.no), null)
                        .setPositiveButton(c.getString(R.string.delete), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                deleteItems(paths);
                            }
                        })
                        .create().show();
                break;
        }

        return false;
    }

    private void cancelSelectorMode() {
        //cancel SelectorMode
        if (nestedRecyclerView.getAdapter() instanceof RecyclerViewAdapter) {
            ((RecyclerViewAdapter) nestedRecyclerView.getAdapter())
                    .cancelSelectorMode((Activity) getContext());
        }

        //update other ViewHolders
        final View rootView = ((Activity) nestedRecyclerView.getContext()).findViewById(R.id.root_view);
        View recyclerView = rootView.findViewById(R.id.recyclerView);
        if (recyclerView instanceof RecyclerView) {
            RecyclerView.Adapter adapter = ((RecyclerView) recyclerView).getAdapter();
            adapter.notifyItemRangeChanged(0, adapter.getItemCount() - 1);
        }
    }

    @Override
    public void onItemChanged() {
        nestedRecyclerView.getAdapter().notifyItemRangeChanged(0,
                nestedRecyclerView.getAdapter().getItemCount() - 1);
    }

    private void deleteItems(String[] paths) {
        File_POJO[] filesToDelete = new File_POJO[paths.length];
        for (int i = 0; i < filesToDelete.length; i++) {
            filesToDelete[i] = new File_POJO(paths[i], true);
        }
        getContext().startService(FileOperation
                .getDefaultIntent(getContext(), FileOperation.DELETE, filesToDelete));
    }

    private static class RecyclerViewAdapter
            extends us.koller.cameraroll.adapter.album.RecyclerViewAdapter {

        RecyclerViewAdapter(SelectorModeManager.Callback callback, final RecyclerView recyclerView,
                            Album album, boolean pick_photos) {
            super(callback, recyclerView, album, pick_photos);
        }

        @Override
        public boolean dragSelectEnabled() {
            return false;
        }
    }

    private static class SelectorModeUtil {

        static final String SELECTOR_TOOLBAR_TAG = "SELECTOR_TOOLBAR_TAG";

        static Toolbar getSelectorModeToolbar(Context context,
                                              View.OnClickListener onClickListener,
                                              Toolbar.OnMenuItemClickListener onItemClickListener) {
            final Toolbar toolbar = new Toolbar(context);
            toolbar.setTag(SELECTOR_TOOLBAR_TAG);

            ThemeableActivity.ColorManager colorManager = ThemeableActivity.getColorManager();
            int accentColor = colorManager.getColor(ThemeableActivity.ColorManager.ACCENT_COLOR);
            int accentTextColor = colorManager.getColor(ThemeableActivity.ColorManager.ACCENT_TEXT_COLOR);

            toolbar.setBackgroundColor(accentColor);

            toolbar.setTitleTextColor(accentTextColor);

            toolbar.inflateMenu(R.menu.selector_mode);
            toolbar.setOnMenuItemClickListener(onItemClickListener);

            Drawable menuIcon = toolbar.getOverflowIcon();
            if (menuIcon != null) {
                DrawableCompat.wrap(menuIcon);
                DrawableCompat.setTint(menuIcon.mutate(), accentTextColor);
            }

            Drawable navIcon = ContextCompat.getDrawable(context,
                    R.drawable.ic_clear_black_24dp);
            DrawableCompat.wrap(navIcon);
            DrawableCompat.setTint(navIcon.mutate(), accentTextColor);
            toolbar.setNavigationIcon(navIcon);

            toolbar.setNavigationOnClickListener(onClickListener);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                toolbar.setElevation(context.getResources()
                        .getDimension(R.dimen.toolbar_elevation));
            }
            return toolbar;
        }
    }
}
