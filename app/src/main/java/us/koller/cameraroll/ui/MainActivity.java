package us.koller.cameraroll.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.AbstractRecyclerViewAdapter;
import us.koller.cameraroll.adapter.SelectorModeManager;
import us.koller.cameraroll.adapter.main.MainAdapter;
import us.koller.cameraroll.adapter.main.NoFolderRecyclerViewAdapter;
import us.koller.cameraroll.adapter.main.viewHolder.NestedRecyclerViewAlbumHolder;
import us.koller.cameraroll.data.ContentObserver;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.data.fileOperations.FileOperation;
import us.koller.cameraroll.data.models.Album;
import us.koller.cameraroll.data.provider.MediaProvider;
import us.koller.cameraroll.styles.NestedRecyclerView;
import us.koller.cameraroll.styles.Style;
import us.koller.cameraroll.themes.Theme;
import us.koller.cameraroll.ui.widget.FastScrollerRecyclerView;
import us.koller.cameraroll.ui.widget.GridMarginDecoration;
import us.koller.cameraroll.ui.widget.ParallaxImageView;
import us.koller.cameraroll.util.SortUtil;
import us.koller.cameraroll.util.Util;

public class MainActivity extends ThemeableActivity {

    //public static final String ALBUMS = "ALBUMS";
    public static final String REFRESH_MEDIA = "REFRESH_MEDIA";
    public static final String PICK_PHOTOS = "PICK_PHOTOS";
    public static final String RESORT = "RESORT";

    public static final int PICK_PHOTOS_REQUEST_CODE = 6;
    public static final int REFRESH_PHOTOS_REQUEST_CODE = 7;
    public static final int REMOVABLE_STORAGE_PERMISSION_REQUEST_CODE = 8;
    public static final int SETTINGS_REQUEST_CODE = 9;

    //needed for sharedElement-Transition in Nested RecyclerView Style
    private NestedRecyclerViewAlbumHolder sharedElementViewHolder;
    private final SharedElementCallback mCallback
            = new SharedElementCallback() {
        @Override
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (sharedElementViewHolder == null) {
                return;
            }

            if (sharedElementViewHolder.sharedElementReturnPosition != -1
                    && sharedElementViewHolder.sharedElementReturnPosition <
                    sharedElementViewHolder.getAlbum().getAlbumItems().size()) {
                String newTransitionName = sharedElementViewHolder.getAlbum().getAlbumItems()
                        .get(sharedElementViewHolder.sharedElementReturnPosition).getPath();
                View layout = sharedElementViewHolder.nestedRecyclerView.findViewWithTag(newTransitionName);
                View newSharedElement = layout != null ? layout.findViewById(R.id.image) : null;
                if (newSharedElement != null) {
                    names.clear();
                    names.add(newTransitionName);
                    sharedElements.clear();
                    sharedElements.put(newTransitionName, newSharedElement);
                }
                sharedElementViewHolder.sharedElementReturnPosition = -1;
            } else {
                View v = sharedElementViewHolder.itemView.getRootView();
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

    private ArrayList<Album> albums;

    private RecyclerView recyclerView;
    private AbstractRecyclerViewAdapter<ArrayList<Album>> recyclerViewAdapter;

    private Snackbar snackbar;

    private MediaProvider mediaProvider;

    private ContentObserver observer;

    private boolean hiddenFolders;

    private boolean pick_photos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pick_photos = getIntent().getAction() != null && getIntent().getAction().equals(PICK_PHOTOS);
        boolean allowMultiple = getIntent().getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);

        final Settings settings = Settings.getInstance(this);

        hiddenFolders = settings.getHiddenFolders();

        //load media
        albums = MediaProvider.getAlbumsWithVirtualDirectories(this);
        if (albums == null) {
            albums = new ArrayList<>();
        }

        if (savedInstanceState == null) {
            refreshPhotos();
        }

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundColor(!pick_photos ? toolbarColor : accentColor);
        toolbar.setTitleTextColor(!pick_photos ? textColorPrimary : accentTextColor);

        ActionBar actionBar = getSupportActionBar();
        if (pick_photos) {
            if (actionBar != null) {
                actionBar.setTitle(allowMultiple ? getString(R.string.pick_photos) : getString(R.string.pick_photo));
            }
            toolbar.setActivated(true);
            toolbar.setNavigationIcon(R.drawable.ic_clear_white);
            Drawable navIcon = toolbar.getNavigationIcon();
            if (navIcon != null) {
                navIcon = DrawableCompat.wrap(navIcon);
                DrawableCompat.setTint(navIcon.mutate(), accentTextColor);
                toolbar.setNavigationIcon(navIcon);
            }
            toolbar.setNavigationOnClickListener(view -> finish());

            Util.colorToolbarOverflowMenuIcon(toolbar, accentTextColor);
            if (theme.darkStatusBarIconsInSelectorMode()) {
                Util.setDarkStatusBarIcons(findViewById(R.id.root_view));
            }
        } else {
            if (actionBar != null) {
                actionBar.setTitle(getString(R.string.toolbar_title));
            }
        }

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setTag(ParallaxImageView.RECYCLER_VIEW_TAG);
        SelectorModeManager.Callback callback = new SelectorModeManager.SimpleCallback() {
            @Override
            public void onSelectorModeEnter() {
                super.onSelectorModeEnter();
                showAndHideFab(false);
            }

            @Override
            public void onSelectorModeExit() {
                super.onSelectorModeExit();
                showAndHideFab(true);
            }
        };
        int spanCount, spacing;
        if (settings.noFolderMode()) {
            spanCount = settings.getColumnCount(this);
            spacing = (int) getResources().getDimension(R.dimen.album_grid_spacing) / 2;
            recyclerView.addItemDecoration(new GridMarginDecoration(spacing + spacing));
            recyclerViewAdapter = new NoFolderRecyclerViewAdapter(callback, recyclerView, pick_photos)
                    .setData(albums);
        } else {
            Style style = settings.getStyleInstance(this, pick_photos);
            spanCount = style.getColumnCount(this);
            spacing = (int) style.getGridSpacing(this);
            recyclerViewAdapter = new MainAdapter(this, pick_photos).setData(albums);
            recyclerViewAdapter.getSelectorManager().addCallback(callback);
        }
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, spanCount));

        if (recyclerView instanceof FastScrollerRecyclerView) {
            ((FastScrollerRecyclerView) recyclerView).addOuterGridSpacing(spacing);
        }

        //disable default change animation
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        //restore Selector mode, when needed
        if (savedInstanceState != null) {
            SelectorModeManager manager = new SelectorModeManager(savedInstanceState);
            recyclerViewAdapter.setSelectorModeManager(manager);
        }

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (pick_photos) {
                    return;
                }

                //hiding toolbar on scroll
                float translationY = toolbar.getTranslationY() - dy;
                if (-translationY > toolbar.getHeight()) {
                    translationY = -toolbar.getHeight();
                    if (theme.elevatedToolbar()) {
                        toolbar.setActivated(true);
                    }
                } else if (translationY > 0) {
                    translationY = 0;
                    if (theme.elevatedToolbar() && !recyclerView.canScrollVertically(-1)) {
                        toolbar.setActivated(false);
                    }
                }
                toolbar.setTranslationY(translationY);

                //animate statusBarIcon color
                boolean selectorModeActive = recyclerViewAdapter
                        .getSelectorManager().isSelectorModeActive();
                if (!selectorModeActive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                        && theme.isBaseLight()) {
                    //only animate statusBar icons color, when not in selectorMode
                    float animatedValue = (-translationY) / toolbar.getHeight();
                    if (animatedValue > 0.9f) {
                        Util.setLightStatusBarIcons(findViewById(R.id.root_view));
                    } else {
                        Util.setDarkStatusBarIcons(findViewById(R.id.root_view));
                    }
                }
            }
        });

        final FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(this::fabClicked);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Drawable d = ContextCompat.getDrawable(this,
                    R.drawable.ic_camera_lens_avd);
            fab.setImageDrawable(d);
        } else {
            fab.setImageResource(R.drawable.ic_camera_white);
        }
        Drawable d = fab.getDrawable();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            d.setTint(accentTextColor);
        } else {
            d = DrawableCompat.wrap(d);
            DrawableCompat.setTint(d.mutate(), accentTextColor);
        }
        fab.setImageDrawable(d);

        if (pick_photos || !settings.getCameraShortcut()) {
            fab.hide();
        }

        //setting window insets manually
        final ViewGroup rootView = findViewById(R.id.root_view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            rootView.setOnApplyWindowInsetsListener((view, insets) -> {
                // clear this listener so insets aren't re-applied
                rootView.setOnApplyWindowInsetsListener(null);
                Log.d("MainActivity", "onApplyWindowInsets()"
                        + "[" + insets.getSystemWindowInsetLeft() + ", " +
                        insets.getSystemWindowInsetTop() + ", " +
                        insets.getSystemWindowInsetRight() + ", " +
                        insets.getSystemWindowInsetBottom() + "]");

                toolbar.setPadding(toolbar.getPaddingStart(),
                        toolbar.getPaddingTop() + insets.getSystemWindowInsetTop(),
                        toolbar.getPaddingEnd(),
                        toolbar.getPaddingBottom());

                ViewGroup.MarginLayoutParams toolbarParams
                        = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
                toolbarParams.leftMargin = insets.getSystemWindowInsetLeft();
                toolbarParams.rightMargin = insets.getSystemWindowInsetRight();
                toolbar.setLayoutParams(toolbarParams);

                recyclerView.setPadding(recyclerView.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                        recyclerView.getPaddingTop() + insets.getSystemWindowInsetTop(),
                        recyclerView.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                        recyclerView.getPaddingBottom() + insets.getSystemWindowInsetBottom());

                fab.setTranslationY(-insets.getSystemWindowInsetBottom());
                fab.setTranslationX(-insets.getSystemWindowInsetRight());

                return insets.consumeSystemWindowInsets();
            });
        } else {
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    // hacky way of getting window insets on pre-Lollipop
                    // somewhat works...
                    int[] screenSize = Util.getScreenSize(MainActivity.this);

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

                    fab.setTranslationX(-windowInsets[2]);
                    fab.setTranslationY(-windowInsets[3]);
                }
            });
        }

        //needed for transparent statusBar
        setSystemUiFlags();
    }

    @Override
    public void onActivityReenter(final int resultCode, Intent intent) {
        super.onActivityReenter(resultCode, intent);

        if (intent.getAction() != null
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && intent.getAction().equals(ItemActivity.SHARED_ELEMENT_RETURN_TRANSITION)
                && Settings.getInstance(this).getStyleInstance(this, pick_photos) instanceof NestedRecyclerView) {
            //handle shared-element transition, for nested nestedRecyclerView style
            Bundle tmpReenterState = new Bundle(intent.getExtras());
            if (tmpReenterState.containsKey(AlbumActivity.ALBUM_PATH)
                    && tmpReenterState.containsKey(AlbumActivity.EXTRA_CURRENT_ALBUM_POSITION)) {

                String albumPath = tmpReenterState.getString(AlbumActivity.ALBUM_PATH);
                Log.d("MainActivity", "albumPath: " + albumPath);
                final int sharedElementReturnPosition = tmpReenterState.getInt(AlbumActivity.EXTRA_CURRENT_ALBUM_POSITION);
                int index = -1;
                ArrayList<Album> albums = MediaProvider.getAlbumsWithVirtualDirectories(this);
                for (int i = 0; i < albums.size(); i++) {
                    Log.d("MainActivity", "albums: " + albums.get(i).getPath());
                    if (albums.get(i).getPath().equals(albumPath)) {
                        index = i;
                        break;
                    }
                }

                Log.d("MainActivity", "index: " + index);

                if (index == -1) {
                    return;
                }

                //postponing transition until sharedElement is laid out
                postponeEnterTransition();
                setExitSharedElementCallback(mCallback);
                //sharedElement is laid out --> start transition
                final NestedRecyclerViewAlbumHolder
                        .StartSharedElementTransitionCallback callback = MainActivity.this::startPostponedEnterTransition;

                final int finalIndex = index;
                recyclerView.scrollToPosition(index);
                //wait until ViewHolder is laid out
                recyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onLayoutChange(View v, int l, int t, int r, int b,
                                               int oL, int oT, int oR, int oB) {
                        RecyclerView.ViewHolder viewHolder
                                = recyclerView.findViewHolderForAdapterPosition(finalIndex);

                        if (viewHolder != null) {
                            recyclerView.removeOnLayoutChangeListener(this);
                        } else {
                            //viewHolder hasn't been laid out yet --> wait
                            recyclerView.scrollToPosition(finalIndex);
                        }

                        if (viewHolder instanceof NestedRecyclerViewAlbumHolder) {
                            //found ViewHolder
                            sharedElementViewHolder = (NestedRecyclerViewAlbumHolder) viewHolder;
                            ((NestedRecyclerViewAlbumHolder) viewHolder)
                                    .onSharedElement(sharedElementReturnPosition, callback);
                        }
                    }
                });
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent == null || intent.getAction() == null) {
            return;
        }

        switch (intent.getAction()) {
            case REFRESH_MEDIA:
                refreshPhotos();
                break;
            case RESORT:
                resortAlbums();
                break;
            default:
                break;
        }
    }

    public void refreshPhotos() {
        if (mediaProvider != null) {
            mediaProvider.onDestroy();
            mediaProvider = null;
        }

        snackbar = Snackbar.make(findViewById(R.id.root_view),
                R.string.loading, Snackbar.LENGTH_INDEFINITE);
        Util.showSnackbar(snackbar);

        final MediaProvider.OnMediaLoadedCallback callback
                = new MediaProvider.OnMediaLoadedCallback() {
            @Override
            public void onMediaLoaded(final ArrayList<Album> albums) {
                final ArrayList<Album> albumsWithVirtualDirs =
                        MediaProvider.getAlbumsWithVirtualDirectories(MainActivity.this);
                if (albums != null) {
                    MainActivity.this.runOnUiThread(() -> {
                        MainActivity.this.albums = albumsWithVirtualDirs;
                        recyclerViewAdapter.setData(albumsWithVirtualDirs);

                        snackbar.dismiss();

                        if (mediaProvider != null) {
                            mediaProvider.onDestroy();
                        }
                        mediaProvider = null;
                    });
                }
            }

            @Override
            public void timeout() {
                //handle timeout
                snackbar.dismiss();
                snackbar = Snackbar.make(findViewById(R.id.root_view),
                        R.string.loading_failed, Snackbar.LENGTH_INDEFINITE);
                snackbar.setAction(getString(R.string.retry), view -> {
                    if (mediaProvider != null) {
                        mediaProvider.onDestroy();
                    }
                    refreshPhotos();
                    snackbar.dismiss();
                });
                Util.showSnackbar(snackbar);

                if (mediaProvider != null) {
                    mediaProvider.onDestroy();
                }
                mediaProvider = null;
            }

            @Override
            public void needPermission() {
                snackbar.dismiss();
            }
        };

        mediaProvider = new MediaProvider(this);
        mediaProvider.loadAlbums(MainActivity.this, hiddenFolders, callback);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.hiddenFolders).setChecked(hiddenFolders);

        int sort_by = Settings.getInstance(this).sortAlbumsBy();
        if (sort_by == SortUtil.BY_NAME) {
            menu.findItem(R.id.sort_by_name).setChecked(true);
        } else if (sort_by == SortUtil.BY_SIZE) {
            menu.findItem(R.id.sort_by_size).setChecked(true);
        } else if (sort_by == SortUtil.BY_DATE) {
            menu.findItem(R.id.sort_by_most_recent).setChecked(true);
        }

        /*Settings s = Settings.getInstance(this);
        MenuItem cameraShortcut = menu.findItem(R.id.camera_shortcut);
        cameraShortcut.setVisible(s.getCameraShortcut() && !pick_photos);
        Drawable cameraIcon = cameraShortcut.getIcon().mutate();
        DrawableCompat.wrap(cameraIcon);
        DrawableCompat.setTint(cameraIcon, theme.getTextColorSecondary(this));
        DrawableCompat.unwrap(cameraIcon);
        cameraShortcut.setIcon(cameraIcon);*/

        if (pick_photos) {
            menu.findItem(R.id.file_explorer).setVisible(false);
            menu.findItem(R.id.settings).setVisible(false);
            menu.findItem(R.id.about).setVisible(false);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.camera_shortcut:
                Drawable d = item.getIcon();
                if (d instanceof Animatable && !((Animatable) d).isRunning()) {
                    ((Animatable) d).start();
                    fabClicked(null);
                }
                break;
            case R.id.refresh:
                refreshPhotos();
                break;
            case R.id.hiddenFolders:
                hiddenFolders = Settings.getInstance(this)
                        .setHiddenFolders(this, !hiddenFolders);
                item.setChecked(hiddenFolders);
                refreshPhotos();
                break;
            case R.id.file_explorer:
                startActivity(new Intent(this, FileExplorerActivity.class),
                        ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle());
                break;
            case R.id.settings:
                startActivityForResult(new Intent(this, SettingsActivity.class),
                        SETTINGS_REQUEST_CODE);
                break;
            case R.id.about:
                startActivity(new Intent(this, AboutActivity.class),
                        ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle());
                break;
            case R.id.sort_by_name:
            case R.id.sort_by_size:
            case R.id.sort_by_most_recent:
                item.setChecked(true);

                int sort_by;
                if (item.getItemId() == R.id.sort_by_name) {
                    sort_by = SortUtil.BY_NAME;
                } else if (item.getItemId() == R.id.sort_by_size) {
                    sort_by = SortUtil.BY_SIZE;
                } else {
                    sort_by = SortUtil.BY_DATE;
                }

                Settings.getInstance(this).sortAlbumsBy(this, sort_by);
                resortAlbums();

                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void resortAlbums() {
        final Snackbar snackbar = Snackbar.make(findViewById(R.id.root_view),
                "Sorting...", Snackbar.LENGTH_INDEFINITE);
        Util.showSnackbar(snackbar);
        AsyncTask.execute(() -> {
            //SortUtil.sortAlbums(MainActivity.this, MediaProvider.getAlbums());
            final ArrayList<Album> albums = MediaProvider.getAlbumsWithVirtualDirectories(MainActivity.this);
            MainActivity.this.runOnUiThread(() -> {
                MainActivity.this.albums = albums;
                recyclerViewAdapter.setData(albums);
                snackbar.dismiss();
            });
        });
    }

    public void fabClicked(View v) {
        if (v instanceof FloatingActionButton) {
            FloatingActionButton fab = (FloatingActionButton) v;
            Drawable drawable = fab.getDrawable();
            if (drawable instanceof Animatable) {
                ((Animatable) drawable).start();
            }
        }
        new Handler().postDelayed(() -> {
            Intent i = new Intent();
            i.setAction(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
            if (i.resolveActivity(getPackageManager()) != null) {
                startActivity(i);
            } else {
                Toast.makeText(MainActivity.this, getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
        }, (int) (500 * Util.getAnimatorSpeed(this)));
    }

    public void showAndHideFab(boolean show) {
        if (pick_photos || !Settings.getInstance(this).getCameraShortcut()) {
            return;
        }

        findViewById(R.id.fab).animate()
                .scaleX(show ? 1.0f : 0.0f)
                .scaleY(show ? 1.0f : 0.0f)
                .alpha(show ? 1.0f : 0.0f)
                .setDuration(250)
                .start();
    }

    @Override
    public void onPermissionGranted() {
        super.onPermissionGranted();
        refreshPhotos();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PICK_PHOTOS_REQUEST_CODE:
                if (resultCode != RESULT_CANCELED) {
                    setResult(RESULT_OK, data);
                    this.finish();
                }
                break;
            case REFRESH_PHOTOS_REQUEST_CODE:
                if (data != null
                        && data.getAction() != null
                        && (data.getAction().equals(AlbumActivity.ALBUM_ITEM_REMOVED)
                        || data.getAction().equals(REFRESH_MEDIA))) {
                    refreshPhotos();
                }
                break;
            case AlbumActivity.FILE_OP_DIALOG_REQUEST:
                if (resultCode == RESULT_OK) {
                    refreshPhotos();
                }
                break;
            case SETTINGS_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    // StatusBar is no longer translucent after recreate() + 2x sharedElementTransition in NestedRecyclerView-Style
                    //this.recreate();
                    Intent intent = getIntent();
                    this.finish();
                    startActivity(intent);
                }
                break;
            case ItemActivity.VIEW_IMAGE:
                break;
            default:
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        observer = new ContentObserver(new Handler());
        observer.setListener((selfChange, uri) -> {
            Log.d("MainActivity", "onChange()");
            MediaProvider.dataChanged = true;
            //observer.unregister(MainActivity.this);
            //observer = null;
        });
        observer.register(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //not able to save albums in Bundle, --> TransactionTooLargeException
        //outState.putParcelableArrayList(ALBUMS, albums);

        recyclerViewAdapter.saveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (!recyclerViewAdapter.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaProvider != null) {
            mediaProvider.onDestroy();
        }

        if (observer != null) {
            observer.unregister(this);
        }
    }

    @Override
    public int getDarkThemeRes() {
        return R.style.CameraRoll_Theme_Main;
    }

    @Override
    public int getLightThemeRes() {
        return R.style.CameraRoll_Theme_Light_Main;
    }

    @Override
    public void onThemeApplied(Theme theme) {
        if (pick_photos) {
            return;
        }

        final Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(toolbarColor);
        toolbar.setTitleTextColor(textColorPrimary);

        if (theme.darkStatusBarIcons()) {
            Util.setDarkStatusBarIcons(findViewById(R.id.root_view));
        } else {
            Util.setLightStatusBarIcons(findViewById(R.id.root_view));
        }

        if (theme.statusBarOverlay()) {
            addStatusBarOverlay(toolbar);
        }
    }

    @Override
    public BroadcastReceiver getDefaultLocalBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, final Intent intent) {
                switch (intent.getAction()) {
                    case FileOperation.RESULT_DONE:
                    case FileOperation.FAILED:
                        refreshPhotos();
                        break;
                    case RESORT:
                        resortAlbums();
                        break;
                    case DATA_CHANGED:
                        albums = MediaProvider.getAlbums();
                        recyclerViewAdapter.setData(albums);
                    default:
                        break;
                }
            }
        };
    }

    @Override
    public IntentFilter getBroadcastIntentFilter() {
        IntentFilter filter = FileOperation.Util.getIntentFilter(super.getBroadcastIntentFilter());
        filter.addAction(RESORT);
        filter.addAction(DATA_CHANGED);
        return filter;
    }
}
