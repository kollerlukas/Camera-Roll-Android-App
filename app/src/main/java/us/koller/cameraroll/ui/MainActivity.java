package us.koller.cameraroll.ui;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import us.koller.cameraroll.jobservices.MediaJobService;
import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.main.RecyclerViewAdapter;
import us.koller.cameraroll.adapter.main.ViewHolder.NestedRecyclerViewAlbumHolder;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.Provider.MediaProvider;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.ui.widget.ParallaxImageView;
import us.koller.cameraroll.util.SortUtil;
import us.koller.cameraroll.util.Util;

public class MainActivity extends ThemeableActivity {

    public static final String ALBUMS = "ALBUMS";
    public static final String REFRESH_MEDIA = "REFRESH_MEDIA";
    public static final String SHARED_PREF_NAME = "CAMERA_ROLL_SETTINGS";
    public static final String HIDDEN_FOLDERS = "HIDDEN_FOLDERS";
    public static final String PICK_PHOTOS = "PICK_PHOTOS";

    public static final int PICK_PHOTOS_REQUEST_CODE = 6;
    public static final int REFRESH_PHOTOS_REQUEST_CODE = 7;

    //needed for sharedElement-Transition in Nested RecyclerView Style
    private NestedRecyclerViewAlbumHolder sharedElementViewHolder;
    private final SharedElementCallback mCallback
            = new SharedElementCallback() {
        @Override
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (sharedElementViewHolder.sharedElementReturnPosition != -1) {
                String newTransitionName = sharedElementViewHolder.album.getAlbumItems()
                        .get(sharedElementViewHolder.sharedElementReturnPosition).getPath();
                View layout = sharedElementViewHolder.recyclerView.findViewWithTag(newTransitionName);
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
    private RecyclerViewAdapter recyclerViewAdapter;

    private Snackbar snackbar;

    private MediaProvider mediaProvider;

    private boolean hiddenFolders = false;

    private boolean pick_photos;
    private boolean allowMultiple;

    public static boolean refreshMediaWhenVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pick_photos = getIntent().getAction() != null && getIntent().getAction().equals(PICK_PHOTOS);
        allowMultiple = getIntent().getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);

        hiddenFolders = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
                .getBoolean(HIDDEN_FOLDERS, false);

        //load media
        albums = MediaProvider.getAlbums();
        if (albums == null) {
            albums = new ArrayList<>();
        }

        if (savedInstanceState == null) {
            refreshPhotos();
        }

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundColor(!pick_photos ?
                ContextCompat.getColor(this, toolbar_color_res) :
                ContextCompat.getColor(this, R.color.colorAccent));
        toolbar.setTitleTextColor(!pick_photos ?
                ContextCompat.getColor(this, text_color_res) :
                ContextCompat.getColor(this, R.color.grey_900_translucent));

        if (pick_photos) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(allowMultiple ? getString(R.string.pick_photos) : getString(R.string.pick_photo));
            }
            toolbar.setActivated(true);
            toolbar.setNavigationIcon(R.drawable.ic_clear_black_24dp);
            Drawable navIcon = toolbar.getNavigationIcon();
            if (navIcon != null) {
                navIcon = DrawableCompat.wrap(navIcon);
                DrawableCompat.setTint(navIcon.mutate(),
                        ContextCompat.getColor(this, R.color.grey_900_translucent));
                toolbar.setNavigationIcon(navIcon);
            }
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });

            Util.colorToolbarOverflowMenuIcon(toolbar,
                    ContextCompat.getColor(this, R.color.grey_900_translucent));

            Util.setDarkStatusBarIcons(findViewById(R.id.root_view));
        }

        Settings settings = Settings.getInstance(this);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setTag(ParallaxImageView.RECYCLER_VIEW_TAG);
        int columnCount = settings.getStyleColumnCount(this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, columnCount));
        recyclerViewAdapter = new RecyclerViewAdapter(this, pick_photos).setAlbums(albums);
        recyclerView.setAdapter(recyclerViewAdapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (pick_photos) {
                    return;
                }

                //hiding toolbar on scroll
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

        //setting window insets manually
        final ViewGroup rootView = (ViewGroup) findViewById(R.id.root_view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            rootView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
                public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                    toolbar.setPadding(toolbar.getPaddingStart() /*+ insets.getSystemWindowInsetLeft()*/,
                            toolbar.getPaddingTop() + insets.getSystemWindowInsetTop(),
                            toolbar.getPaddingEnd() /*+ insets.getSystemWindowInsetRight()*/,
                            toolbar.getPaddingBottom());

                    ViewGroup.MarginLayoutParams toolbarParams
                            = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
                    toolbarParams.leftMargin += insets.getSystemWindowInsetLeft();
                    toolbarParams.rightMargin += insets.getSystemWindowInsetRight();
                    toolbar.setLayoutParams(toolbarParams);

                    recyclerView.setPadding(recyclerView.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                            recyclerView.getPaddingTop() + (pick_photos ? 0 : insets.getSystemWindowInsetTop()),
                            recyclerView.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                            recyclerView.getPaddingBottom() + insets.getSystemWindowInsetBottom());

                    // clear this listener so insets aren't re-applied
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

                                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                }
                            });
        }

        //setting recyclerView top padding, so recyclerView starts below the toolbar
        toolbar.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                recyclerView.setPadding(recyclerView.getPaddingStart(),
                        recyclerView.getPaddingTop() + toolbar.getHeight(),
                        recyclerView.getPaddingEnd(),
                        recyclerView.getPaddingBottom());

                recyclerView.scrollToPosition(0);

                toolbar.getViewTreeObserver().removeOnPreDrawListener(this);
                return false;
            }
        });
    }

    @Override
    public void onActivityReenter(final int resultCode, Intent intent) {
        super.onActivityReenter(resultCode, intent);

        if (intent.getAction() != null) {
            if (intent.getAction().equals(ItemActivity.SHARED_ELEMENT_RETURN_TRANSITION)) {
                //handle shared-element transition, for nested recyclerView style
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (Settings.getInstance(this).getStyle()
                            == getResources().getInteger(R.integer.STYLE_NESTED_RECYCLER_VIEW_VALUE)) {
                        Bundle tmpReenterState = new Bundle(intent.getExtras());
                        if (tmpReenterState.containsKey(AlbumActivity.ALBUM_PATH)
                                && tmpReenterState.containsKey(AlbumActivity.EXTRA_CURRENT_ALBUM_POSITION)) {

                            String albumPath = tmpReenterState.getString(AlbumActivity.ALBUM_PATH);
                            final int sharedElementReturnPosition
                                    = tmpReenterState.getInt(AlbumActivity.EXTRA_CURRENT_ALBUM_POSITION);

                            int index = -1;

                            for (int i = 0; i < albums.size(); i++) {
                                if (albums.get(i).getPath().equals(albumPath)) {
                                    index = i;
                                    break;
                                }
                            }

                            if (index == -1) {
                                return;
                            }

                            postponeEnterTransition();

                            setExitSharedElementCallback(mCallback);

                            final NestedRecyclerViewAlbumHolder
                                    .StartSharedElementTransitionCallback callback =
                                    new NestedRecyclerViewAlbumHolder
                                            .StartSharedElementTransitionCallback() {
                                        @Override
                                        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                                        public void startPostponedEnterTransition() {
                                            MainActivity.this.startPostponedEnterTransition();
                                        }
                                    };

                            final int finalIndex = index;

                            recyclerView.scrollToPosition(index);

                            recyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                                @Override
                                public void onLayoutChange(View v, int l, int t, int r, int b,
                                                           int oL, int oT, int oR, int oB) {
                                    RecyclerView.ViewHolder viewHolder
                                            = recyclerView.findViewHolderForAdapterPosition(finalIndex);
                                    if (viewHolder instanceof NestedRecyclerViewAlbumHolder) {
                                        sharedElementViewHolder = (NestedRecyclerViewAlbumHolder) viewHolder;
                                        ((NestedRecyclerViewAlbumHolder) viewHolder)
                                                .onSharedElement(sharedElementReturnPosition, callback);
                                    }

                                    recyclerView.removeOnLayoutChangeListener(this);
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (getResources().getBoolean(R.bool.landscape)) {
            setSystemUiFlags();
        }

        if (refreshMediaWhenVisible) {
            refreshPhotos();
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
        }
    }

    @Override
    protected void onStop() {
        getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE).edit()
                .putBoolean(HIDDEN_FOLDERS, hiddenFolders).apply();
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (recyclerView != null) {
            int columnCount = Settings.getInstance(this).getStyleColumnCount(this);
            ((GridLayoutManager) recyclerView.getLayoutManager()).setSpanCount(columnCount);
        }
    }

    private void setSystemUiFlags() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    public void refreshPhotos() {
        if (mediaProvider != null) {
            mediaProvider.onDestroy();
            mediaProvider = null;
        }

        refreshMediaWhenVisible = false;

        snackbar = Snackbar.make(findViewById(R.id.root_view),
                R.string.loading, Snackbar.LENGTH_INDEFINITE);
        Util.showSnackbar(snackbar);

        final MediaProvider.Callback callback
                = new MediaProvider.Callback() {
            @Override
            public void onMediaLoaded(final ArrayList<Album> albums) {
                if (albums != null) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.albums = albums;
                            recyclerViewAdapter.setAlbums(albums);
                            recyclerViewAdapter.notifyDataSetChanged();

                            snackbar.dismiss();

                            if (mediaProvider != null) {
                                mediaProvider.onDestroy();
                            }
                            mediaProvider = null;
                        }
                    });
                }
            }

            @Override
            public void timeout() {
                //handle timeout
                snackbar.dismiss();
                snackbar = Snackbar.make(findViewById(R.id.root_view),
                        R.string.loading_failed, Snackbar.LENGTH_INDEFINITE);
                snackbar.setAction(getString(R.string.retry), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mediaProvider != null) {
                            mediaProvider.onDestroy();
                        }
                        refreshPhotos();
                        snackbar.dismiss();
                    }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            listenForMediaChanges();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void listenForMediaChanges() {
        //schedule Job
        JobScheduler js =
                (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo.Builder builder = new JobInfo.Builder(
                5,
                new ComponentName(this, MediaJobService.class));
        builder.addTriggerContentUri(
                new JobInfo.TriggerContentUri(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS));
        js.schedule(builder.build());
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
        }

        if (pick_photos) {
            menu.findItem(R.id.about).setVisible(false);
            menu.findItem(R.id.file_explorer).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                refreshPhotos();
                break;
            case R.id.hiddenFolders:
                hiddenFolders = !hiddenFolders;
                item.setChecked(hiddenFolders);
                refreshPhotos();
                break;
            case R.id.file_explorer:
                startActivity(new Intent(this, FileExplorerActivity.class),
                        ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle());
                break;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.about:
                startActivity(new Intent(this, AboutActivity.class),
                        ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle());
                break;
            case R.id.sort_by_name:
            case R.id.sort_by_size:
                item.setChecked(true);

                int sort_by = item.getItemId() == R.id.sort_by_name ?
                        SortUtil.BY_NAME : SortUtil.BY_SIZE;
                Settings.getInstance(this).sortAlbumsBy(this, sort_by);

                refreshPhotos();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MediaProvider.PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission granted
                    refreshPhotos();
                    if (snackbar != null) {
                        snackbar.dismiss();
                    }
                } else {
                    // permission denied
                    snackbar = Util.getPermissionDeniedSnackbar(findViewById(R.id.root_view));
                    snackbar.setAction(R.string.retry, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            refreshPhotos();
                        }
                    });
                    Util.showSnackbar(snackbar);
                }
            }
        }
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
                if (data != null && data.getAction() != null) {
                    if (data.getAction().equals(AlbumActivity.ALBUM_ITEM_DELETED)
                            || data.getAction().equals(REFRESH_MEDIA)) {
                        refreshPhotos();
                    }
                }
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //not able to save albums in Bundle, --> TransactionTooLargeException
        //outState.putParcelableArrayList(ALBUMS, albums);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaProvider != null) {
            mediaProvider.onDestroy();
        }
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

        if (theme == ThemeableActivity.LIGHT) {
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
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
