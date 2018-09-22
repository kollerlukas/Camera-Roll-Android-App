package us.koller.cameraroll.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.TransitionSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.fileExplorer.FileExplorerAdapter;
import us.koller.cameraroll.data.fileOperations.Copy;
import us.koller.cameraroll.data.fileOperations.Delete;
import us.koller.cameraroll.data.fileOperations.FileOperation;
import us.koller.cameraroll.data.fileOperations.Move;
import us.koller.cameraroll.data.models.File_POJO;
import us.koller.cameraroll.data.models.StorageRoot;
import us.koller.cameraroll.data.models.VirtualAlbum;
import us.koller.cameraroll.data.provider.FilesProvider;
import us.koller.cameraroll.data.provider.Provider;
import us.koller.cameraroll.themes.Theme;
import us.koller.cameraroll.ui.widget.SwipeBackCoordinatorLayout;
import us.koller.cameraroll.util.Util;
import us.koller.cameraroll.util.animators.ColorFade;

public class FileExplorerActivity extends ThemeableActivity
        implements SwipeBackCoordinatorLayout.OnSwipeListener, FileExplorerAdapter.Callback {

    public static final String ROOTS = "ROOTS";
    public static final String CURRENT_DIR = "CURRENT_DIR";
    public static final String MODE = "MODE";
    public static final String SELECTED_ITEMS = "SELECTED_ITEMS";
    public static final String STORAGE_ROOTS = "Storage Roots";
    public static final String FILE_OPERATION = "FILE_OPERATION";

    private File_POJO roots;

    private File_POJO currentDir;

    private FilesProvider filesProvider;

    private RecyclerView recyclerView;
    private FileExplorerAdapter recyclerViewAdapter;

    private Menu menu;

    private Intent fileOpIntent;

    public interface OnDirectoryChangeCallback {
        void changeDir(String path);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_explorer);

        currentDir = new File_POJO("", false);

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

        final Toolbar toolbar = findViewById(R.id.toolbar);

        toolbar.setBackgroundColor(toolbarColor);
        toolbar.setTitleTextColor(textColorPrimary);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && showAnimations()) {
            AnimatedVectorDrawable drawable = (AnimatedVectorDrawable)
                    ContextCompat.getDrawable(FileExplorerActivity.this, R.drawable.back_to_cancel_avd);
            //mutating avd to reset it
            drawable.mutate();
            toolbar.setNavigationIcon(drawable);
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);

        }
        Drawable navIcon = toolbar.getNavigationIcon();
        if (navIcon != null) {
            navIcon = DrawableCompat.wrap(navIcon);
            DrawableCompat.setTint(navIcon.mutate(), textColorSecondary);
            toolbar.setNavigationIcon(navIcon);
        }

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.file_explorer));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Util.colorToolbarOverflowMenuIcon(toolbar, textColorSecondary);

        //need to be called after setTitle(), to ensure, that mTitleTextView exists
        final TextView titleTextView = Util.setToolbarTypeface(toolbar);
        if (titleTextView != null) {
            titleTextView.setEllipsize(TextUtils.TruncateAt.START);
        }

        final ViewGroup rootView = findViewById(R.id.swipeBackView);
        if (rootView instanceof SwipeBackCoordinatorLayout) {
            ((SwipeBackCoordinatorLayout) rootView).setOnSwipeListener(this);
        }

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAdapter = new FileExplorerAdapter(this::loadDirectory, this);
        if (savedInstanceState != null && savedInstanceState.containsKey(CURRENT_DIR)) {
            recyclerViewAdapter.setFiles(currentDir);
        }
        recyclerViewAdapter.notifyDataSetChanged();
        recyclerView.setAdapter(recyclerViewAdapter);

        //setup fab
        final FloatingActionButton fab = findViewById(R.id.fab);
        fab.setImageResource(R.drawable.ic_create_new_folder_white);
        Drawable d = fab.getDrawable();
        d = DrawableCompat.wrap(d);
        DrawableCompat.setTint(d.mutate(), accentTextColor);
        fab.setImageDrawable(d);
        fab.setScaleX(0.0f);
        fab.setScaleY(0.0f);

        //setting window insets manually
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            rootView.setOnApplyWindowInsetsListener((view, insets) -> {
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
                        recyclerView.getPaddingTop() + insets.getSystemWindowInsetTop(),
                        recyclerView.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                        recyclerView.getPaddingBottom() + insets.getSystemWindowInsetBottom());

                fab.setTranslationY(-insets.getSystemWindowInsetBottom());
                fab.setTranslationX(-insets.getSystemWindowInsetRight());

                // clear this listener so insets aren't re-applied
                rootView.setOnApplyWindowInsetsListener(null);
                return insets.consumeSystemWindowInsets();
            });
        } else {
            rootView.getViewTreeObserver()
                    .addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    // hacky way of getting window insets on pre-Lollipop
                                    // somewhat works...
                                    int[] screenSize = Util.getScreenSize(FileExplorerActivity.this);

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

                                    fab.setTranslationY(-windowInsets[2]);
                                    fab.setTranslationX(-windowInsets[3]);

                                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                }
                            });
        }

        //needed to achieve transparent navBar
        setSystemUiFlags();

        //load files
        if (savedInstanceState != null
                && savedInstanceState.containsKey(CURRENT_DIR)
                && savedInstanceState.containsKey(ROOTS)) {
            roots = savedInstanceState.getParcelable(ROOTS);
            currentDir = savedInstanceState.getParcelable(CURRENT_DIR);

            recyclerViewAdapter.setFiles(currentDir);
            recyclerViewAdapter.notifyDataSetChanged();
            onDataChanged();

            if (savedInstanceState.containsKey(MODE)) {
                int mode = savedInstanceState.getInt(MODE);

                if (mode == FileExplorerAdapter.SELECTOR_MODE) {
                    if (savedInstanceState.containsKey(SELECTED_ITEMS)) {
                        final File_POJO[] selectedItems
                                = (File_POJO[]) savedInstanceState.getParcelableArray(SELECTED_ITEMS);
                        if (selectedItems != null) {
                            rootView.getViewTreeObserver().addOnGlobalLayoutListener(
                                    new ViewTreeObserver.OnGlobalLayoutListener() {
                                        @Override
                                        public void onGlobalLayout() {
                                            rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                            recyclerViewAdapter.enterSelectorMode(selectedItems);
                                        }
                                    });

                        }
                    }
                } else if (mode == FileExplorerAdapter.PICK_TARGET_MODE &&
                        savedInstanceState.containsKey(FILE_OPERATION)) {
                    onSelectorModeEnter();
                    //fileOp = savedInstanceState.getParcelable(FILE_OPERATION);
                        /*FileOperation.operation = fileOp != null ?
                                fileOp.getType() : FileOperation.EMPTY;*/
                    //need to call pick target after onSelectorModeEnter animation are done
                    new Handler().postDelayed(() -> recyclerViewAdapter.pickTarget(),
                            (int) (500 * Util.getAnimatorSpeed(this)));
                }
            }

        } else {
            loadRoots();

            //show warning dialog
            /*new AlertDialog.Builder(this, getDialogThemeRes())
                    .setTitle(R.string.warning)
                    .setMessage(Html.fromHtml(getString(R.string.file_explorer_warning_message)))
                    .setPositiveButton(R.string.ok, null)
                    .setNegativeButton(R.string.cancel, (dialogInterface, i) -> finish())
                    .show();*/
        }
    }

    public void loadRoots() {
        StorageRoot[] storageRoots = FilesProvider.getRoots(this);
        roots = new StorageRoot(STORAGE_ROOTS);
        for (StorageRoot storageRoot : storageRoots) {
            roots.addChild(storageRoot);
        }

        FileExplorerActivity.this.currentDir = roots;
        if (recyclerViewAdapter != null) {
            recyclerViewAdapter.setFiles(currentDir);
            recyclerViewAdapter.notifyDataSetChanged();
            onDataChanged();
        }
    }

    public void loadDirectory(final String path) {
        Log.d("FileExplorerActivity", "loadDirectory(): " + path);
        final Snackbar snackbar = Snackbar.make(findViewById(R.id.root_view),
                getString(R.string.loading), Snackbar.LENGTH_INDEFINITE);
        Util.showSnackbar(snackbar);

        final FilesProvider.Callback callback = new FilesProvider.Callback() {
            @Override
            public void onDirLoaded(final File_POJO dir) {
                runOnUiThread(() -> {
                    filesProvider.onDestroy();
                    filesProvider = null;

                    if (dir != null) {
                        FileExplorerActivity.this.currentDir = dir;
                        if (recyclerViewAdapter != null) {
                            recyclerViewAdapter.setFiles(currentDir);
                            recyclerViewAdapter.notifyDataSetChanged();
                            onDataChanged();
                        }
                    }

                    snackbar.dismiss();
                });
            }

            @Override
            public void timeout() {
                runOnUiThread(() -> {
                    snackbar.dismiss();

                    final Snackbar snackbar1 = Snackbar.make(findViewById(R.id.root_view),
                            R.string.loading_failed, Snackbar.LENGTH_INDEFINITE);
                    snackbar1.setAction(getString(R.string.retry), view -> loadDirectory(path));
                    Util.showSnackbar(snackbar1);
                });
            }

            @Override
            public void needPermission() {
                runOnUiThread(snackbar::dismiss);
            }
        };

        filesProvider = new FilesProvider(this);
        filesProvider.loadDir(this, path, callback);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ROOTS, roots);
        if (currentDir != null) {
            outState.putParcelable(CURRENT_DIR, currentDir);
        }

        outState.putInt(MODE, recyclerViewAdapter.getMode());

        File_POJO[] selectedItems = recyclerViewAdapter.getSelectedItems();
        if (selectedItems.length > 0) {
            outState.putParcelableArray(SELECTED_ITEMS, selectedItems);
        }

        /*if (fileOp != null) {
            outState.putParcelable(FILE_OPERATION, fileOp);
            File_POJO[] files = FileOperation.getFiles(fileOpIntent);
            outState.putParcelableArray(SELECTED_ITEMS, files);
        }*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_explorer, menu);
        this.menu = menu;
        //hide menu items; items are made visible, when a folder gets selected
        manageMenuItems();

        Drawable icon = menu.findItem(R.id.paste).getIcon().mutate();
        icon = DrawableCompat.wrap(icon);
        DrawableCompat.setTint(icon.mutate(), accentTextColor);
        menu.findItem(R.id.paste).setIcon(icon);

        return super.onCreateOptionsMenu(menu);
    }

    public void manageMenuItems() {
        if (menu != null) {
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                switch (item.getItemId()) {
                    case R.id.exclude:
                        if (currentDir != null) {
                            item.setVisible(!currentDir.getPath().equals(STORAGE_ROOTS));
                            if (Provider.isPathPermanentlyExcluded(currentDir.getPath())) {
                                item.setChecked(true);
                                item.setEnabled(false);
                            } else {
                                item.setChecked(currentDir.excluded);
                                item.setEnabled(!currentDir.getPath().equals(STORAGE_ROOTS)
                                        && Provider.isDirExcludedBecauseParentDirIsExcluded(
                                        currentDir.getPath(), Provider.getExcludedPaths()));
                            }
                        } else {
                            item.setVisible(true);
                            item.setChecked(false);
                            item.setEnabled(false);
                        }
                        break;
                    case R.id.scan:
                        item.setVisible(!currentDir.getPath().equals(STORAGE_ROOTS));
                        break;
                    case R.id.add_to_virtual_album:
                        item.setVisible(!currentDir.getPath().equals(STORAGE_ROOTS));
                        break;
                    default:
                        item.setVisible(false);
                        break;
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (recyclerViewAdapter.isModeActive() || recyclerViewAdapter.getMode() ==
                        FileExplorerAdapter.PICK_TARGET_MODE) {
                    recyclerViewAdapter.cancelMode();
                } else {
                    onBackPressed();
                }
                break;
            case R.id.exclude:
                currentDir.excluded = !currentDir.excluded;
                item.setChecked(currentDir.excluded);
                if (currentDir.excluded) {
                    FilesProvider.addExcludedPath(this, currentDir.getPath());
                } else {
                    FilesProvider.removeExcludedPath(this, currentDir.getPath());
                }
                break;
            case R.id.scan:
                AsyncTask.execute(() -> {
                    ArrayList<String> paths = FileOperation.Util
                            .getAllChildPaths(new ArrayList<>(), currentDir.getPath());
                    String[] pathsArray = new String[paths.size()];
                    paths.toArray(pathsArray);
                    FileOperation.Util.scanPathsWithNotification(FileExplorerActivity.this, pathsArray);
                });
                break;
            case R.id.add_to_virtual_album:
                String path = currentDir.getPath();
                AlertDialog dialog = VirtualAlbum.Util.getAddToVirtualAlbumDialog(this, path);
                dialog.show();
                break;
            case R.id.paste:
                if (!currentDir.getPath().equals(STORAGE_ROOTS)) {
                    recyclerViewAdapter.cancelMode();
                    if (fileOpIntent != null) {
                        File_POJO target = recyclerViewAdapter.getFiles();
                        fileOpIntent.putExtra(FileOperation.TARGET, target);
                        startService(fileOpIntent);
                        fileOpIntent = null;
                    }
                } else {
                    Toast.makeText(this, R.string.paste_error, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.copy:
                fileOpIntent = new Intent(this, Copy.class)
                        .setAction(FileOperation.Util.getActionString(this, FileOperation.COPY));
                recyclerViewAdapter.cancelMode();
                break;
            case R.id.move:
                fileOpIntent = new Intent(this, Move.class)
                        .setAction(FileOperation.Util.getActionString(this, FileOperation.MOVE));
                recyclerViewAdapter.cancelMode();
                break;
            case R.id.delete:
                fileOpIntent = new Intent(this, Delete.class)
                        .setAction(FileOperation.Util.getActionString(this, FileOperation.DELETE));
                recyclerViewAdapter.cancelMode();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void fabClicked(View v) {
        if (showAnimations()) {
            animateFab(false);
        }

        View dialogLayout = LayoutInflater.from(this).inflate(R.layout.input_dialog_layout,
                findViewById(R.id.root_view), false);

        final EditText editText = dialogLayout.findViewById(R.id.edit_text);

        AlertDialog dialog = new AlertDialog.Builder(this, theme.getDialogThemeRes())
                .setTitle(R.string.new_folder)
                .setView(dialogLayout)
                .setPositiveButton(R.string.create, (dialogInterface, i) -> {
                    String filename = editText.getText().toString();
                    File_POJO newFolder = new File_POJO(currentDir.getPath()
                            + "/" + filename, false);

                    File_POJO[] files = new File_POJO[]{newFolder};

                    Intent intent = FileOperation.getDefaultIntent(
                            FileExplorerActivity.this,
                            FileOperation.NEW_DIR,
                            files);
                    startService(intent);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .setOnDismissListener(dialogInterface -> animateFab(true))
                .create();
        //noinspection ConstantConditions
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    public void animateFab(final boolean show) {
        final FloatingActionButton fab = findViewById(R.id.fab);

        if ((fab.getScaleX() == 1.0f && show) || (fab.getScaleX() == 0.0f && !show)) {
            return;
        }

        if (show) {
            fab.setOnClickListener(this::fabClicked);
        } else {
            fab.setOnClickListener(null);
        }

        fab.animate()
                .scaleX(show ? 1.0f : 0.0f)
                .scaleY(show ? 1.0f : 0.0f)
                .alpha(show ? 1.0f : 0.0f)
                .setDuration(250)
                .start();
    }

    @Override
    public void onBackPressed() {
        if (recyclerViewAdapter.isModeActive()) {
            recyclerViewAdapter.cancelMode();
        } else if (currentDir != null && !currentDir.getPath().equals(STORAGE_ROOTS)) {
            if (!isCurrentFileARoot()) {
                String path = currentDir.getPath();
                int index = path.lastIndexOf("/");
                String parentPath = path.substring(0, index);

                loadDirectory(parentPath);
            } else {
                loadRoots();
            }
        } else {
            //setResult(RESULT_OK, new Intent(MainActivity.REFRESH_MEDIA));
            super.onBackPressed();
        }
    }

    private boolean isCurrentFileARoot() {
        if (currentDir != null) {
            if (currentDir.getPath().equals(STORAGE_ROOTS)) {
                return true;
            }

            for (int i = 0; i < roots.getChildren().size(); i++) {
                if (currentDir.getPath().equals(roots.getChildren().get(i).getPath())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Provider.saveExcludedPaths(this);

        if (filesProvider != null) {
            filesProvider.onDestroy();
        }
    }

    @Override
    public boolean canSwipeBack(int dir) {
        return SwipeBackCoordinatorLayout.canSwipeBackForThisView(recyclerView, dir);
    }

    @Override
    public void onSwipeProcess(float percent) {
        getWindow().getDecorView().setBackgroundColor(
                SwipeBackCoordinatorLayout.getBackgroundColor(percent));
        boolean selectorModeActive = ((FileExplorerAdapter) recyclerView.getAdapter()).isModeActive();
        if (!theme.darkStatusBarIcons() && selectorModeActive) {
            SwipeBackCoordinatorLayout layout = findViewById(R.id.swipeBackView);
            Toolbar toolbar = findViewById(R.id.toolbar);
            View rootView = findViewById(R.id.root_view);
            int translationY = (int) layout.getTranslationY();
            int statusBarHeight = toolbar.getPaddingTop();
            if (translationY > statusBarHeight * 0.5) {
                Util.setLightStatusBarIcons(rootView);
            } else {
                Util.setDarkStatusBarIcons(rootView);
            }
        }
    }

    @Override
    public void onSwipeFinish(int dir) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setReturnTransition(new TransitionSet()
                    .setOrdering(TransitionSet.ORDERING_TOGETHER)
                    .addTransition(new Slide(dir > 0 ? Gravity.TOP : Gravity.BOTTOM))
                    .addTransition(new Fade())
                    .setInterpolator(new AccelerateDecelerateInterpolator()));
        }
        this.finish();
    }

    @Override
    public void onSelectorModeEnter() {
        fileOpIntent = null;

        final Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setActivated(true);
        toolbar.animate().translationY(0.0f).start();

        if (theme.darkStatusBarIconsInSelectorMode()) {
            Util.setDarkStatusBarIcons(findViewById(R.id.root_view));
        } else {
            Util.setLightStatusBarIcons(findViewById(R.id.root_view));
        }

        ColorDrawable statusBarOverlay = getStatusBarOverlay();
        if (statusBarOverlay != null) {
            ColorFade.fadeDrawableAlpha(statusBarOverlay, 0);
        }

        ColorFade.fadeBackgroundColor(toolbar, toolbarColor, accentColor);

        ColorFade.fadeToolbarTitleColor(toolbar, accentTextColor, null);

        //fade overflow menu icon
        ColorFade.fadeDrawableColor(toolbar.getOverflowIcon(),
                textColorSecondary, accentTextColor);

        Drawable navIcon = toolbar.getNavigationIcon();
        if (navIcon instanceof Animatable) {
            ((Animatable) navIcon).start();
            ColorFade.fadeDrawableColor(navIcon,
                    textColorSecondary, accentTextColor);
        }
        new Handler().postDelayed(() -> {
            Drawable d;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AnimatedVectorDrawable drawable = (AnimatedVectorDrawable)
                        ContextCompat.getDrawable(FileExplorerActivity.this,
                                R.drawable.cancel_to_back_avd);
                //mutating avd to reset it
                drawable.mutate();
                d = drawable;
            } else {
                d = ContextCompat.getDrawable(FileExplorerActivity.this,
                        R.drawable.ic_clear_white);
            }
            d = DrawableCompat.wrap(d);
            DrawableCompat.setTint(d.mutate(), accentTextColor);
            toolbar.setNavigationIcon(d);

            //make menu items visible
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                switch (item.getItemId()) {
                    case R.id.copy:
                    case R.id.move:
                    case R.id.delete:
                        item.setVisible(true);
                        break;
                    default:
                        item.setVisible(false);
                        break;
                }
            }
        }, navIcon instanceof Animatable ? (int) (500 * Util.getAnimatorSpeed(this)) : 0);
    }

    @Override
    public void onSelectorModeExit(final File_POJO[] selected_items) {
        if (fileOpIntent != null) {
            fileOpIntent.putExtra(FileOperation.FILES, selected_items);
            switch (FileOperation.Util.getActionInt(this, fileOpIntent.getAction())) {
                case FileOperation.DELETE:
                    resetToolbar();

                    String title;
                    int count = selected_items.length;
                    if (count == 1) {
                        title = getString(R.string.delete_file, count);
                    } else {
                        title = getString(R.string.delete_files, count);
                    }

                    new AlertDialog.Builder(this, theme.getDialogThemeRes())
                            .setTitle(title)
                            .setNegativeButton(getString(R.string.no), null)
                            .setPositiveButton(getString(R.string.delete), (dialogInterface, i) -> {
                                startService(fileOpIntent);
                                fileOpIntent = null;
                            })
                            .create()
                            .show();
                    break;
                case FileOperation.COPY:
                case FileOperation.MOVE:
                    recyclerViewAdapter.pickTarget();
                    break;
                default:
                    break;
            }
        }

        if (fileOpIntent == null) {
            resetToolbar();
        }
    }

    @Override
    public void onItemSelected(int count) {
        if (count != 0) {
            Toolbar toolbar = findViewById(R.id.toolbar);
            final String title = getString(R.string.selected_count, count);
            ColorFade.fadeToolbarTitleColor(toolbar, accentTextColor, toolbar1 -> toolbar1.setTitle(title));
        }
    }

    @Override
    public void onPickTargetModeEnter() {
        final Toolbar toolbar = findViewById(R.id.toolbar);
        if (fileOpIntent != null) {
            final int count = FileOperation.getFiles(fileOpIntent).length;

            ColorFade.fadeToolbarTitleColor(toolbar, accentTextColor, toolbar1 -> {
                String title = "";
                int action = FileOperation.Util.getActionInt(
                        FileExplorerActivity.this, fileOpIntent.getAction());
                switch (action) {
                    case FileOperation.COPY:
                        if (count == 1) {
                            title = getString(R.string.copy_file, count);
                        } else {
                            title = getString(R.string.copy_files, count);
                        }
                        break;
                    case FileOperation.MOVE:
                        if (count == 1) {
                            title = getString(R.string.move_file, count);
                        } else {
                            title = getString(R.string.move_files, count);
                        }
                        break;
                }
                toolbar1.setTitle(title);
            });
        }

        animateFab(true);

        new Handler().postDelayed(() -> {
            //hide menu items
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                switch (item.getItemId()) {
                    case R.id.paste:
                        item.setVisible(true);
                        break;
                    default:
                        item.setVisible(false);
                        break;
                }
            }
        }, (int) (300 * Util.getAnimatorSpeed(this)));
    }

    @Override
    public void onPickTargetModeExit() {
        animateFab(false);
        resetToolbar();
    }

    @Override
    public void onDataChanged() {
        final View emptyState = findViewById(R.id.empty_state_text);
        emptyState.animate()
                .alpha(currentDir.getChildren().size() == 0 ? 1.0f : 0.0f)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        emptyState.setVisibility(
                                currentDir.getChildren().size() == 0 ?
                                        View.VISIBLE : View.GONE);
                    }
                })
                .setDuration(100)
                .start();

        if (recyclerViewAdapter.getMode() == FileExplorerAdapter.NORMAL_MODE) {
            final Toolbar toolbar = findViewById(R.id.toolbar);

            ColorFade.fadeToolbarTitleColor(toolbar, textColorPrimary,
                    toolbar1 -> toolbar1.setTitle(currentDir.getPath()));
        }

        if (recyclerViewAdapter.getMode() == FileExplorerAdapter.NORMAL_MODE) {
            manageMenuItems();
        }
    }

    public void resetToolbar() {
        final Toolbar toolbar = findViewById(R.id.toolbar);

        if (theme.darkStatusBarIcons()) {
            Util.setDarkStatusBarIcons(findViewById(R.id.root_view));
        } else {
            Util.setLightStatusBarIcons(findViewById(R.id.root_view));
        }

        ColorDrawable statusBarOverlay = getStatusBarOverlay();
        if (statusBarOverlay != null) {
            int alpha = Color.alpha(getStatusBarColor());
            ColorFade.fadeDrawableAlpha(statusBarOverlay, alpha);
        }

        toolbar.setActivated(theme.elevatedToolbar());

        ColorFade.fadeBackgroundColor(toolbar, accentColor, toolbarColor);
        ColorFade.fadeToolbarTitleColor(toolbar, textColorPrimary,
                toolbar1 -> toolbar1.setTitle(currentDir.getPath()));

        //fade overflow menu icon
        ColorFade.fadeDrawableColor(toolbar.getOverflowIcon(), accentTextColor, textColorSecondary);

        Drawable navIcon = toolbar.getNavigationIcon();
        if (navIcon instanceof Animatable) {
            ((Animatable) navIcon).start();
            ColorFade.fadeDrawableColor(navIcon, accentTextColor, textColorSecondary);
        }
        new Handler().postDelayed(() -> {
            Drawable d;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AnimatedVectorDrawable drawable = (AnimatedVectorDrawable)
                        ContextCompat.getDrawable(FileExplorerActivity.this,
                                R.drawable.back_to_cancel_avd);
                //mutating avd to reset it
                drawable.mutate();
                d = drawable;
            } else {
                d = ContextCompat.getDrawable(FileExplorerActivity.this,
                        R.drawable.ic_arrow_back_white);
            }
            d = DrawableCompat.wrap(d);
            DrawableCompat.setTint(d.mutate(), textColorSecondary);
            toolbar.setNavigationIcon(d);

            //hide menu items
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                switch (item.getItemId()) {
                    case R.id.exclude:
                    case R.id.scan:
                        item.setVisible(true);
                        break;
                    default:
                        item.setVisible(false);
                        break;
                }
            }
        }, navIcon instanceof Animatable ? (int) (500 * Util.getAnimatorSpeed(this)) : 0);
    }

    @Override
    public int getDarkThemeRes() {
        return R.style.CameraRoll_Theme_Translucent_FileExplorer;
    }

    @Override
    public int getLightThemeRes() {
        return R.style.CameraRoll_Theme_Light_Translucent_FileExplorer;
    }

    @Override
    public void onThemeApplied(Theme theme) {
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setBackgroundTintList(ColorStateList.valueOf(accentColor));

        if (theme.darkStatusBarIcons()) {
            Util.setDarkStatusBarIcons(findViewById(R.id.root_view));
        } else {
            Util.setLightStatusBarIcons(findViewById(R.id.root_view));
        }

        final Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setActivated(theme.elevatedToolbar());

        if (theme.statusBarOverlay()) {
            addStatusBarOverlay(toolbar);
        }
    }

    @Override
    public BroadcastReceiver getDefaultLocalBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case FileOperation.RESULT_DONE:
                        loadDirectory(currentDir.getPath());
                        break;
                    case FileOperation.FAILED:
                        break;
                    default:
                        break;
                }
            }
        };
    }

    @Override
    public IntentFilter getBroadcastIntentFilter() {
        return FileOperation.Util.getIntentFilter(super.getBroadcastIntentFilter());
    }
}
