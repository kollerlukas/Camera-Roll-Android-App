package us.koller.cameraroll.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ShareCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.ContextCompat;
import android.support.v4.print.PrintHelper;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import com.davemorrissey.labs.subscaleview.ImageViewState;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.item.InfoRecyclerViewAdapter;
import us.koller.cameraroll.adapter.item.viewHolder.ViewHolder;
import us.koller.cameraroll.adapter.item.ViewPagerAdapter;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.fileOperations.FileOperation;
import us.koller.cameraroll.data.fileOperations.Rename;
import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.data.Gif;
import us.koller.cameraroll.data.Photo;
import us.koller.cameraroll.data.provider.MediaProvider;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.data.Video;
import us.koller.cameraroll.util.ZoomOutPageTransformer;
import us.koller.cameraroll.util.animators.ColorFade;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.TransitionListenerAdapter;
import us.koller.cameraroll.util.Util;

public class ItemActivity extends ThemeableActivity {

    public static int FILE_OP_DIALOG_REQUEST = 1;

    public static final String ALBUM_ITEM = "ALBUM_ITEM";
    public static final String ALBUM = "ALBUM";
    public static final String ALBUM_PATH = "ALBUM_PATH";
    public static final String ITEM_POSITION = "ITEM_POSITION";
    public static final String VIEW_ONLY = "VIEW_ONLY";
    private static final String WAS_SYSTEM_UI_HIDDEN = "WAS_SYSTEM_UI_HIDDEN";
    private static final String IMAGE_VIEW_SAVED_STATE = "IMAGE_VIEW_SAVED_STATE";
    private static final String INFO_DIALOG_SHOWN = "INFO_DIALOG_SHOWN";
    public static final String SHARED_ELEMENT_RETURN_TRANSITION = "SHARED_ELEMENT_RETURN_TRANSITION";

    private Toolbar toolbar;
    private View bottomBar;
    private ViewPager viewPager;

    private AlertDialog infoDialog;
    private Menu menu;

    private boolean systemUiVisible = true;

    private Album album;
    private AlbumItem albumItem;

    public boolean view_only;

    private boolean isReturning;

    private final SharedElementCallback sharedElementCallback = new SharedElementCallback() {
        @Override
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (isReturning) {
                ViewGroup v = viewPager.findViewWithTag(albumItem.getPath());
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

    public interface ViewPagerOnInstantiateItemCallback {
        boolean onInstantiateItem(ViewHolder viewHolder);
    }

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

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(albumItem.getName());
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(new ViewPagerAdapter(album));
        viewPager.setCurrentItem(album.getAlbumItems().indexOf(albumItem), false);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            private final int color = ContextCompat.getColor(ItemActivity.this, R.color.white);

            @Override
            public void onPageSelected(int position) {
                //set new AlbumItem
                albumItem = album.getAlbumItems().get(position);
                ColorFade.fadeToolbarTitleColor(toolbar, color,
                        new ColorFade.ToolbarTitleFadeCallback() {
                            @Override
                            public void setTitle(Toolbar toolbar) {
                                toolbar.setTitle(albumItem.getName() != null ? albumItem.getName() : "");
                            }
                        });

                ViewHolder viewHolder = ((ViewPagerAdapter) viewPager.getAdapter())
                        .findViewHolderByTag(albumItem.getPath());

                onShowViewHolder(viewHolder);
            }
        });

        viewPager.setPageTransformer(true, new ZoomOutPageTransformer());

        bottomBar = findViewById(R.id.bottom_bar);
        /*ImageView delete_button = bottomBar.findViewById(R.id.delete_button);
        if (!view_only) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Drawable d = ContextCompat.getDrawable(this, R.drawable.ic_delete_avd);
                delete_button.setImageDrawable(d);
            } else {
                delete_button.setImageResource(R.drawable.ic_delete_white_24dp);
            }
        } else {
            ((View) delete_button.getParent()).setVisibility(View.GONE);
        }*/

        final ViewGroup rootView = findViewById(R.id.root_view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            rootView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
                public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                    toolbar.setPadding(toolbar.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                            toolbar.getPaddingTop() + insets.getSystemWindowInsetTop(),
                            toolbar.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                            toolbar.getPaddingBottom());

                    bottomBar.setPadding(bottomBar.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                            bottomBar.getPaddingTop(),
                            bottomBar.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                            bottomBar.getPaddingBottom() + insets.getSystemWindowInsetBottom());

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

                                    rootView.getViewTreeObserver()
                                            .removeOnGlobalLayoutListener(this);
                                }
                            });
        }

        //needed to achieve transparent navBar
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        if (view_only
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
                || savedInstanceState != null) {
            albumItem.isSharedElement = false;
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
            case R.id.open_with:
                openWith();
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
            case R.id.copy:
            case R.id.move:
                Intent intent = new Intent(this, FileOperationDialogActivity.class);
                intent.setAction(item.getItemId() == R.id.copy ?
                        FileOperationDialogActivity.ACTION_COPY :
                        FileOperationDialogActivity.ACTION_MOVE);
                intent.putExtra(FileOperationDialogActivity.FILES,
                        new String[]{albumItem.getPath()});

                startActivityForResult(intent, FILE_OP_DIALOG_REQUEST);
                break;
            case R.id.rename:
                File_POJO file = new File_POJO(albumItem.getPath(), true);
                Rename.Util.getRenameDialog(this, file, new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        final Activity a = ItemActivity.this;

                        new MediaProvider(a).loadAlbums(a,
                                Settings.getInstance(a).getHiddenFolders(),
                                new MediaProvider.Callback() {
                                    @Override
                                    public void onMediaLoaded(ArrayList<Album> albums) {
                                        //reload activity
                                        Intent intent = new Intent(a, AlbumActivity.class);
                                        intent.setAction(AlbumActivity.VIEW_ALBUM);
                                        intent.putExtra(ALBUM_PATH, album.getPath());
                                        finish();
                                        startActivity(intent);
                                    }

                                    @Override
                                    public void timeout() {
                                        onBackPressed();
                                    }

                                    @Override
                                    public void needPermission() {
                                        onBackPressed();
                                    }
                                });
                    }
                }).show();
                break;
            case R.id.delete:
                showDeleteDialog();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onShowViewHolder(ViewHolder viewHolder) {
        viewHolder.onSharedElementEnter();

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
        intent.setDataAndType(uri, MediaType.getMimeType(this, uri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivityForResult(Intent.createChooser(intent,
                    getString(R.string.set_as)), 13);
        } catch (SecurityException se) {
            Toast.makeText(this, "Error (SecurityException)", Toast.LENGTH_SHORT).show();
            se.printStackTrace();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No App found to set your photo", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void openWith() {
        Uri uri = albumItem.getUri(this);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, MediaType.getMimeType(this, uri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivityForResult(Intent.createChooser(intent,
                    getString(R.string.set_as)), 13);
        } catch (SecurityException se) {
            Toast.makeText(this, "Error (SecurityException)", Toast.LENGTH_SHORT).show();
            se.printStackTrace();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No App found to view your " + albumItem.getType(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void sharePhoto() {
        Uri uri = albumItem.getUri(this);

        Intent shareIntent = ShareCompat.IntentBuilder.from(this)
                .addStream(uri)
                .setType(MediaType.getMimeType(this, uri))
                .getIntent();

        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (shareIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_photo)));
        } else {
            Toast.makeText(this, "No App found to share your " + albumItem.getType(), Toast.LENGTH_SHORT).show();
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
            photoPrinter.printBitmap(
                    albumItem.getPath(),
                    albumItem.getUri(this));
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "Error (FileNotFoundException)", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void editPhoto() {
        Uri uri = albumItem.getUri(this);

        Intent intent = new Intent(Intent.ACTION_EDIT)
                .setDataAndType(uri, MediaType.getMimeType(this, uri))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(intent,
                        getString(R.string.edit_space) + albumItem.getType()));
            } else {
                Toast.makeText(this, "No App found to edit your "
                        + albumItem.getType(), Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException se) {
            Toast.makeText(this, "Error (SecurityException)", Toast.LENGTH_SHORT).show();
            se.printStackTrace();
        }
    }

    public void showDeleteDialog() {
        new AlertDialog.Builder(this, theme.getDialogThemeRes())
                .setTitle(getString(R.string.delete) + " " + albumItem.getType() + "?")
                .setNegativeButton(getString(R.string.no), null)
                .setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
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

        final File_POJO[] files = new File_POJO[]{new File_POJO(albumItem.getPath(), true)};

        registerLocalBroadcastReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                unregisterLocalBroadcastReceiver(this);

                switch (intent.getAction()) {
                    case FileOperation.RESULT_DONE:
                        Intent i = new Intent(AlbumActivity.ALBUM_ITEM_DELETED)
                                .putExtra(AlbumActivity.ALBUM_PATH, album.getPath())
                                .putExtra(ALBUM_ITEM, albumItem)
                                .putExtra(VIEW_ONLY, view_only);

                        ItemActivity.this.setResult(RESULT_OK, i);

                        finish();
                        break;
                    case FileOperation.FAILED:
                        //onBackPressed();
                        break;
                    default:
                        break;
                }
            }
        });
        startService(FileOperation.getDefaultIntent(this, FileOperation.DELETE, files));
    }

    public void showInfoDialog() {
        final InfoRecyclerViewAdapter adapter = new InfoRecyclerViewAdapter();
        boolean exifSupported = adapter.exifSupported(this, albumItem);

        final View rootView = LayoutInflater.from(this)
                .inflate(R.layout.info_dialog_layout,
                        (ViewGroup) findViewById(R.id.root_view), false);

        final View loadingBar = rootView.findViewById(R.id.progress_bar);
        loadingBar.setVisibility(View.VISIBLE);
        final View dialogLayout = rootView.findViewById(R.id.dialog_layout);
        dialogLayout.setVisibility(View.GONE);

        AlertDialog.Builder builder
                = new AlertDialog.Builder(this, theme.getDialogThemeRes())
                .setTitle(getString(R.string.info))
                .setView(rootView)
                .setPositiveButton(R.string.done, null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        infoDialog = null;
                    }
                });
        if (exifSupported && !view_only) {
            builder.setNeutralButton(R.string.edit_exif, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent =
                            new Intent(ItemActivity.this,
                                    ExifEditorActivity.class);
                    intent.putExtra(ExifEditorActivity.ALBUM_ITEM, albumItem);
                    startActivity(intent);
                }
            });
        }
        infoDialog = builder.create();
        infoDialog.show();

        boolean showColors = (albumItem instanceof Photo || albumItem instanceof Gif) && !view_only;
        adapter.retrieveData(albumItem, showColors,
                new InfoRecyclerViewAdapter.OnDataRetrievedCallback() {
                    @Override
                    public void onDataRetrieved() {
                        ItemActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final View scrollIndicatorTop = rootView.findViewById(R.id.scroll_indicator_top);
                                final View scrollIndicatorBottom = rootView.findViewById(R.id.scroll_indicator_bottom);

                                RecyclerView recyclerView = rootView.findViewById(R.id.recyclerView);
                                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(ItemActivity.this);
                                recyclerView.setLayoutManager(linearLayoutManager);
                                recyclerView.setAdapter(adapter);

                                recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                                    @Override
                                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                                        super.onScrolled(recyclerView, dx, dy);
                                        scrollIndicatorTop.setVisibility(
                                                recyclerView.canScrollVertically(-1) ?
                                                        View.VISIBLE : View.INVISIBLE);

                                        scrollIndicatorBottom.setVisibility(
                                                recyclerView.canScrollVertically(1) ?
                                                        View.VISIBLE : View.INVISIBLE);
                                    }
                                });

                                loadingBar.setVisibility(View.GONE);
                                dialogLayout.setVisibility(View.VISIBLE);
                            }
                        });
                    }

                    @Override
                    public Context getContext() {
                        return ItemActivity.this;
                    }
                });
    }

    public void bottomBarOnClick(final View v) {
        Drawable d = ((ImageView) v).getDrawable();
        if (d instanceof Animatable) {
            ((Animatable) d).start();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    bottomBarAction(v);
                }
            }, (int) (400 * Util.getAnimatorSpeed(this)));
        } else {
            bottomBarAction(v);
        }
    }

    private void bottomBarAction(View v) {
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
                showDeleteDialog();
                break;
            default:
                break;
        }
    }

    public void imageOnClick() {
        systemUiVisible = !systemUiVisible;
        showSystemUI(systemUiVisible);
    }

    public static void videoOnClick(Context context, AlbumItem albumItem) {
        if (!(albumItem instanceof Video)) {
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(albumItem.getUri(context), "video/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "No App found to play your video", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
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

    @Override
    public void onPermissionGranted() {
        super.onPermissionGranted();
        this.finish();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (albumItem instanceof Photo) {
            View itemView = viewPager.findViewWithTag(albumItem.getPath());
            if (itemView != null) {
                View view = itemView.findViewById(R.id.subsampling);
                if (view instanceof SubsamplingScaleImageView) {
                    SubsamplingScaleImageView imageView = (SubsamplingScaleImageView) view;
                    ImageViewState state = imageView.getState();
                    if (state != null) {
                        outState.putSerializable(IMAGE_VIEW_SAVED_STATE, state);
                    }
                }
            }
        }
        //outState.putParcelable(ALBUM, album);
        outState.putParcelable(ALBUM_ITEM, albumItem);
        outState.putBoolean(WAS_SYSTEM_UI_HIDDEN, !systemUiVisible);
        outState.putBoolean(INFO_DIALOG_SHOWN, infoDialog != null);
    }

    public interface Callback {
        void done();
    }

    @Override
    public void onBackPressed() {
        if (view_only) {
            /*if (getIntent().getBooleanExtra(FINISH_AFTER, false)) {
                this.finishAffinity();
            } else {
                this.finish();
            }*/
            this.finish();
        } else {
            showUI(false);
            ViewHolder viewHolder = ((ViewPagerAdapter)
                    viewPager.getAdapter()).findViewHolderByTag(albumItem.getPath());
            if (viewHolder != null) {
                viewHolder.onSharedElementExit(new ItemActivity.Callback() {
                    @Override
                    public void done() {
                        setResultAndFinish();
                    }
                });
            }
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
        data.setAction(SHARED_ELEMENT_RETURN_TRANSITION);
        data.putExtra(AlbumActivity.ALBUM_PATH, album.getPath());
        data.putExtra(AlbumActivity.EXTRA_CURRENT_ALBUM_POSITION,
                viewPager.getCurrentItem());
        setResult(RESULT_OK, data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition();
        } else {
            finish();
        }
    }

    @Override
    public int getDarkThemeRes() {
        return R.style.Theme_CameraRoll_PhotoView;
    }

    @Override
    public int getLightThemeRes() {
        return R.style.Theme_CameraRoll_Light_PhotoView;
    }

    @Override
    public IntentFilter getBroadcastIntentFilter() {
        return FileOperation.Util.getIntentFilter(super.getBroadcastIntentFilter());
    }
}
