package us.koller.cameraroll.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
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
import android.view.WindowInsets;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.Toast;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.fileExplorer.RecyclerViewAdapter;
import us.koller.cameraroll.data.FileOperations.Copy;
import us.koller.cameraroll.data.FileOperations.Delete;
import us.koller.cameraroll.data.FileOperations.FileOperation;
import us.koller.cameraroll.data.FileOperations.Move;
import us.koller.cameraroll.data.FileOperations.NewDirectory;
import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.data.Provider.FilesProvider;
import us.koller.cameraroll.data.Provider.Provider;
import us.koller.cameraroll.data.StorageRoot;
import us.koller.cameraroll.ui.widget.ParallaxImageView;
import us.koller.cameraroll.ui.widget.SwipeBackCoordinatorLayout;
import us.koller.cameraroll.util.animators.ColorFade;
import us.koller.cameraroll.util.Util;

public class FileExplorerActivity extends ThemeableActivity
        implements SwipeBackCoordinatorLayout.OnSwipeListener, RecyclerViewAdapter.Callback {

    public interface OnDirectoryChangeCallback {
        void changeDir(String path);
    }

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
    private RecyclerViewAdapter recyclerViewAdapter;

    private Menu menu;

    private FileOperation fileOperation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_explorer);

        currentDir = new File_POJO("", false);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(ContextCompat.getColor(this, toolbar_color_res));
        toolbar.setTitleTextColor(ContextCompat.getColor(this, text_color_res));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AnimatedVectorDrawable drawable = (AnimatedVectorDrawable)
                    ContextCompat.getDrawable(FileExplorerActivity.this, R.drawable.back_to_cancel_avd);
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

        setSupportActionBar(toolbar);

        Util.colorToolbarOverflowMenuIcon(toolbar,
                ContextCompat.getColor(this, text_color_secondary_res));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.file_explorer));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final ViewGroup rootView = (ViewGroup) findViewById(R.id.swipeBackView);
        if (rootView instanceof SwipeBackCoordinatorLayout) {
            ((SwipeBackCoordinatorLayout) rootView).setOnSwipeListener(this);
        }

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setTag(ParallaxImageView.RECYCLER_VIEW_TAG);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAdapter = new RecyclerViewAdapter(
                new OnDirectoryChangeCallback() {
                    @Override
                    public void changeDir(String path) {
                        loadDirectory(path);
                    }
                }, this);
        if (savedInstanceState != null && savedInstanceState.containsKey(CURRENT_DIR)) {
            recyclerViewAdapter.setFiles(currentDir);
        }
        recyclerViewAdapter.notifyDataSetChanged();
        recyclerView.setAdapter(recyclerViewAdapter);

        //setup fab
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setImageResource(R.drawable.ic_create_new_folder_white_24dp);
        Drawable d = fab.getDrawable();
        d = DrawableCompat.wrap(d);
        DrawableCompat.setTint(d.mutate(),
                ContextCompat.getColor(this, R.color.grey_900_translucent));
        fab.setImageDrawable(d);
        fab.setScaleX(0.0f);
        fab.setScaleY(0.0f);

        //setting window insets manually
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            rootView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
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
                            recyclerView.getPaddingTop() /*+ insets.getSystemWindowInsetTop()*/,
                            recyclerView.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                            recyclerView.getPaddingBottom() + insets.getSystemWindowInsetBottom());

                    ViewGroup.MarginLayoutParams fabParams
                            = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
                    fabParams.rightMargin += insets.getSystemWindowInsetRight();
                    fabParams.bottomMargin += insets.getSystemWindowInsetBottom();
                    fab.setLayoutParams(fabParams);

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

                                    ViewGroup.MarginLayoutParams fabParams
                                            = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
                                    fabParams.rightMargin += windowInsets[2];
                                    fabParams.bottomMargin += windowInsets[3];
                                    fab.setLayoutParams(fabParams);

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

        //needed to achieve transparent navBar
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

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

                if (mode == RecyclerViewAdapter.SELECTOR_MODE) {
                    if (savedInstanceState.containsKey(SELECTED_ITEMS)) {
                        File_POJO[] selectedItems
                                = (File_POJO[]) savedInstanceState.getParcelableArray(SELECTED_ITEMS);
                        if (selectedItems != null) {
                            recyclerViewAdapter.enterSelectorMode(selectedItems);
                        }
                    }
                } else if (mode == RecyclerViewAdapter.PICK_TARGET_MODE) {
                    if (savedInstanceState.containsKey(FILE_OPERATION)) {
                        onSelectorModeEnter();
                        fileOperation = savedInstanceState.getParcelable(FILE_OPERATION);
                        FileOperation.operation = fileOperation.getType();
                        //need to call pick target after onSelectorModeEnter animation are done
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                recyclerViewAdapter.pickTarget();
                            }
                        }, (int) (500 * Util.getAnimatorSpeed(this)));
                    }
                }
            }

        } else {
            loadRoots();

            //show warning dialog
            new AlertDialog.Builder(this, getDialogThemeRes())
                    .setTitle(R.string.warning)
                    .setMessage(Html.fromHtml(getString(R.string.file_explorer_warning_message)))
                    .setPositiveButton(R.string.ok, null)
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    })
                    .show();
        }
    }

    public void loadRoots() {
        StorageRoot[] storageRoots = FilesProvider.getRoots(this);
        roots = new StorageRoot(STORAGE_ROOTS);
        for (int i = 0; i < storageRoots.length; i++) {
            roots.addChild(storageRoots[i]);
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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
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
                    }
                });
            }

            @Override
            public void timeout() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        snackbar.dismiss();

                        final Snackbar snackbar = Snackbar.make(findViewById(R.id.root_view),
                                R.string.loading_failed, Snackbar.LENGTH_INDEFINITE);
                        snackbar.setAction(getString(R.string.retry), new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                loadDirectory(path);
                            }
                        });
                        Util.showSnackbar(snackbar);
                    }
                });
            }

            @Override
            public void needPermission() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        snackbar.dismiss();
                    }
                });
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

        if (fileOperation != null) {
            outState.putParcelable(FILE_OPERATION, fileOperation);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_explorer, menu);
        this.menu = menu;
        //hide menu items; items are made visible, when a folder gets selected
        manageMenuItems();

        Drawable icon = menu.findItem(R.id.paste).getIcon().mutate();
        //icon.setTint(ContextCompat.getColor(FileExplorerActivity.this, R.color.grey_900_translucent));
        icon = DrawableCompat.wrap(icon);
        DrawableCompat.setTint(icon.mutate(),
                ContextCompat.getColor(this, R.color.grey_900_translucent));
        menu.findItem(R.id.paste).setIcon(icon);

        return super.onCreateOptionsMenu(menu);
    }

    public void manageMenuItems() {
        if (menu != null) {
            for (int i = 0; i < menu.size(); i++) {
                menu.getItem(i).setVisible(false);

                int id = menu.getItem(i).getItemId();
                if (id == R.id.exclude) {
                    if (currentDir != null) {
                        menu.getItem(i).setVisible(!isCurrentFileARoot());
                        if (Provider.isPathPermanentlyExcluded(currentDir.getPath())) {
                            menu.getItem(i).setChecked(true);
                            menu.getItem(i).setEnabled(false);
                        } else {
                            menu.getItem(i).setChecked(!isCurrentFileARoot() && currentDir.excluded);
                            menu.getItem(i).setEnabled(!isCurrentFileARoot()
                                    && !Provider.isDirExcludedBecauseParentDirIsExcluded(
                                    currentDir.getPath(), Provider.getExcludedPaths()));
                        }
                    } else {
                        menu.getItem(i).setVisible(true);
                        menu.getItem(i).setChecked(false);
                        menu.getItem(i).setEnabled(false);
                    }
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (recyclerViewAdapter.isModeActive()
                        || recyclerViewAdapter.getMode()
                        == RecyclerViewAdapter.PICK_TARGET_MODE) {
                    FileOperation.operation = FileOperation.EMPTY;
                    recyclerViewAdapter.cancelMode();
                } else {
                    onBackPressed();
                }
                break;
            case R.id.exclude:
                currentDir.excluded = !currentDir.excluded;
                Log.d("FileExplorerActivity", "onOptionsItemSelected: " + currentDir.getPath()
                        + "; " + String.valueOf(currentDir.excluded));
                item.setChecked(currentDir.excluded);
                if (currentDir.excluded) {
                    FilesProvider.addExcludedPath(this, currentDir.getPath());
                } else {
                    FilesProvider.removeExcludedPath(this, currentDir.getPath());
                }
                break;
            case R.id.paste:
                if (!currentDir.getPath().equals(STORAGE_ROOTS)) {
                    recyclerViewAdapter.cancelMode();
                    if (fileOperation != null) {
                        fileOperation.execute(this,
                                recyclerViewAdapter.getFiles(),
                                new FileOperation.Callback() {
                                    @Override
                                    public void done() {
                                        loadDirectory(currentDir.getPath());
                                    }

                                    @Override
                                    public void failed(String path) {

                                    }
                                });
                    }
                } else {
                    Toast.makeText(this, "You can't "
                            + FileOperation.getModeString(this)
                            + " files here!", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.move:
                FileOperation.operation = FileOperation.MOVE;
                recyclerViewAdapter.cancelMode();
                break;
            case R.id.copy:
                FileOperation.operation = FileOperation.COPY;
                recyclerViewAdapter.cancelMode();
                break;
            case R.id.delete:
                FileOperation.operation = FileOperation.DELETE;
                recyclerViewAdapter.cancelMode();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void fabClicked(View v) {
        animateFab(false);

        View dialogLayout = LayoutInflater.from(this).inflate(R.layout.new_folder_dialog,
                (ViewGroup) findViewById(R.id.root_view), false);

        final EditText editText = (EditText) dialogLayout.findViewById(R.id.edit_text);

        new AlertDialog.Builder(this, getDialogThemeRes())
                .setTitle(R.string.new_folder)
                .setView(dialogLayout)
                .setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String filename = editText.getText().toString();
                        File_POJO newFolder = new File_POJO(currentDir.getPath()
                                + "/" + filename, false);
                        new NewDirectory(new File_POJO[]{newFolder})
                                .execute(FileExplorerActivity.this,
                                        null,
                                        new FileOperation.Callback() {
                                            @Override
                                            public void done() {
                                                loadDirectory(currentDir.getPath());
                                            }

                                            @Override
                                            public void failed(String path) {

                                            }
                                        });
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        animateFab(true);
                    }
                })
                .create().show();
    }

    public void animateFab(final boolean show) {
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        if ((fab.getScaleX() == 1.0f && show)
                || (fab.getScaleX() == 0.0f && !show)) {
            return;
        }

        if (show) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fabClicked(view);
                }
            });
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
            setResult(RESULT_OK, new Intent(MainActivity.REFRESH_MEDIA));
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
        fileOperation = null;
        FileOperation.operation = FileOperation.EMPTY;

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(ContextCompat.getColor(this,
                android.R.color.transparent));
        toolbar.setActivated(true);
        toolbar.animate().translationY(0.0f).start();

        Util.setDarkStatusBarIcons(findViewById(R.id.root_view));

        ColorFade.fadeBackgroundColor(toolbar,
                ContextCompat.getColor(this, toolbar_color_res),
                ContextCompat.getColor(this, accent_color_res));

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
                            ContextCompat.getDrawable(FileExplorerActivity.this, R.drawable.cancel_to_back_avd);
                    //mutating avd to reset it
                    drawable.mutate();
                    d = drawable;
                } else {
                    d = ContextCompat.getDrawable(FileExplorerActivity.this, R.drawable.ic_arrow_back_white_24dp);
                }
                d = DrawableCompat.wrap(d);
                DrawableCompat.setTint(d.mutate(),
                        ContextCompat.getColor(FileExplorerActivity.this,
                                R.color.grey_900_translucent));
                toolbar.setNavigationIcon(d);

                //make menu items visible
                for (int i = 0; i < menu.size(); i++) {
                    int id = menu.getItem(i).getItemId();
                    if (id == R.id.paste || id == R.id.exclude) {
                        menu.getItem(i).setVisible(false);
                    } else {
                        menu.getItem(i).setVisible(true);
                    }
                }
            }
        }, navIcon instanceof Animatable ? (int) (500 * Util.getAnimatorSpeed(this)) : 0);
    }

    @Override
    public void onSelectorModeExit(File_POJO[] selected_items) {
        switch (FileOperation.operation) {
            case FileOperation.DELETE:
                resetToolbar();
                fileOperation = new Delete(selected_items);
                fileOperation.execute(this, null,
                        new FileOperation.Callback() {
                            @Override
                            public void done() {
                                loadDirectory(currentDir.getPath());
                            }

                            @Override
                            public void failed(String path) {

                            }
                        });
                break;
            case FileOperation.COPY:
                fileOperation = new Copy(selected_items);
                recyclerViewAdapter.pickTarget();
                break;
            case FileOperation.MOVE:
                fileOperation = new Move(selected_items);
                recyclerViewAdapter.pickTarget();
                break;
        }

        if (fileOperation == null) {
            resetToolbar();
        }
    }

    @Override
    public void onItemSelected(int count) {
        if (count != 0) {
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            final String title = String.valueOf(count) + (count > 1 ?
                    getString(R.string.items) : getString(R.string.item));

            int color = ContextCompat.getColor(this, R.color.grey_900_translucent);
            ColorFade.fadeToolbarTitleColor(toolbar, color,
                    new ColorFade.ToolbarTitleFadeCallback() {
                        @Override
                        public void setTitle(Toolbar toolbar) {
                            toolbar.setTitle(title);
                        }
                    }, true);
        }
    }

    @Override
    public void onPickTargetModeEnter() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (fileOperation != null) {
            final int count = fileOperation.getFiles().length;

            int color = ContextCompat.getColor(this, R.color.grey_900_translucent);
            ColorFade.fadeToolbarTitleColor(toolbar, color,
                    new ColorFade.ToolbarTitleFadeCallback() {
                        @Override
                        public void setTitle(Toolbar toolbar) {
                            toolbar.setTitle(FileOperation.getModeString(FileExplorerActivity.this) + " "
                                    + String.valueOf(count)
                                    + (count > 1 ? getString(R.string.items) : getString(R.string.item)));
                        }
                    }, true);
        }

        animateFab(true);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //hide menu items
                for (int i = 0; i < menu.size(); i++) {
                    int id = menu.getItem(i).getItemId();
                    if (id == R.id.paste) {
                        menu.getItem(i).setVisible(true);
                    } else {
                        menu.getItem(i).setVisible(false);
                    }
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

        if (recyclerViewAdapter.getMode() == RecyclerViewAdapter.NORMAL_MODE) {
            final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

            int color = ContextCompat.getColor(FileExplorerActivity.this, text_color_res);
            ColorFade.fadeToolbarTitleColor(toolbar, color,
                    new ColorFade.ToolbarTitleFadeCallback() {
                        @Override
                        public void setTitle(Toolbar toolbar) {
                            toolbar.setTitle(currentDir.getPath());
                        }
                    }, false);
        }

        if (recyclerViewAdapter.getMode()
                == RecyclerViewAdapter.NORMAL_MODE) {
            manageMenuItems();
        }
    }

    public void resetToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(ContextCompat.getColor(this,
                android.R.color.transparent));

        if (THEME != ThemeableActivity.LIGHT) {
            toolbar.setActivated(false);
        }

        ColorFade.fadeBackgroundColor(toolbar,
                ContextCompat.getColor(this, accent_color_res),
                ContextCompat.getColor(this, toolbar_color_res));

        //fade overflow menu icon
        ColorFade.fadeIconColor(toolbar.getOverflowIcon(),
                ContextCompat.getColor(this, R.color.grey_900_translucent),
                ContextCompat.getColor(this, text_color_secondary_res));

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
                            ContextCompat.getDrawable(FileExplorerActivity.this, R.drawable.back_to_cancel_avd);
                    //mutating avd to reset it
                    drawable.mutate();
                    d = drawable;
                } else {
                    d = ContextCompat.getDrawable(FileExplorerActivity.this, R.drawable.ic_arrow_back_white_24dp);
                }
                d = DrawableCompat.wrap(d);
                DrawableCompat.setTint(d.mutate(),
                        ContextCompat.getColor(FileExplorerActivity.this,
                                text_color_secondary_res));
                toolbar.setNavigationIcon(d);

                if (THEME != ThemeableActivity.LIGHT) {
                    Util.setLightStatusBarIcons(findViewById(R.id.root_view));
                }

                int color = ContextCompat.getColor(FileExplorerActivity.this, text_color_res);
                ColorFade.fadeToolbarTitleColor(toolbar, color,
                        new ColorFade.ToolbarTitleFadeCallback() {
                            @Override
                            public void setTitle(Toolbar toolbar) {
                                toolbar.setTitle(currentDir.getPath());
                            }
                        }, false);

                //hide menu items
                for (int i = 0; i < menu.size(); i++) {
                    //menu.getItem(i).setVisible(false);
                    int id = menu.getItem(i).getItemId();
                    if (id == R.id.exclude) {
                        menu.getItem(i).setVisible(true);
                    } else {
                        menu.getItem(i).setVisible(false);
                    }
                }
            }
        }, navIcon instanceof Animatable ? (int) (500 * Util.getAnimatorSpeed(this)) : 0);
    }

    @Override
    public int getThemeRes(int style) {
        if (style == ThemeableActivity.DARK) {
            return R.style.Theme_CameraRoll_Translucent_FileExplorer;
        } else {
            return R.style.Theme_CameraRoll_Light_Translucent_FileExplorer;
        }
    }

    @Override
    public void onThemeApplied(int theme) {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setBackgroundTintList(ColorStateList
                .valueOf(ContextCompat.getColor(this, accent_color_res)));

        if (theme == ThemeableActivity.LIGHT) {
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            toolbar.setActivated(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Util.setDarkStatusBarIcons(findViewById(R.id.root_view));
            }
        }
    }
}
