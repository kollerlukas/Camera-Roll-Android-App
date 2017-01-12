package us.koller.cameraroll.ui;

import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.TransitionSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
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
import us.koller.cameraroll.data.MediaLoader;
import us.koller.cameraroll.data.Photo;
import us.koller.cameraroll.ui.widget.GridMarginDecoration;
import us.koller.cameraroll.ui.widget.SwipeBackCoordinatorLayout;
import us.koller.cameraroll.util.Util;

public class AlbumActivity extends AppCompatActivity implements SwipeBackCoordinatorLayout.OnSwipeListener {

    public static final String ALBUM = "ALBUM";
    public static final String VIEW_ALBUM = "VIEW_ALBUM";
    public static final String DELETE_PHOTO = "DELETE_PHOTO";
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

    private boolean refreshMainActivityAfterPhotoWasDeleted = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

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

        final SwipeBackCoordinatorLayout swipeBackView
                = (SwipeBackCoordinatorLayout) findViewById(R.id.root_view);
        swipeBackView.setOnSwipeListener(this);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.black_translucent2));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(album.getName());
        }

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        final int columnCount = Util.getAlbumActivityGridColumnCount(this);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, columnCount);
        /*gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                Album.AlbumItem albumItem = album.getAlbumItems().get(position);
                int[] size = Util.getImageSize(albumItem);
                Log.d("AlbumActivity", size[0] + ", " + size[1]);
                if(size[0] == 0 || size[1] == 0) {
                    return 1;
                }
                int ratio = size[0]/size[1];
                if(ratio == 0) {
                    return new Random().nextInt(2) + 1;
                } else if (ratio > columnCount) {
                    return columnCount;
                } else {
                    return ratio;
                }
            }
        });*/
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.addItemDecoration(new GridMarginDecoration(
                (int) getResources().getDimension(R.dimen.grid_spacing)));
        recyclerView.setAdapter(new RecyclerViewAdapter(album));
        if (savedInstanceState != null
                && savedInstanceState.containsKey(RECYCLER_VIEW_SCROLL_STATE)) {
            recyclerView.getLayoutManager().onRestoreInstanceState(
                    savedInstanceState.getParcelable(RECYCLER_VIEW_SCROLL_STATE));
        }

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                float translationY = toolbar.getTranslationY() - dy;
                if (-translationY > (toolbar.getHeight() + getResources().getDimension(R.dimen.statusBarSize))) {
                    translationY = -(toolbar.getHeight() + getResources().getDimension(R.dimen.statusBarSize));
                } else if (translationY > 0) {
                    translationY = 0;
                }
                toolbar.setTranslationY(translationY);
            }
        });

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setImageResource(R.drawable.ic_delete_vector_animateable);
        Drawable d = fab.getDrawable().mutate();
        d.setTint(ContextCompat.getColor(this, R.color.grey_900_translucent));
        fab.setImageDrawable(d);
        fab.setScaleX(0.0f);
        fab.setScaleY(0.0f);

        swipeBackView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                toolbar.setPadding(toolbar.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                        toolbar.getPaddingTop() + insets.getSystemWindowInsetTop(),
                        toolbar.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                        toolbar.getPaddingBottom());

                recyclerView.setPadding(recyclerView.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                        recyclerView.getPaddingTop(),
                        recyclerView.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                        recyclerView.getPaddingBottom() + insets.getSystemWindowInsetBottom());

                ViewGroup.MarginLayoutParams fabParams
                        = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
                fabParams.rightMargin += insets.getSystemWindowInsetRight();
                fabParams.bottomMargin += insets.getSystemWindowInsetBottom();
                fab.setLayoutParams(fabParams);

                swipeBackView.setOnApplyWindowInsetsListener(null);
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
        if (intent.getAction().equals(DELETE_PHOTO)) {
            final Photo photo = intent.getParcelableExtra(ItemActivity.ALBUM_ITEM);
            deletePhotoSnackbar(photo, intent.getBooleanExtra(ItemActivity.HIDDEN_PHOTO, false),
                    intent.getBooleanExtra(ItemActivity.VIEW_ONLY, false));
        }
    }

    @Override
    public void onActivityReenter(int requestCode, Intent data) {
        super.onActivityReenter(requestCode, data);
        Log.d("AlbumActivity", "onActivityReenter()");
        Bundle tmpReenterState = new Bundle(data.getExtras());
        sharedElementReturnPosition = tmpReenterState.getInt(EXTRA_CURRENT_ALBUM_POSITION);
        album.getAlbumItems().get(sharedElementReturnPosition).isSharedElement = true;
        if (recyclerView.getAdapter().getItemCount() > 0) {
            postponeEnterTransition();
            Log.d("AlbumActivity", "postponeEnterTransition()");
            recyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int l, int t, int r, int b,
                                           int oL, int oT, int oR, int oB) {
                    recyclerView.removeOnLayoutChangeListener(this);
                    startPostponedEnterTransition();
                    Log.d("AlbumActivity", "startPostponedEnterTransition()");
                }
            });
            recyclerView.scrollToPosition(sharedElementReturnPosition);
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (getResources().getBoolean(R.bool.landscape)) {
            setSystemUiFlags();
        }
    }

    public void deletePhotoSnackbar(final AlbumItem albumItem, final boolean hiddenPhoto, boolean VIEW_ONLY) {
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
                            deletePhotos(albumItems, indices);
                            /*if (!hiddenPhoto) {
                                getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                        MediaStore.Images.ImageColumns.DATA + "=?", new String[]{photo.getPath()});
                            } else {
                                File file = new File(photo.getPath());
                                boolean result = file.delete();
                            }

                            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                    Uri.parse(photo.getPath())));

                            if (refreshMainActivityAfterPhotoWasDeleted) {
                                Intent intent = new Intent(AlbumActivity.this, MainActivity.class);
                                intent.setAction(MainActivity.REFRESH_PHOTOS);
                                startActivity(intent);
                            }*/
                        }
                    }
                });
        snackbar.show();

        if (VIEW_ONLY) {
            snackbar.dismiss();
            this.finish();
        }
    }

    public void deletePhotosSnackbar() {
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
                            deletePhotos(selected_items, indices);
                            /*int successfully_deleted = 0;
                            for (int i = 0; i < selected_items.length; i++) {
                                boolean success;
                                if (!album.hiddenAlbum) {
                                    int result = getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                            MediaStore.Images.ImageColumns.DATA + "=?", new String[]{selected_items[i].getPath()});
                                    success = result > 0;
                                } else {
                                    File file = new File(selected_items[i].getPath());
                                    success = file.delete();
                                }

                                if(!success) {
                                    album.getAlbumItems().add(indices[i], selected_items[i]);
                                    recyclerView.getAdapter().notifyItemInserted(indices[i]);
                                } else {
                                    successfully_deleted++;

                                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                            Uri.parse(selected_items[i].getPath())));

                                    if (refreshMainActivityAfterPhotoWasDeleted) {
                                        Intent intent = new Intent(AlbumActivity.this, MainActivity.class);
                                        intent.setAction(MainActivity.REFRESH_PHOTOS);
                                        startActivity(intent);
                                    }
                                }
                            }
                            Toast.makeText(AlbumActivity.this, "successfully deleted " + successfully_deleted + " from " + selected_items.length, Toast.LENGTH_SHORT).show();*/
                        }
                    }
                });
        snackbar.show();
    }

    public void deletePhotos(AlbumItem[] selected_items, int[] indices) {
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

                if (refreshMainActivityAfterPhotoWasDeleted) {
                    Intent intent = new Intent(AlbumActivity.this, MainActivity.class);
                    intent.setAction(MainActivity.REFRESH_PHOTOS);
                    startActivity(intent);
                }
            }
        }
        Toast.makeText(AlbumActivity.this, getString(R.string.successfully_deleted) + successfully_deleted + " / " + selected_items.length, Toast.LENGTH_SHORT).show();
    }

    public void fabClicked() {
        animateFab(false, true);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                deletePhotosSnackbar();
            }
        }, 400);
    }

    public void animateFab(final boolean show, boolean click) {
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
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
        if (snackbar != null) {
            snackbar.dismiss();
            snackbar = null;
            refreshMainActivityAfterPhotoWasDeleted = true;
        }
        if (((RecyclerViewAdapter) recyclerView.getAdapter()).onBackPressed()) {
            animateFab(false, false);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
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
        return SwipeBackCoordinatorLayout.canSwipeBackForThisView(recyclerView, dir);
    }

    @Override
    public void onSwipeProcess(float percent) {
        ViewGroup viewGroup = (ViewGroup) findViewById(R.id.root_view);
        viewGroup.setAlpha(1 - percent + 0.7f);
    }

    @Override
    public void onSwipeFinish(int dir) {
        getWindow().setReturnTransition(new TransitionSet()
                .setOrdering(TransitionSet.ORDERING_TOGETHER)
                .addTransition(new Slide(dir > 0 ? Gravity.TOP : Gravity.BOTTOM))
                .addTransition(new Fade())
                .setInterpolator(new AccelerateDecelerateInterpolator()));
        onBackPressed();
    }
}
