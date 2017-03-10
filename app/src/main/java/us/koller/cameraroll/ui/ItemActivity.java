package us.koller.cameraroll.ui;

import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ShareCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.ContextCompat;
import android.support.v4.print.PrintHelper;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.davemorrissey.labs.subscaleview.ImageViewState;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.item.ViewHolder.GifViewHolder;
import us.koller.cameraroll.adapter.item.ViewHolder.PhotoViewHolder;
import us.koller.cameraroll.adapter.item.ViewHolder.VideoViewHolder;
import us.koller.cameraroll.adapter.item.ViewHolder.ViewHolder;
import us.koller.cameraroll.adapter.item.ViewPagerAdapter;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.Photo;
import us.koller.cameraroll.data.Provider.MediaProvider;
import us.koller.cameraroll.data.Video;
import us.koller.cameraroll.util.ColorFade;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.TransitionListenerAdapter;
import us.koller.cameraroll.util.Util;
import us.koller.cameraroll.util.ZoomOutPageTransformer;

public class ItemActivity extends AppCompatActivity {

    public interface ViewPagerOnInstantiateItemCallback {
        public boolean onInstantiateItem(ViewHolder viewHolder);
    }

    public static final String ALBUM_ITEM = "ALBUM_ITEM";
    public static final String ALBUM = "ALBUM";
    public static final String ALBUM_PATH = "ALBUM_PATH";
    public static final String ITEM_POSITION = "ITEM_POSITION";
    public static final String VIEW_ONLY = "VIEW_ONLY";
    public static final String FINISH_AFTER = "FINISH_AFTER";
    public static final String HIDDEN_ALBUMITEM = "HIDDEN_ALBUMITEM";
    private static final String WAS_SYSTEM_UI_HIDDEN = "WAS_SYSTEM_UI_HIDDEN";
    private static final String IMAGE_VIEW_SAVED_STATE = "IMAGE_VIEW_SAVED_STATE";
    private static final String INFO_DIALOG_SHOWN = "INFO_DIALOG_SHOWN";
    private static final String NO_DATA = "Unknown";

    private boolean isReturning;
    private final SharedElementCallback sharedElementCallback = new SharedElementCallback() {
        @Override
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (isReturning) {
                ViewGroup v = (ViewGroup) viewPager.findViewWithTag(albumItem.getPath());
                View sharedElement = v.findViewById(R.id.image);
                if (sharedElement == null) {
                    names.clear();
                    sharedElements.clear();
                } else {
                    names.clear();
                    names.add(sharedElement.getTransitionName());
                    sharedElements.clear();
                    sharedElements.put(sharedElement.getTransitionName(), sharedElement);
                }
            }
        }
    };

    private final TransitionListenerAdapter transitionListener
            = new TransitionListenerAdapter() {
        @Override
        public void onTransitionStart(Transition transition) {
            //hide toolbar & statusbar
            float toolbar_translationY = -(toolbar.getHeight());
            float bottomBar_translationY = ((View) bottomBar.getParent()).getHeight();
            toolbar.setTranslationY(toolbar_translationY);
            ((View) bottomBar.getParent()).setTranslationY(bottomBar_translationY);
            super.onTransitionStart(transition);
        }

        @Override
        public void onTransitionEnd(Transition transition) {
            ViewHolder viewHolder = ((ViewPagerAdapter)
                    viewPager.getAdapter()).findViewHolderByTag(albumItem.getPath());
            if (viewHolder == null) {
                return;
            }

            if (!isReturning) {
                onShowViewHolder(viewHolder);
            }

            if (transition != null) {
                super.onTransitionEnd(transition);
            }
            albumItem.isSharedElement = false;
            showUI(!isReturning);
        }
    };

    private Toolbar toolbar;
    private View bottomBar;
    private ViewPager viewPager;

    private AlertDialog infoDialog;
    private Snackbar snackbar;
    private Menu menu;

    private boolean systemUiVisible = true;

    private Album album;
    private AlbumItem albumItem;

    public boolean view_only;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item);

        MediaProvider.checkPermission(this);

        view_only = getIntent().getBooleanExtra(VIEW_ONLY, false);

        if (!view_only && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            postponeEnterTransition();
            setEnterSharedElementCallback(sharedElementCallback);
            getWindow().getSharedElementEnterTransition().addListener(transitionListener);
        }

        if (!view_only) {
            String path;
            if (savedInstanceState != null && savedInstanceState.containsKey(ALBUM_PATH)) {
                path = savedInstanceState.getString(ALBUM_PATH);
            } else {
                path = getIntent().getStringExtra(ALBUM_PATH);
            }
            album = MediaProvider.loadAlbum(path);
        } else {
            album = getIntent().getExtras().getParcelable(ALBUM);
        }

        if (savedInstanceState != null) {
            //album = savedInstanceState.getParcelable(ALBUM);
            albumItem = savedInstanceState.getParcelable(ALBUM_ITEM);
            if (albumItem != null && albumItem instanceof Photo) {
                Photo photo = (Photo) albumItem;
                ImageViewState imageViewState
                        = (ImageViewState) savedInstanceState.getSerializable(IMAGE_VIEW_SAVED_STATE);
                photo.putImageViewSavedState(imageViewState);
            }
            if (savedInstanceState.getBoolean(INFO_DIALOG_SHOWN, false)) {
                showInfoDialog();
            }
        } else {
            //album = getIntent().getExtras().getParcelable(AlbumActivity.ALBUM);
            int position = getIntent().getIntExtra(ITEM_POSITION, 0);
            if (album != null) {
                albumItem = album.getAlbumItems().get(position);
                albumItem.isSharedElement = true;
            }
        }

        if (album == null || albumItem == null) {
            return;
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(albumItem.getName() != null
                    && !view_only ? albumItem.getName() : "");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        viewPager = (ViewPager) findViewById(R.id.view_pager);
        viewPager.setAdapter(new ViewPagerAdapter(album));
        viewPager.setCurrentItem(album.getAlbumItems().indexOf(albumItem), false);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            private final int color = ContextCompat.getColor(ItemActivity.this, R.color.white);

            @Override
            public void onPageSelected(int position) {
                //if previous viewHolder was VideoViewHolder
                //pause Video
                ViewHolder viewHolder = ((ViewPagerAdapter) viewPager.getAdapter())
                        .findViewHolderByTag(albumItem.getPath());
                if (viewHolder instanceof VideoViewHolder) {
                    ((VideoViewHolder) viewHolder).pauseVideo();
                }

                //set new AlbumItem
                albumItem = album.getAlbumItems().get(position);
                ColorFade.fadeToolbarTitleColor(toolbar, color,
                        new ColorFade.ToolbarTitleFadeCallback() {
                            @Override
                            public void setTitle(Toolbar toolbar) {
                                toolbar.setTitle(albumItem.getName() != null ? albumItem.getName() : "");
                            }
                        }, true);

                viewHolder = ((ViewPagerAdapter) viewPager.getAdapter())
                        .findViewHolderByTag(albumItem.getPath());

                onShowViewHolder(viewHolder);
            }
        });

        viewPager.setPageTransformer(true, new ZoomOutPageTransformer());

        bottomBar = findViewById(R.id.bottom_bar);
        ImageView delete_button = (ImageView) bottomBar.findViewById(R.id.delete_button);
        if (!view_only) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Drawable d = ContextCompat.getDrawable(this, R.drawable.ic_delete_avd);
                delete_button.setImageDrawable(d);
            } else {
                delete_button.setImageResource(R.drawable.ic_delete_white_24dp);
            }
        } else {
            ((View) delete_button.getParent()).setVisibility(View.GONE);
        }
        /*if (albumItem instanceof Video) {
            //hide bottom bar
            bottomBar.setVisibility(View.GONE);
        } else {
            bottomBar.setVisibility(View.VISIBLE);
        }*/

        final ViewGroup rootView = (ViewGroup) findViewById(R.id.root_view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            rootView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
                public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                    toolbar.setPadding(toolbar.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                            toolbar.getPaddingTop() + insets.getSystemWindowInsetTop(),
                            toolbar.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                            toolbar.getPaddingBottom());

                    /*ViewGroup.MarginLayoutParams toolbarParams
                         = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
                    toolbarParams.leftMargin += insets.getSystemWindowInsetLeft();
                    toolbarParams.rightMargin += insets.getSystemWindowInsetRight();
                    toolbar.setLayoutParams(toolbarParams);*/

                    bottomBar.setPadding(bottomBar.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                            bottomBar.getPaddingTop(),
                            bottomBar.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                            bottomBar.getPaddingBottom() + insets.getSystemWindowInsetBottom());

                    /*ViewGroup.MarginLayoutParams bottomBarParams
                            = (ViewGroup.MarginLayoutParams) ((View) bottomBar.getParent()).getLayoutParams();
                    bottomBarParams.leftMargin += insets.getSystemWindowInsetLeft();
                    bottomBarParams.rightMargin += insets.getSystemWindowInsetRight();
                    ((View) bottomBar.getParent()).setLayoutParams(bottomBarParams);*/

                    VideoViewHolder.onBottomInset(
                            new int[]{insets.getSystemWindowInsetLeft(),
                                    insets.getSystemWindowInsetTop(),
                                    insets.getSystemWindowInsetRight(),
                                    insets.getSystemWindowInsetBottom()});

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
                                    //hacky way of getting window insets on pre-Lollipop
                                    int[] screenSize = Util.getScreenSize(ItemActivity.this);

                                    int[] windowInsets = new int[]{
                                            Math.abs(screenSize[0] - rootView.getLeft()),
                                            Math.abs(screenSize[1] - rootView.getTop()),
                                            Math.abs(screenSize[2] - rootView.getRight()),
                                            Math.abs(screenSize[3] - rootView.getBottom())};

                                    toolbar.setPadding(toolbar.getPaddingStart() + windowInsets[0],
                                            toolbar.getPaddingTop() + windowInsets[1],
                                            toolbar.getPaddingEnd() + windowInsets[2],
                                            toolbar.getPaddingBottom());

                                    bottomBar.setPadding(bottomBar.getPaddingStart() + windowInsets[0],
                                            bottomBar.getPaddingTop(),
                                            bottomBar.getPaddingEnd() + windowInsets[2],
                                            bottomBar.getPaddingBottom() + windowInsets[3]);

                                    VideoViewHolder.onBottomInset(windowInsets);

                                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                }
                            });
        }

        //needed to achieve transparent navBar
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setupTaskDescription();
        }

        if (view_only || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            albumItem.isSharedElement = false;
        }

        if (view_only || savedInstanceState != null) {
            //config was changed
            //skipping sharedElement transition
            ((ViewPagerAdapter) viewPager.getAdapter())
                    .addOnInstantiateItemCallback(
                            new ViewPagerOnInstantiateItemCallback() {
                                @Override
                                public boolean onInstantiateItem(ViewHolder viewHolder) {
                                    if (viewHolder.albumItem.getPath()
                                            .equals(albumItem.getPath())) {
                                        onShowViewHolder(viewHolder);
                                        return false;
                                    }
                                    return true;
                                }
                            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.item, menu);
        this.menu = menu;
        if (view_only) {
            menu.findItem(R.id.delete).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.set_as:
                setPhotoAs();
                break;
            case R.id.info:
                showInfoDialog();
                break;
            case R.id.share:
                sharePhoto();
                break;
            case R.id.print:
                printPhoto();
                break;
            case R.id.edit:
                editPhoto();
                break;
            case R.id.delete:
                showDeleteDialog();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();

        //if current viewHolder is instance VideoViewHolder
        //pause Video
        ViewHolder viewHolder = ((ViewPagerAdapter) viewPager.getAdapter())
                .findViewHolderByTag(albumItem.getPath());
        if (viewHolder instanceof VideoViewHolder) {
            ((VideoViewHolder) viewHolder).pauseVideo();
        }
    }

    public void onShowViewHolder(ViewHolder viewHolder) {
        if (viewHolder instanceof PhotoViewHolder) {
            ((PhotoViewHolder) viewHolder).swapView(false);
        } else if (viewHolder instanceof GifViewHolder) {
            ((GifViewHolder) viewHolder).reloadGif();
        } else if (viewHolder instanceof VideoViewHolder) {
            ((VideoViewHolder) viewHolder).swapView(false);
        }

        if (albumItem instanceof Video) {
            //hide bottom bar
            bottomBar.setVisibility(View.GONE);
            //show ui
            showSystemUI(true);
        } else {
            bottomBar.setVisibility(View.VISIBLE);
        }

        if (menu != null) {
            menu.findItem(R.id.set_as).setVisible(albumItem instanceof Photo);
            menu.findItem(R.id.print).setVisible(albumItem instanceof Photo);
        }
    }

    public void setPhotoAs() {
        if (!(albumItem instanceof Photo)) {
            return;
        }

        Uri uri = albumItem.getUri(this);

        Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);
        intent.setDataAndType(uri, MediaType.getMimeType(this, albumItem.getPath()));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        try {
            startActivityForResult(Intent.createChooser(intent,
                    getString(R.string.set_as)), 13);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No App found to edit your item", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void sharePhoto() {
        Uri uri = albumItem.getUri(this);

        Intent shareIntent = ShareCompat.IntentBuilder.from(this)
                .addStream(uri)
                .setType(MediaType.getMimeType(this, albumItem.getPath()))
                .getIntent();

        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if (shareIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_photo)));
        }
    }

    public void printPhoto() {
        if (!(albumItem instanceof Photo)) {
            Toast.makeText(this, "Printing of " + albumItem.getType()
                    + "s not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        PrintHelper photoPrinter = new PrintHelper(this);
        photoPrinter.setScaleMode(PrintHelper.SCALE_MODE_FIT);
        try {
            photoPrinter.printBitmap(albumItem.getPath(),
                    albumItem.getUri(this));
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void editPhoto() {
        Uri uri = albumItem.getUri(this);

        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setDataAndType(uri, MediaType.getMimeType(this, albumItem.getPath()));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No App found to edit your "
                    + albumItem.getType(), Toast.LENGTH_SHORT).show();
        }
    }

    public void showDeleteDialog() {
        new AlertDialog.Builder(this, R.style.Theme_CameraRoll_Dialog)
                .setTitle("Delete Photo?")
                .setNegativeButton("No", null)
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deletePhoto();
                    }
                })
                .create().show();
    }

    public void deletePhoto() {
        if (!MediaProvider.checkPermission(this)) {
            return;
        }

        if (albumItem == null) {
            return;
        }

        ViewGroup view = (ViewGroup) findViewById(R.id.root_view);
        view.removeView(viewPager);

        Intent intent = new Intent(this, AlbumActivity.class);
        intent.setAction(AlbumActivity.DELETE_ALBUMITEM);
        //intent.putExtra(AlbumActivity.ALBUM, album);
        intent.putExtra(AlbumActivity.ALBUM_PATH, album.getPath());
        intent.putExtra(ALBUM_ITEM, albumItem);
        intent.putExtra(HIDDEN_ALBUMITEM, album.isHidden());
        intent.putExtra(VIEW_ONLY, view_only);
        if (view_only) {
            startActivity(intent);
        } else {
            Log.d("ItemActivity", "setResult()");
            setResult(RESULT_OK, intent);
        }

        this.finish();
    }

    public void showInfoDialog() {
        ExifInterface exif = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri uri = albumItem.getUri(this);
                Log.d("ItemActivity", "showInfoDialog: " + uri);
                exif = new ExifInterface(getContentResolver()
                        .openInputStream(uri));
            } else {
                exif = new ExifInterface(albumItem.getPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        File file = new File(albumItem.getPath());

        String name = file.getName();
        String path = file.getPath();
        String size = getFileSize(file);

        boolean exifSupported = exif != null;
        if (!albumItem.contentUri) {
            exifSupported = exifSupported && MediaType.doesSupportExif(albumItem.getPath());
        }

        String height = NO_DATA, width = NO_DATA,
                date = NO_DATA, focal_length = NO_DATA,
                exposure = NO_DATA, model = NO_DATA,
                aperture = NO_DATA, iso = NO_DATA;
        if (exifSupported) {
            if (exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH) != null) {
                height = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
            }
            if (exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH) != null) {
                width = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
            }
            if (exif.getAttribute(ExifInterface.TAG_DATETIME) != null) {
                date = exif.getAttribute(ExifInterface.TAG_DATETIME);
            }

            focal_length = parseFocalLength(exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH));
            exposure = parseExposureTime(exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME));
            if (exif.getAttribute(ExifInterface.TAG_MAKE) != null) {
                model = exif.getAttribute(ExifInterface.TAG_MAKE) + " "
                        + exif.getAttribute(ExifInterface.TAG_MODEL);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (exif.getAttribute(ExifInterface.TAG_F_NUMBER) != null) {
                    aperture = "f/" + exif.getAttribute(ExifInterface.TAG_F_NUMBER);
                }
                if (exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS) != null) {
                    iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS);
                }
            }
        } else {
            int[] imageDimens = albumItem instanceof Video ?
                    Util.getVideoDimensions(albumItem.getPath()) :
                    Util.getImageDimensions(this, albumItem.getPath());

            height = String.valueOf(imageDimens[1]);
            width = String.valueOf(imageDimens[0]);
        }


        String[] values = {name, path, size, width + " x " + height,
                date, model, focal_length, exposure, aperture, iso};

        View rootView = LayoutInflater.from(this)
                .inflate(R.layout.info_dialog_layout,
                        (ViewGroup) findViewById(R.id.root_view), false);

        final View scrollIndicatorTop = rootView.findViewById(R.id.scroll_indicator_top);
        final View scrollIndicatorBottom = rootView.findViewById(R.id.scroll_indicator_bottom);

        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(new InfoRecyclerViewAdapter(values));

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                scrollIndicatorTop.setVisibility(
                        ViewCompat.canScrollVertically(recyclerView, -1) ?
                                View.VISIBLE : View.INVISIBLE);

                scrollIndicatorBottom.setVisibility(
                        ViewCompat.canScrollVertically(recyclerView, 1) ?
                                View.VISIBLE : View.INVISIBLE);
            }
        });

        infoDialog = new AlertDialog.Builder(this, R.style.Theme_CameraRoll_Dialog)
                .setTitle(getString(R.string.info))
                .setView(rootView)
                .setPositiveButton(R.string.done, null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        infoDialog = null;
                    }
                })
                .create();
        infoDialog.show();
    }

    public void bottomBarOnClick(View v) {
        switch (v.getId()) {
            case R.id.info_button:
                showInfoDialog();
                break;
            case R.id.share_button:
                sharePhoto();
                break;
            case R.id.edit_button:
                editPhoto();
                break;
            case R.id.delete_button:
                ImageView delete_button = (ImageView) v;
                Drawable d = delete_button.getDrawable();
                if (d instanceof Animatable) {
                    ((Animatable) d).start();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showDeleteDialog();
                        }
                    }, 400);
                } else {
                    showDeleteDialog();
                }
                break;
        }
    }

    public boolean imageOnClick() {
        systemUiVisible = !systemUiVisible;
        showSystemUI(systemUiVisible);
        return systemUiVisible;
    }

    public boolean imageOnClick(boolean show) {
        systemUiVisible = show;
        showSystemUI(systemUiVisible);
        return systemUiVisible;
    }

    private void showUI(boolean show) {
        float toolbar_translationY = show ? 0 : -(toolbar.getHeight());
        float bottomBar_translationY = show ? 0
                : ((View) bottomBar.getParent()).getHeight();
        toolbar.animate()
                .translationY(toolbar_translationY)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        ((View) bottomBar.getParent()).animate()
                .translationY(bottomBar_translationY)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        if (snackbar != null && !show) {
            snackbar.dismiss();
        }
    }

    private void showSystemUI(final boolean show) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                getWindow().getDecorView().setSystemUiVisibility(show ?
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN :
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                | View.SYSTEM_UI_FLAG_IMMERSIVE
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        });

        showUI(show);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setupTaskDescription() {
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.app_name),
                icon,
                ContextCompat.getColor(this, R.color.colorAccent)));
        icon.recycle();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull
            String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MediaProvider.PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0]
                        == PackageManager.PERMISSION_GRANTED) {
                    //permission granted
                    this.finish();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    snackbar = Util.getPermissionDeniedSnackbar(findViewById(R.id.root_view));
                    snackbar.setAction(R.string.retry, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            MediaProvider.checkPermission(ItemActivity.this);
                        }
                    });
                    Util.showSnackbar(snackbar);
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (albumItem instanceof Photo) {
            View view = viewPager.findViewWithTag(albumItem.getPath())
                    .findViewById(R.id.subsampling);
            if (view instanceof SubsamplingScaleImageView) {
                SubsamplingScaleImageView imageView = (SubsamplingScaleImageView) view;
                ImageViewState state = imageView.getState();
                if (state != null) {
                    outState.putSerializable(IMAGE_VIEW_SAVED_STATE, imageView.getState());
                }
            }
        } else if (albumItem instanceof Video) {
            ViewHolder viewHolder = ((ViewPagerAdapter) viewPager.getAdapter())
                    .findViewHolderByTag(albumItem.getPath());
            ((VideoViewHolder) viewHolder).savePlayedTime();
        }
        //outState.putParcelable(ALBUM, album);
        outState.putParcelable(ALBUM_ITEM, albumItem);
        outState.putBoolean(WAS_SYSTEM_UI_HIDDEN, !systemUiVisible);
        outState.putBoolean(INFO_DIALOG_SHOWN, infoDialog != null);
    }

    public interface Callback {
        void callback();
    }

    @Override
    public void onBackPressed() {
        if (view_only) {
            if (getIntent().getBooleanExtra(FINISH_AFTER, false)) {
                this.finishAffinity();
            } else {
                this.finish();
            }
        } else {
            showUI(false);
            ViewHolder viewHolder = ((ViewPagerAdapter)
                    viewPager.getAdapter()).findViewHolderByTag(albumItem.getPath());
            viewHolder.onSharedElement(new ItemActivity.Callback() {
                @Override
                public void callback() {
                    setResultAndFinish();
                }
            });
        }
    }

    @Override
    public boolean onNavigateUp() {
        setResultAndFinish();
        return true;
    }

    public void setResultAndFinish() {
        isReturning = true;
        Intent data = new Intent();
        data.putExtra(AlbumActivity.EXTRA_CURRENT_ALBUM_POSITION,
                viewPager.getCurrentItem());
        setResult(RESULT_OK, data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition();
        } else {
            finish();
        }
    }

    public String getFileSize(File file) {
        long file_bytes = file.length() / 1000 * 1000;
        float size = file_bytes;
        int i = 0;
        while (size > 1000) {
            size = size / 1000;
            i++;
        }
        switch (i) {
            case 1:
                return size + " KB";
            case 2:
                return size + " MB";
            case 3:
                return size + " GB";
        }
        return file_bytes + " Bytes";
    }

    public String parseExposureTime(String input) {
        if (input == null || input.equals("null")) {
            return NO_DATA;
        }
        float f = Float.valueOf(input);
        try {
            int i = Math.round(1 / f);
            return String.valueOf(1 + "/" + i) + " sec";
        } catch (NumberFormatException e) {
            return input;
        }
    }

    public String parseFocalLength(String input) {
        if (input == null || input.equals("null")) {
            return NO_DATA;
        }
        String[] arr = input.split("/");
        if (arr.length != 2) {
            return input;
        }
        try {
            double focalLength = Double.valueOf(arr[0]) / Double.valueOf(arr[1]);
            return String.valueOf(focalLength) + " mm";
        } catch (NumberFormatException e) {
            return input;
        }
    }

    private static class InfoRecyclerViewAdapter extends RecyclerView.Adapter {
        private static String[] types = {"Filename: ", "Filepath: ", "Size: ",
                "Dimensions: ", "Date: ", "Camera model: ", "Focal length: ",
                "Exposure: ", "Aperture: ", "ISO: "};
        private String[] values;

        InfoRecyclerViewAdapter(String[] values) {
            this.values = values;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.info_item, parent, false);
            return new InfoHolder(v);
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            TextView type = (TextView) holder.itemView.findViewById(R.id.type);
            type.setText(types[position]);
            TextView value = (TextView) holder.itemView.findViewById(R.id.value);
            value.setText(values[position]);
        }

        @Override
        public int getItemCount() {
            return types.length;
        }

        static class InfoHolder extends RecyclerView.ViewHolder {
            InfoHolder(View itemView) {
                super(itemView);
            }
        }
    }
}
