package us.koller.cameraroll.ui;

import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;

import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.main.RecyclerViewAdapter;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.MediaLoader.MediaLoader;
import us.koller.cameraroll.ui.widget.ParallaxImageView;
import us.koller.cameraroll.util.Util;

public class MainActivity extends AppCompatActivity {

    public static final String ALBUMS = "ALBUMS";
    public static final String REFRESH_MEDIA = "REFRESH_MEDIA";
    public static final String SHARED_PREF_NAME = "CAMERA_ROLL_SETTINGS";
    public static final String HIDDEN_FOLDERS = "HIDDEN_FOLDERS";
    public static final String PICK_PHOTOS = "PICK_PHOTOS";

    public static final int PICK_PHOTOS_REQUEST_CODE = 6;

    private ArrayList<Album> albums;

    private RecyclerView recyclerView;
    private RecyclerViewAdapter recyclerViewAdapter;

    private Snackbar snackbar;

    private MediaLoader mediaLoader;

    private boolean hiddenFolders = false;
    private boolean pick_photos;

    private boolean allowMultiple;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pick_photos = getIntent().getAction() != null && getIntent().getAction().equals(PICK_PHOTOS);
        allowMultiple = getIntent().getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);

        hiddenFolders = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
                .getBoolean(HIDDEN_FOLDERS, false);

        //loading media
        if (savedInstanceState != null && savedInstanceState.containsKey(ALBUMS)) {
            albums = savedInstanceState.getParcelableArrayList(ALBUMS);
        } else {
            albums = new ArrayList<>();
        }

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundColor(!pick_photos ?
                ContextCompat.getColor(this, R.color.black_translucent2) :
                ContextCompat.getColor(this, R.color.colorAccent));

        if (pick_photos) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(allowMultiple ? getString(R.string.pick_photos) : getString(R.string.pick_photo));
            }
            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.grey_900_translucent));
            toolbar.setActivated(true);
            toolbar.setNavigationIcon(R.drawable.ic_clear_black_24dp);
            toolbar.getNavigationIcon().setTint(ContextCompat.getColor(this, R.color.grey_900_translucent));
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });

            Util.setDarkStatusBarIcons(findViewById(R.id.root_view));
        }

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setTag(ParallaxImageView.RECYCLER_VIEW_TAG);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAdapter = new RecyclerViewAdapter(pick_photos).setAlbums(albums);
        recyclerView.setAdapter(recyclerViewAdapter);

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
                } else if (translationY > 0) {
                    translationY = 0;
                }
                toolbar.setTranslationY(translationY);
            }
        });

        //setting window insets manually
        final ViewGroup rootView = (ViewGroup) findViewById(R.id.root_view);
        rootView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
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
                        recyclerView.getPaddingTop() + (pick_photos ? 0 : +insets.getSystemWindowInsetTop()),
                        recyclerView.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                        recyclerView.getPaddingBottom() + insets.getSystemWindowInsetBottom());

                // clear this listener so insets aren't re-applied
                rootView.setOnApplyWindowInsetsListener(null);
                return insets.consumeSystemWindowInsets();
            }
        });

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

        setupTaskDescription();

        //load media
        if (savedInstanceState == null) {
            refreshPhotos();
        }
    }

    @Override
    public void onActivityReenter(int resultCode, Intent intent) {
        super.onActivityReenter(resultCode, intent);
        if (intent.getAction().equals(REFRESH_MEDIA)) {
            refreshPhotos();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (getResources().getBoolean(R.bool.landscape)) {
            setSystemUiFlags();
        }
    }

    @Override
    protected void onStop() {
        getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE).edit()
                .putBoolean(HIDDEN_FOLDERS, hiddenFolders).apply();
        super.onStop();
    }

    private void setSystemUiFlags() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    public void refreshPhotos() {
        snackbar = Snackbar.make(findViewById(R.id.root_view),
                R.string.loading, Snackbar.LENGTH_INDEFINITE);
        Util.showSnackbar(snackbar);

        final MediaLoader.LoaderCallback callback
                = new MediaLoader.LoaderCallback() {
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

                            mediaLoader.onDestroy();
                            mediaLoader = null;
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
                        mediaLoader.onDestroy();
                        refreshPhotos();
                        snackbar.dismiss();
                    }
                });
                Util.showSnackbar(snackbar);

                if (mediaLoader != null) {
                    mediaLoader.onDestroy();
                }
                mediaLoader = null;
            }

            @Override
            public void needPermission() {
                snackbar.dismiss();
            }
        };

        mediaLoader = new MediaLoader();
        mediaLoader.loadAlbums(MainActivity.this, hiddenFolders, callback);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        if (!pick_photos) {
            menu.findItem(R.id.hiddenFolders).setChecked(hiddenFolders);
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
            case R.id.about:
                startActivity(new Intent(this, AboutActivity.class),
                        ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle());
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupTaskDescription() {
        Bitmap overviewIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.app_name),
                overviewIcon,
                ContextCompat.getColor(this, R.color.colorAccent)));
        overviewIcon.recycle();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MediaLoader.PERMISSION_REQUEST_CODE: {
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
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(ALBUMS, albums);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaLoader != null) {
            mediaLoader.onDestroy();
        }
    }
}
