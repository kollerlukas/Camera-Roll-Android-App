package us.koller.cameraroll.ui;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.TransitionSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import java.io.File;
import java.util.List;
import java.util.Map;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.album.RecyclerViewAdapter;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.MediaLoader.MediaLoader;
import us.koller.cameraroll.data.Video;
import us.koller.cameraroll.ui.widget.GridMarginDecoration;
import us.koller.cameraroll.ui.widget.SwipeBackCoordinatorLayout;
import us.koller.cameraroll.util.ColorFade;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.Util;

public class AlbumActivity extends AppCompatActivity implements SwipeBackCoordinatorLayout.OnSwipeListener, RecyclerViewAdapter.Callback {

    public static final String ALBUM = "ALBUM";
    public static final String VIEW_ALBUM = "VIEW_ALBUM";
    public static final String DELETE_ALBUMITEM = "DELETE_ALBUMITEM";
    public static final String EXTRA_CURRENT_ALBUM_POSITION = "EXTRA_CURRENT_ALBUM_POSITION";
    public static final String RECYCLER_VIEW_SCROLL_STATE = "RECYCLER_VIEW_SCROLL_STATE";

    private int sharedElementReturnPosition = -1;

    private final SharedElementCallback mCallback = new SharedElementCallback() {
        @Override
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
                View navigationBar = findViewById(android.R.id.navigationBarBackground);
                View statusBar = findViewById(android.R.id.statusBarBackground);
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

    private Album album;

    private RecyclerView recyclerView;

    private Snackbar snackbar;

    private boolean refreshMainActivityAfterItemWasDeleted = false;

    private boolean pick_photos;
    private boolean allowMultiple;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        pick_photos = getIntent().getAction() != null
                && getIntent().getAction().equals(MainActivity.PICK_PHOTOS);
        allowMultiple = getIntent().getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        if (pick_photos) {
            Util.setDarkStatusBarIcons(findViewById(R.id.root_view));
        }

        MediaLoader.checkPermission(this);

        setExitSharedElementCallback(mCallback);
        getWindow().setEnterTransition(new TransitionSet()
                .setOrdering(TransitionSet.ORDERING_TOGETHER)
                .addTransition(new Slide(Gravity.BOTTOM))
                .addTransition(new Fade())
                .setInterpolator(new AccelerateDecelerateInterpolator()));
        getWindow().setReturnTransition(new TransitionSet()
                .setOrdering(TransitionSet.ORDERING_TOGETHER)
                .addTransition(new Slide(Gravity.BOTTOM))
                .addTransition(new Fade())
                .setInterpolator(new AccelerateDecelerateInterpolator()));

        if (savedInstanceState != null && savedInstanceState.containsKey(ALBUM)) {
            album = savedInstanceState.getParcelable(ALBUM);
        } else {
            album = getIntent().getExtras().getParcelable(ALBUM);
        }

        if (album == null) {
            return;
        }

        final ViewGroup rootView = (ViewGroup) findViewById(R.id.root_view);
        if (rootView instanceof SwipeBackCoordinatorLayout) {
            ((SwipeBackCoordinatorLayout) rootView).setOnSwipeListener(this);
        }

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.black_translucent2));
        if (!pick_photos) {
            toolbar.setNavigationIcon(AnimatedVectorDrawableCompat.create(this, R.drawable.back_to_cancel_animateable));
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_clear_black_24dp);
            toolbar.getNavigationIcon().setTint(ContextCompat.getColor(this, R.color.grey_900_translucent));
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (((RecyclerViewAdapter) recyclerView.getAdapter()).isSelectorModeActive()) {
                    ((RecyclerViewAdapter) recyclerView.getAdapter()).cancelSelectorMode();
                } else {
                    onBackPressed();
                }
            }
        });

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        final int columnCount = Util.getAlbumActivityGridColumnCount(this);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, columnCount);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.addItemDecoration(new GridMarginDecoration(
                (int) getResources().getDimension(R.dimen.grid_spacing)));
        recyclerView.setAdapter(new RecyclerViewAdapter(this, recyclerView, album, pick_photos));
        if (savedInstanceState != null
                && savedInstanceState.containsKey(RECYCLER_VIEW_SCROLL_STATE)) {
            recyclerView.getLayoutManager().onRestoreInstanceState(
                    savedInstanceState.getParcelable(RECYCLER_VIEW_SCROLL_STATE));
        }

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (((RecyclerViewAdapter) recyclerView.getAdapter()).isSelectorModeActive()
                        || pick_photos) {
                    return;
                }

                float translationY = toolbar.getTranslationY() - dy;
                if (-translationY > toolbar.getHeight()) {
                    translationY = -toolbar.getHeight();
                } else if (translationY > 0) {
                    translationY = 0;
                }
                toolbar.setTranslationY(translationY);
            }
        });

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (!pick_photos) {
            fab.setImageDrawable(AnimatedVectorDrawableCompat.create(this, R.drawable.ic_delete_vector_animateable));
        } else {
            fab.setImageResource(R.drawable.ic_send_white_24dp);
        }
        Drawable d = fab.getDrawable();
        d.setTint(ContextCompat.getColor(this, R.color.grey_900_translucent));
        fab.setImageDrawable(d);
        fab.setScaleX(0.0f);
        fab.setScaleY(0.0f);

        rootView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                toolbar.setPadding(toolbar.getPaddingStart(),
                        toolbar.getPaddingTop() + insets.getSystemWindowInsetTop(),
                        toolbar.getPaddingEnd(),
                        toolbar.getPaddingBottom());

                ViewGroup.MarginLayoutParams toolbarParams
                        = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
                toolbarParams.leftMargin += insets.getSystemWindowInsetLeft();
                toolbarParams.rightMargin += insets.getSystemWindowInsetRight();
                toolbar.setLayoutParams(toolbarParams);

                recyclerView.setPadding(recyclerView.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                        recyclerView.getPaddingTop() /*+ insets.getSystemWindowInsetTop()*/,
                        recyclerView.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                        recyclerView.getPaddingBottom() + insets.getSystemWindowInsetBottom());

                ViewGroup.MarginLayoutParams fabParams
                        = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
                fabParams.rightMargin += insets.getSystemWindowInsetRight();
                fabParams.bottomMargin += insets.getSystemWindowInsetBottom();
                fab.setLayoutParams(fabParams);

                rootView.setOnApplyWindowInsetsListener(null);
                return insets.consumeSystemWindowInsets();
            }
        });

        toolbar.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                recyclerView.setPadding(recyclerView.getPaddingStart(),
                        recyclerView.getPaddingTop() + toolbar.getHeight(),
                        recyclerView.getPaddingEnd(),
                        recyclerView.getPaddingBottom());

                recyclerView.scrollBy(0, -toolbar.getHeight());

                toolbar.getViewTreeObserver().removeOnPreDrawListener(this);
                return false;
            }
        });

        setupTaskDescription();

        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction().equals(DELETE_ALBUMITEM)) {
            final AlbumItem albumItem = intent.getParcelableExtra(ItemActivity.ALBUM_ITEM);
            deleteAlbumItemSnackbar(albumItem, intent.getBooleanExtra(ItemActivity.VIEW_ONLY, false));
        }
    }

    @Override
    public void onActivityReenter(int requestCode, Intent data) {
        super.onActivityReenter(requestCode, data);
        Bundle tmpReenterState = new Bundle(data.getExtras());
        sharedElementReturnPosition = tmpReenterState.getInt(EXTRA_CURRENT_ALBUM_POSITION);
        album.getAlbumItems().get(sharedElementReturnPosition).isSharedElement = true;
        if (recyclerView.getAdapter().getItemCount() > 0) {
            postponeEnterTransition();
            recyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int l, int t, int r, int b,
                                           int oL, int oT, int oR, int oB) {
                    recyclerView.removeOnLayoutChangeListener(this);
                    startPostponedEnterTransition();
                }
            });
            recyclerView.scrollToPosition(sharedElementReturnPosition);
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(!pick_photos ? album.getName() :
                (allowMultiple ? getString(R.string.pick_photo) : getString(R.string.pick_photos)));
        if (getResources().getBoolean(R.bool.landscape)) {
            setSystemUiFlags();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MediaLoader.PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission granted
                    if (snackbar != null) {
                        snackbar.dismiss();
                    }
                    recyclerView.getAdapter().notifyDataSetChanged();
                } else {
                    // permission denied
                    Snackbar snackbar = Util.getPermissionDeniedSnackbar(findViewById(R.id.root_view));
                    snackbar.setAction(R.string.retry, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            MediaLoader.checkPermission(AlbumActivity.this);
                        }
                    });
                    Util.showSnackbar(snackbar);
                }
            }
        }
    }

    public void deleteAlbumItemSnackbar(final AlbumItem albumItem, boolean VIEW_ONLY) {
        if (!MediaLoader.checkPermission(this)) {
            return;
        }

        int k = album.getAlbumItems().indexOf(albumItem);
        for (int i = 0; i < album.getAlbumItems().size(); i++) {
            if (album.getAlbumItems().get(i).getPath().equals(albumItem.getPath())) {
                k = i;
                break;
            }
        }
        final int index = k;
        album.getAlbumItems().remove(index);
        recyclerView.getAdapter().notifyDataSetChanged();

        snackbar = Snackbar.make(findViewById(R.id.root_view), R.string.photo_deleted, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        album.getAlbumItems().add(index, albumItem);
                        recyclerView.getAdapter().notifyItemInserted(index);
                    }
                })
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                            AlbumItem[] albumItems = {albumItem};
                            int[] indices = {index};
                            deleteAlbumItems(albumItems, indices);
                        }
                    }
                });
        Util.showSnackbar(snackbar);

        if (VIEW_ONLY) {
            snackbar.dismiss();
            this.finish();
        }
    }

    public void deleteAlbumItemsSnackbar() {
        if (!MediaLoader.checkPermission(this)) {
            return;
        }

        final AlbumItem[] selected_items = ((RecyclerViewAdapter) recyclerView.getAdapter()).cancelSelectorMode();
        final int[] indices = new int[selected_items.length];
        for (int i = 0; i < selected_items.length; i++) {
            AlbumItem albumItem = selected_items[i];
            indices[i] = album.getAlbumItems().indexOf(albumItem);
            album.getAlbumItems().remove(albumItem);
            recyclerView.getAdapter().notifyItemRemoved(indices[i]);
        }

        String message = selected_items.length == 1 ?
                selected_items.length + " " + getString(R.string.photo_deleted) :
                selected_items.length + " " + getString(R.string.photos_deleted);

        snackbar = Snackbar.make(findViewById(R.id.root_view), message, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        for (int i = 0; i < selected_items.length; i++) {
                            AlbumItem albumItem = selected_items[i];
                            int index = indices[i];
                            album.getAlbumItems().add(index, albumItem);
                            recyclerView.getAdapter().notifyItemInserted(index);
                        }
                    }
                })
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                            deleteAlbumItems(selected_items, indices);
                        }
                    }
                });
        Util.showSnackbar(snackbar);
    }

    public void deleteAlbumItems(AlbumItem[] selected_items, int[] indices) {
        int successfully_deleted = 0;
        for (int i = 0; i < selected_items.length; i++) {
            boolean success;
            if (!album.hiddenAlbum) {
                int result = getContentResolver().delete(MediaStore.Files.getContentUri("external"),
                        MediaStore.Files.FileColumns.DATA + "=?", new String[]{selected_items[i].getPath()});
                success = result > 0;
            } else {
                File file = new File(selected_items[i].getPath());
                success = file.delete();
            }

            if (!success) {
                album.getAlbumItems().add(indices[i], selected_items[i]);
                recyclerView.getAdapter().notifyItemInserted(indices[i]);
            } else {
                successfully_deleted++;

                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.parse(selected_items[i].getPath())));
            }
        }

        if (refreshMainActivityAfterItemWasDeleted) {
            setResult(RESULT_OK, new Intent(MainActivity.REFRESH_MEDIA));
            onBackPressed();
        }

        Toast.makeText(AlbumActivity.this, getString(R.string.successfully_deleted) + successfully_deleted + " / " + selected_items.length, Toast.LENGTH_SHORT).show();
    }

    public void setPhotosResult() {
        final AlbumItem[] selected_items
                = ((RecyclerViewAdapter) recyclerView.getAdapter()).cancelSelectorMode();

        String[] mimeTypes = new String[selected_items.length];
        for (int i = 0; i < selected_items.length; i++) {
            mimeTypes[i] = MediaType.getMimeType(this, selected_items[i].getPath());
        }

        ClipData clipData =
                new ClipData("Images", mimeTypes,
                        new ClipData.Item(selected_items[0].getUri(this)));
        for (int i = 1; i < selected_items.length; i++) {
            clipData.addItem(new ClipData.Item(selected_items[i].getUri(this)));
        }

        Intent intent = new Intent("us.koller.RESULT_ACTION");
        intent.setClipData(clipData);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onSelectorModeEnter() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(ContextCompat.getColor(AlbumActivity.this, android.R.color.transparent));
        toolbar.setActivated(true);
        toolbar.animate().translationY(0.0f).start();

        Util.setDarkStatusBarIcons(findViewById(R.id.root_view));

        if (!pick_photos) {
            ColorFade.fadeBackgroundColor(toolbar,
                    ContextCompat.getColor(this, R.color.black_translucent2),
                    ContextCompat.getColor(this, R.color.colorAccent));

            ((Animatable) toolbar.getNavigationIcon()).start();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    toolbar.setNavigationIcon(AnimatedVectorDrawableCompat
                            .create(AlbumActivity.this, R.drawable.cancel_to_back_vector_animateable));
                    toolbar.setTitleTextColor(ContextCompat.getColor(AlbumActivity.this, R.color.grey_900_translucent));
                }
            }, 300);
        } else {
            toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent));
            toolbar.setTitleTextColor(ContextCompat.getColor(AlbumActivity.this, R.color.grey_900_translucent));
        }

        if (!pick_photos) {
            animateFab(true, false);
        }
    }

    @Override
    public void onSelectorModeExit() {
        if (pick_photos) {
            return;
        }

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(ContextCompat.getColor(AlbumActivity.this, android.R.color.transparent));
        toolbar.setActivated(false);
        ColorFade.fadeBackgroundColor(toolbar,
                ContextCompat.getColor(this, R.color.colorAccent),
                ContextCompat.getColor(this, R.color.black_translucent2));
        toolbar.setTitle(album.getName());

        ((Animatable) toolbar.getNavigationIcon()).start();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                toolbar.setNavigationIcon(AnimatedVectorDrawableCompat
                        .create(AlbumActivity.this, R.drawable.back_to_cancel_animateable));
                toolbar.setTitleTextColor(ContextCompat.getColor(AlbumActivity.this, R.color.white));

                Util.setLightStatusBarIcons(findViewById(R.id.root_view));
            }
        }, 300);

        animateFab(false, false);
    }

    @Override
    public void onItemSelected(int selectedItemCount) {
        String title = String.valueOf(selectedItemCount) + (selectedItemCount > 1 ?
                getString(R.string.items) : getString(R.string.item));
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(title);

        if (pick_photos) {
            if (selectedItemCount > 0) {
                animateFab(true, false);
            } else {
                animateFab(false, false);
            }
        }
    }

    public static void videoOnClick(Activity context, AlbumItem albumItem) {
        if (!(albumItem instanceof Video)) {
            return;
        }

        File file = new File(albumItem.getPath());
        Uri uri = FileProvider.getUriForFile(context,
                context.getApplicationContext().getPackageName() + ".provider", file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "video/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "No App found to play your video", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void fabClicked() {
        animateFab(false, true);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!pick_photos) {
                    deleteAlbumItemsSnackbar();
                } else {
                    setPhotosResult();
                }
            }
        }, 400);
    }

    public void animateFab(final boolean show, boolean click) {
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        if ((fab.getScaleX() == 1.0f && show)
                || (fab.getScaleX() == 0.0f && !show)) {
            return;
        }

        if (show) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fabClicked();
                }
            });
        } else {
            fab.setOnClickListener(null);
        }
        if (click) {
            Drawable drawable = fab.getDrawable();
            if (drawable instanceof Animatable) {
                ((Animatable) drawable).start();
            }
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                fab.animate()
                        .scaleX(show ? 1.0f : 0.0f)
                        .scaleY(show ? 1.0f : 0.0f)
                        .alpha(show ? 1.0f : 0.0f)
                        .setDuration(250)
                        .start();
            }
        }, click ? 400 : 0);
    }

    private void setSystemUiFlags() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    @Override
    public void onBackPressed() {
        if (((RecyclerViewAdapter) recyclerView.getAdapter()).onBackPressed()) {
            animateFab(false, false);
        } else if (snackbar != null) {
            snackbar.dismiss();
            snackbar = null;
            refreshMainActivityAfterItemWasDeleted = true;
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(ALBUM, album);
        outState.putParcelable(RECYCLER_VIEW_SCROLL_STATE,
                recyclerView.getLayoutManager().onSaveInstanceState());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

    }

    private void setupTaskDescription() {
        Bitmap overviewIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.app_name),
                overviewIcon,
                ContextCompat.getColor(this, R.color.colorAccent)));
        overviewIcon.recycle();
    }

    @Override
    public boolean canSwipeBack(int dir) {
        return SwipeBackCoordinatorLayout.canSwipeBackForThisView(recyclerView, dir) && !pick_photos;
    }

    @Override
    public void onSwipeProcess(float percent) {
        ViewGroup viewGroup = (ViewGroup) findViewById(R.id.root_view);
        viewGroup.setAlpha(1 - percent + 0.7f);
    }

    @Override
    public void onSwipeFinish(int dir) {
        if (((RecyclerViewAdapter) recyclerView.getAdapter()).isSelectorModeActive()) {
            ((RecyclerViewAdapter) recyclerView.getAdapter()).cancelSelectorMode();
        }
        getWindow().setReturnTransition(new TransitionSet()
                .setOrdering(TransitionSet.ORDERING_TOGETHER)
                .addTransition(new Slide(dir > 0 ? Gravity.TOP : Gravity.BOTTOM))
                .addTransition(new Fade())
                .setInterpolator(new AccelerateDecelerateInterpolator()));
        onBackPressed();
    }
}
