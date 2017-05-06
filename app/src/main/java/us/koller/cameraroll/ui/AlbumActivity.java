package us.koller.cameraroll.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.TransitionSet;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.SelectorModeManager;
import us.koller.cameraroll.adapter.album.RecyclerViewAdapter;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.FileOperations.Delete;
import us.koller.cameraroll.data.FileOperations.FileOperation;
import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.data.Provider.MediaProvider;
import us.koller.cameraroll.data.Provider.Provider;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.ui.widget.GridMarginDecoration;
import us.koller.cameraroll.ui.widget.SwipeBackCoordinatorLayout;
import us.koller.cameraroll.util.SortUtil;
import us.koller.cameraroll.util.StorageUtil;
import us.koller.cameraroll.util.animators.ColorFade;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.Util;

public class AlbumActivity extends ThemeableActivity
        implements SwipeBackCoordinatorLayout.OnSwipeListener, RecyclerViewAdapter.Callback {

    public static final int FILE_OP_DIALOG_REQUEST = 1;

    public static final String ALBUM = "ALBUM";
    public static final String ALBUM_PATH = "ALBUM_PATH";
    public static final String VIEW_ALBUM = "VIEW_ALBUM";
    public static final String ALBUM_ITEM_DELETED = "ALBUM_ITEM_DELETED";
    public static final String EXTRA_CURRENT_ALBUM_POSITION = "EXTRA_CURRENT_ALBUM_POSITION";
    public static final String RECYCLER_VIEW_SCROLL_STATE = "RECYCLER_VIEW_STATE";

    private int sharedElementReturnPosition = -1;

    private final SharedElementCallback mCallback = new SharedElementCallback() {
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

    private Menu menu;

    //to refresh MainActivity, when deleting of items is done
    private boolean refreshMainActivityAfterItemWasDeleted = false;
    //to refresh MainActivity, when exclude-Flag changed
    private boolean refreshMainActivityWhenClosed = false;

    private boolean pick_photos;
    private boolean allowMultiple;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        pick_photos = getIntent().getAction() != null
                && getIntent().getAction().equals(MainActivity.PICK_PHOTOS);
        allowMultiple = getIntent().getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);

        MediaProvider.checkPermission(this);

        setExitSharedElementCallback(mCallback);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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
        }

        String path;
        if (savedInstanceState != null && savedInstanceState.containsKey(ALBUM_PATH)) {
            path = savedInstanceState.getString(ALBUM_PATH);
        } else {
            path = getIntent().getStringExtra(ALBUM_PATH);
        }
        album = MediaProvider.loadAlbum(path);

        if (album == null) {
            return;
        }

        int sort_by = Settings.getInstance(this).sortAlbumBy();
        SortUtil.sort(this, album.getAlbumItems(), sort_by);

        final ViewGroup swipeBackView = (ViewGroup) findViewById(R.id.swipeBackView);
        if (swipeBackView instanceof SwipeBackCoordinatorLayout) {
            ((SwipeBackCoordinatorLayout) swipeBackView).setOnSwipeListener(this);
        }

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (!pick_photos) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AnimatedVectorDrawable drawable = (AnimatedVectorDrawable)
                        ContextCompat.getDrawable(AlbumActivity.this, R.drawable.back_to_cancel_avd);
                //mutating avd to reset it
                drawable.mutate();
                toolbar.setNavigationIcon(drawable);
            } else {
                toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
            }
            Drawable navIcon = toolbar.getNavigationIcon();
            if (navIcon != null) {
                navIcon = DrawableCompat.wrap(navIcon);
                DrawableCompat.setTint(navIcon.mutate(),
                        ContextCompat.getColor(this, text_color_secondary_res));
                toolbar.setNavigationIcon(navIcon);
            }
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_clear_black_24dp);
            Drawable navIcon = toolbar.getNavigationIcon();
            if (navIcon != null) {
                navIcon = DrawableCompat.wrap(navIcon);
                DrawableCompat.setTint(navIcon.mutate(),
                        ContextCompat.getColor(this, R.color.grey_900_translucent));
                toolbar.setNavigationIcon(navIcon);
            }
            Util.setDarkStatusBarIcons(findViewById(R.id.root_view));

            Util.colorToolbarOverflowMenuIcon(toolbar,
                    ContextCompat.getColor(this, R.color.grey_900_translucent));
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
        final int columnCount = Settings.getInstance(this).getColumnCount(this);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, columnCount);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.addItemDecoration(new GridMarginDecoration((int)
                getResources().getDimension(R.dimen.album_grid_spacing)));
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

                float translationY = toolbar.getTranslationY() - dy * 0.5f;
                if (-translationY > toolbar.getHeight()) {
                    translationY = -toolbar.getHeight();
                } else if (translationY > 0) {
                    translationY = 0;
                }
                toolbar.setTranslationY(translationY);

                //animate statusBarIcon color
                if (THEME == LIGHT) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        float animatedValue = (-translationY) / toolbar.getHeight();
                        if (animatedValue > 0.9f) {
                            Util.setLightStatusBarIcons(findViewById(R.id.root_view));
                        } else {
                            Util.setDarkStatusBarIcons(findViewById(R.id.root_view));
                        }
                    }
                }
            }
        });

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (!pick_photos) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Drawable d = ContextCompat.getDrawable(this,
                        R.drawable.ic_delete_avd);
                fab.setImageDrawable(d);
            } else {
                fab.setImageResource(R.drawable.ic_delete_white_24dp);
            }
        } else {
            fab.setImageResource(R.drawable.ic_send_white_24dp);
        }
        Drawable d = fab.getDrawable();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            d.setTint(ContextCompat.getColor(this, R.color.grey_900_translucent));
        } else {
            d = DrawableCompat.wrap(d);
            DrawableCompat.setTint(d.mutate(),
                    ContextCompat.getColor(this, R.color.grey_900_translucent));
        }
        fab.setImageDrawable(d);
        fab.setScaleX(0.0f);
        fab.setScaleY(0.0f);

        final ViewGroup rootView = (ViewGroup) findViewById(R.id.root_view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            rootView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
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
        } else {
            rootView.getViewTreeObserver()
                    .addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    // hacky way of getting window insets on pre-Lollipop
                                    // somewhat works...
                                    int[] screenSize = Util.getScreenSize(AlbumActivity.this);

                                    int[] windowInsets = new int[]{
                                            Math.abs(screenSize[0] - rootView.getLeft()),
                                            Math.abs(screenSize[1] - rootView.getTop()),
                                            Math.abs(screenSize[2] - rootView.getRight()),
                                            Math.abs(screenSize[3] - rootView.getBottom())};

                                    toolbar.setPadding(toolbar.getPaddingStart(),
                                            toolbar.getPaddingTop() + windowInsets[1],
                                            toolbar.getPaddingEnd(),
                                            toolbar.getPaddingBottom());

                                    ViewGroup.MarginLayoutParams toolbarParams
                                            = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
                                    toolbarParams.leftMargin += windowInsets[0];
                                    toolbarParams.rightMargin += windowInsets[2];
                                    toolbar.setLayoutParams(toolbarParams);

                                    recyclerView.setPadding(recyclerView.getPaddingStart() + windowInsets[0],
                                            recyclerView.getPaddingTop() + windowInsets[1],
                                            recyclerView.getPaddingEnd() + windowInsets[2],
                                            recyclerView.getPaddingBottom() + windowInsets[3]);
                                    recyclerView.scrollToPosition(0);

                                    ViewGroup.MarginLayoutParams fabParams
                                            = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
                                    fabParams.rightMargin += windowInsets[2];
                                    fabParams.bottomMargin += windowInsets[3];
                                    fab.setLayoutParams(fabParams);

                                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                }
                            });
        }

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

        onNewIntent(getIntent());

        //restore Selector mode, when needed
        if (savedInstanceState != null) {
            RecyclerViewAdapter adapter = ((RecyclerViewAdapter) recyclerView.getAdapter());
            SelectorModeManager manager = new SelectorModeManager(savedInstanceState);
            adapter.setSelectorModeManager(manager);
            if (manager.isSelectorModeActive()) {
                adapter.restoreSelectedItems();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction().equals(ALBUM_ITEM_DELETED)) {
            final AlbumItem albumItem = intent.getParcelableExtra(ItemActivity.ALBUM_ITEM);

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

            refreshMainActivityWhenClosed = true;
        }
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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

            /*Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            toolbar.setTranslationY(0);*/
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.album, menu);
        this.menu = menu;

        if (!pick_photos) {
            //setup exclude checkbox
            boolean enabled = !Provider
                    .isDirExcludedBecauseParentDirIsExcluded(album.getPath(),
                            Provider.getExcludedPaths());
            menu.findItem(R.id.exclude).setEnabled(enabled);
            menu.findItem(R.id.exclude).setChecked(album.excluded || !enabled);

            if (recyclerView.getAdapter() instanceof RecyclerViewAdapter &&
                    ((RecyclerViewAdapter) recyclerView.getAdapter()).isSelectorModeActive()) {
                handleMenuVisibilityForSelectorMode(true);
            }
        } else {
            menu.findItem(R.id.share).setVisible(false);
            menu.findItem(R.id.exclude).setVisible(false);
            menu.findItem(R.id.copy).setVisible(false);
            menu.findItem(R.id.move).setVisible(false);
        }

        int sort_by = Settings.getInstance(this).sortAlbumBy();
        if (sort_by == SortUtil.BY_DATE) {
            menu.findItem(R.id.sort_by_date).setChecked(true);
        } else if (sort_by == SortUtil.BY_NAME) {
            menu.findItem(R.id.sort_by_name).setChecked(true);
        }

        return super.onCreateOptionsMenu(menu);
    }

    public void handleMenuVisibilityForSelectorMode(boolean selectorModeActive) {
        if (menu != null) {
            menu.findItem(R.id.exclude).setVisible(!selectorModeActive);
            menu.findItem(R.id.sort_by).setVisible(!selectorModeActive);
            //show share button
            menu.findItem(R.id.share).setVisible(selectorModeActive);
            //show copy & move button
            menu.findItem(R.id.copy).setVisible(selectorModeActive);
            menu.findItem(R.id.move).setVisible(selectorModeActive);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final String[] selected_items_paths;
        Intent intent;
        switch (item.getItemId()) {
            case R.id.share:
                //share multiple items
                selected_items_paths =
                        ((RecyclerViewAdapter) recyclerView.getAdapter())
                                .cancelSelectorMode();

                ArrayList<Uri> uris = new ArrayList<>();
                for (int i = 0; i < selected_items_paths.length; i++) {
                    uris.add(StorageUtil.getContentUriFromFilePath(
                            this, selected_items_paths[i]));
                }

                intent = new Intent();
                intent.setAction(Intent.ACTION_SEND_MULTIPLE)
                        .setType(MediaType.getMimeType(this, selected_items_paths[0]))
                        .putExtra(Intent.EXTRA_STREAM, uris);

                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(Intent.createChooser(intent, getString(R.string.share_photo)));
                }
                break;
            case R.id.copy:
            case R.id.move:
                selected_items_paths =
                        ((RecyclerViewAdapter) recyclerView.getAdapter())
                                .cancelSelectorMode();

                intent = new Intent(this, FileOperationDialogActivity.class);
                intent.setAction(item.getItemId() == R.id.copy ?
                        FileOperationDialogActivity.ACTION_COPY :
                        FileOperationDialogActivity.ACTION_MOVE);
                intent.putExtra(FileOperationDialogActivity.FILES, selected_items_paths);

                startActivityForResult(intent, FILE_OP_DIALOG_REQUEST);
                break;
            case R.id.exclude:
                Provider.loadExcludedPaths(this);
                if (!album.excluded) {
                    Provider.addExcludedPath(this, album.getPath());
                    album.excluded = true;
                } else {
                    Provider.removeExcludedPath(this, album.getPath());
                    album.excluded = false;
                }
                item.setChecked(album.excluded);
                refreshMainActivityWhenClosed = !refreshMainActivityWhenClosed;
                break;
            case R.id.sort_by_date:
            case R.id.sort_by_name:
                item.setChecked(true);

                int sort_by = item.getItemId() == R.id.sort_by_date ?
                        SortUtil.BY_DATE : SortUtil.BY_NAME;
                Settings.getInstance(this).sortAlbumBy(this, sort_by);

                SortUtil.sort(this, album.getAlbumItems(), sort_by);

                recyclerView.getAdapter().notifyDataSetChanged();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && data.getAction() != null) {
            onNewIntent(data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MediaProvider.PERMISSION_REQUEST_CODE: {
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
                            MediaProvider.checkPermission(AlbumActivity.this);
                        }
                    });
                    Util.showSnackbar(snackbar);
                }
            }
        }
    }

    public void deleteAlbumItemsSnackbar() {
        if (!MediaProvider.checkPermission(this)) {
            return;
        }

        final String[] selected_items
                = ((RecyclerViewAdapter) recyclerView.getAdapter()).cancelSelectorMode();

        final int[] indices = new int[selected_items.length];
        final AlbumItem[] deletedItems = new AlbumItem[selected_items.length];
        for (int i = 0; i < selected_items.length; i++) {
            for (int k = 0; k < album.getAlbumItems().size(); k++) {
                AlbumItem albumItem = album.getAlbumItems().get(k);
                if (selected_items[i].equals(albumItem.getPath())) {
                    indices[i] = k;
                    deletedItems[i] = albumItem;
                    album.getAlbumItems().remove(k);
                    recyclerView.getAdapter().notifyItemRemoved(k);
                }
            }
        }

        String message = selected_items.length == 1 ?
                selected_items.length + " " + getString(R.string.photo_deleted) :
                selected_items.length + " " + getString(R.string.photos_deleted);

        snackbar = Snackbar.make(findViewById(R.id.root_view), message, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        for (int i = 0; i < deletedItems.length; i++) {
                            AlbumItem albumItem = deletedItems[i];
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
                            deleteAlbumItems(deletedItems, indices);
                        }
                    }
                });
        Util.showSnackbar(snackbar);
    }

    public void deleteAlbumItems(final AlbumItem[] selected_items, final int[] indices) {
        File_POJO[] filesToDelete = new File_POJO[selected_items.length];
        for (int i = 0; i < filesToDelete.length; i++) {
            filesToDelete[i] = new File_POJO(selected_items[i].getPath(), true);
        }

        new Delete(filesToDelete)
                .execute(this, null,
                        new FileOperation.Callback() {
                            @Override
                            public void done() {
                                refreshMainActivityWhenClosed = true;
                                if (refreshMainActivityAfterItemWasDeleted) {
                                    setResult(RESULT_OK,
                                            new Intent(MainActivity.REFRESH_MEDIA));
                                    finish();
                                }
                            }

                            @Override
                            public void failed(String path) {
                                for (int i = 0; i < selected_items.length; i++) {
                                    if (selected_items[i].getPath().equals(path)) {
                                        album.getAlbumItems().add(indices[i],
                                                selected_items[i]);
                                        recyclerView.getAdapter()
                                                .notifyItemInserted(indices[i]);
                                        break;
                                    }
                                }
                            }
                        });
    }

    //needed to send multiple uris in intents
    private ClipData createClipData(AlbumItem[] items) {
        String[] mimeTypes = new String[items.length];
        for (int i = 0; i < items.length; i++) {
            mimeTypes[i] = MediaType.getMimeType(this, items[i].getPath());
        }

        ClipData clipData =
                new ClipData("Images", mimeTypes,
                        new ClipData.Item(items[0].getUri(this)));
        for (int i = 1; i < items.length; i++) {
            clipData.addItem(new ClipData.Item(items[i].getUri(this)));
        }
        return clipData;
    }

    public void setPhotosResult() {
        final AlbumItem[] selected_items
                = SelectorModeManager.createAlbumItemArray(this,
                ((RecyclerViewAdapter) recyclerView.getAdapter()).cancelSelectorMode());

        ClipData clipData = createClipData(selected_items);

        Intent intent = new Intent("us.koller.RESULT_ACTION");
        intent.setClipData(clipData);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onSelectorModeEnter() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setActivated(true);
        toolbar.animate().translationY(0.0f).start();

        Util.setDarkStatusBarIcons(findViewById(R.id.root_view));

        /*if (menu != null) {
            menu.findItem(R.id.exclude).setVisible(false);
            menu.findItem(R.id.sort_by).setVisible(false);
            //show share button
            menu.findItem(R.id.share).setVisible(true);
            //show copy & move button
            menu.findItem(R.id.copy).setVisible(true);
            menu.findItem(R.id.move).setVisible(true);
        }*/
        handleMenuVisibilityForSelectorMode(true);

        if (!pick_photos) {
            ColorFade.fadeBackgroundColor(toolbar,
                    ContextCompat.getColor(this, toolbar_color_res),
                    ContextCompat.getColor(this, accent_color_res));

            ColorFade.fadeToolbarTitleColor(toolbar,
                    ContextCompat.getColor(this, R.color.grey_900_translucent),
                    null, true);

            //fade overflow menu icon
            ColorFade.fadeIconColor(toolbar.getOverflowIcon(),
                    ContextCompat.getColor(this, text_color_secondary_res),
                    ContextCompat.getColor(this, R.color.grey_900_translucent));

            Drawable navIcon = toolbar.getNavigationIcon();
            if (navIcon instanceof Animatable) {
                ((Animatable) navIcon).start();
                ColorFade.fadeIconColor(navIcon,
                        ContextCompat.getColor(this, text_color_secondary_res),
                        ContextCompat.getColor(this, R.color.grey_900_translucent));
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Drawable d;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        AnimatedVectorDrawable drawable = (AnimatedVectorDrawable)
                                ContextCompat.getDrawable(AlbumActivity.this, R.drawable.cancel_to_back_avd);
                        //mutating avd to reset it
                        drawable.mutate();
                        d = drawable;
                    } else {
                        d = ContextCompat.getDrawable(AlbumActivity.this, R.drawable.ic_arrow_back_white_24dp);
                    }
                    d = DrawableCompat.wrap(d);
                    DrawableCompat.setTint(d.mutate(),
                            ContextCompat.getColor(AlbumActivity.this,
                                    R.color.grey_900_translucent));
                    toolbar.setNavigationIcon(d);

                }
            }, navIcon instanceof Animatable ? (int) (500 * Util.getAnimatorSpeed(this)) : 0);
        } else {
            toolbar.setBackgroundColor(ContextCompat
                    .getColor(this, accent_color_res));
            toolbar.setTitleTextColor(ContextCompat
                    .getColor(AlbumActivity.this, R.color.grey_900_translucent));
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

        if (THEME != ThemeableActivity.LIGHT) {
            toolbar.setActivated(false);
        }

        ColorFade.fadeBackgroundColor(toolbar,
                ContextCompat.getColor(this, accent_color_res),
                ContextCompat.getColor(this, toolbar_color_res));

        ColorFade.fadeToolbarTitleColor(toolbar,
                ContextCompat.getColor(this, text_color_res),
                new ColorFade.ToolbarTitleFadeCallback() {
                    @Override
                    public void setTitle(Toolbar toolbar) {
                        toolbar.setTitle(album.getName());
                    }
                }, true);

        //fade overflow menu icon
        ColorFade.fadeIconColor(toolbar.getOverflowIcon(),
                ContextCompat.getColor(this, R.color.grey_900_translucent),
                ContextCompat.getColor(this, text_color_secondary_res));

        if (THEME != ThemeableActivity.LIGHT) {
            Util.setLightStatusBarIcons(findViewById(R.id.root_view));
        }

        Drawable navIcon = toolbar.getNavigationIcon();
        if (navIcon instanceof Animatable) {
            ((Animatable) navIcon).start();
            ColorFade.fadeIconColor(navIcon,
                    ContextCompat.getColor(this, R.color.grey_900_translucent),
                    ContextCompat.getColor(this, text_color_secondary_res));
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Drawable d;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    AnimatedVectorDrawable drawable = (AnimatedVectorDrawable)
                            ContextCompat.getDrawable(AlbumActivity.this, R.drawable.back_to_cancel_avd);
                    //mutating avd to reset it
                    drawable.mutate();
                    d = drawable;
                } else {
                    d = ContextCompat.getDrawable(AlbumActivity.this, R.drawable.ic_arrow_back_white_24dp);
                }
                d = DrawableCompat.wrap(d);
                DrawableCompat.setTint(d.mutate(),
                        ContextCompat.getColor(AlbumActivity.this,
                                text_color_secondary_res));
                toolbar.setNavigationIcon(d);

                /*menu.findItem(R.id.exclude).setVisible(true);
                menu.findItem(R.id.sort_by).setVisible(true);
                menu.findItem(R.id.share).setVisible(false);
                menu.findItem(R.id.copy).setVisible(false);
                menu.findItem(R.id.move).setVisible(false);*/
                handleMenuVisibilityForSelectorMode(false);
            }
        }, navIcon instanceof Animatable ? (int) (500 * Util.getAnimatorSpeed(this)) : 0);

        animateFab(false, false);
    }

    @Override
    public void onItemSelected(int selectedItemCount) {
        final String title = String.valueOf(selectedItemCount) + (selectedItemCount > 1 ?
                getString(R.string.items) : getString(R.string.item));
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //toolbar.setTitle(title);

        ColorFade.fadeToolbarTitleColor(toolbar,
                ContextCompat.getColor(this, R.color.grey_900_translucent),
                new ColorFade.ToolbarTitleFadeCallback() {
                    @Override
                    public void setTitle(Toolbar toolbar) {
                        toolbar.setTitle(title);
                    }
                }, true);


        if (selectedItemCount > 0) {
            if (pick_photos) {
                animateFab(true, false);
            }
        } else {
            if (pick_photos) {
                animateFab(false, false);
            }
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
        }, (int) (400 * Util.getAnimatorSpeed(this)));
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
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                if (show) {
                                    Drawable drawable = fab.getDrawable();
                                    if (drawable instanceof Animatable) {
                                        ((Animatable) drawable).start();
                                    }
                                }
                            }
                        })
                        .start();
            }
        }, click ? (int) (400 * Util.getAnimatorSpeed(this)) : 0);
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
            if (refreshMainActivityWhenClosed) {
                Provider.saveExcludedPaths(this);
                setResult(RESULT_OK, new Intent(MainActivity.REFRESH_MEDIA));
            }
            super.onBackPressed();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //outState.putParcelable(ALBUM, album);
        outState.putParcelable(RECYCLER_VIEW_SCROLL_STATE,
                recyclerView.getLayoutManager().onSaveInstanceState());

        RecyclerViewAdapter adapter = ((RecyclerViewAdapter) recyclerView.getAdapter());
        adapter.saveInstanceState(outState);
    }

    @Override
    public boolean canSwipeBack(int dir) {
        return SwipeBackCoordinatorLayout.canSwipeBackForThisView(recyclerView, dir) && !pick_photos;
    }

    @Override
    public void onSwipeProcess(float percent) {
        getWindow().getDecorView().setBackgroundColor(
                SwipeBackCoordinatorLayout.getBackgroundColor(percent));
    }

    @Override
    public void onSwipeFinish(int dir) {
        if (((RecyclerViewAdapter) recyclerView.getAdapter()).isSelectorModeActive()) {
            ((RecyclerViewAdapter) recyclerView.getAdapter()).cancelSelectorMode();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setReturnTransition(new TransitionSet()
                    .setOrdering(TransitionSet.ORDERING_TOGETHER)
                    .addTransition(new Slide(dir > 0 ? Gravity.TOP : Gravity.BOTTOM))
                    .addTransition(new Fade())
                    .setInterpolator(new AccelerateDecelerateInterpolator()));
        }
        onBackPressed();
    }

    @Override
    public int getThemeRes(int style) {
        if (style == DARK) {
            return R.style.Theme_CameraRoll_Main;
        } else {
            return R.style.Theme_CameraRoll_Light_Main;
        }
    }

    @Override
    public void onThemeApplied(int theme) {
        if (pick_photos) {
            return;
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(ContextCompat.getColor(this, toolbar_color_res));
        toolbar.setTitleTextColor(ContextCompat.getColor(this, text_color_res));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setBackgroundTintList(ColorStateList
                .valueOf(ContextCompat.getColor(this, accent_color_res)));

        if (theme == ThemeableActivity.LIGHT) {
            toolbar.setActivated(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Util.setDarkStatusBarIcons(findViewById(R.id.root_view));
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(ContextCompat.getColor(this,
                        R.color.black_translucent1));
            }
        }
    }
}
