package us.koller.cameraroll.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.TransitionSet;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.fileExplorer.RecyclerViewAdapter;
import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.data.FilesLoader.FilesLoader;
import us.koller.cameraroll.ui.widget.ParallaxImageView;
import us.koller.cameraroll.ui.widget.SwipeBackCoordinatorLayout;
import us.koller.cameraroll.util.ColorFade;
import us.koller.cameraroll.util.Util;

public class FileExplorerActivity extends AppCompatActivity
        implements SwipeBackCoordinatorLayout.OnSwipeListener, RecyclerViewAdapter.Callback {

    public static final String FILES = "FILES";
    public static final String CURRENT_FILE = "CURRENT_FILE";
    public static final String SELECTED_ITEMS = "SELECTED_ITEMS";

    private File_POJO files;

    private FilesLoader loader;

    private RecyclerView recyclerView;
    private RecyclerViewAdapter recyclerViewAdapter;

    private Menu menu;

    private FileAction fileAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_explorer);

        //load files
        if (savedInstanceState != null
                && savedInstanceState.containsKey(FILES)) {
            files = savedInstanceState.getParcelable(FILES);
        } else {
            files = new File_POJO("", false);
        }

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.black_translucent2));
        toolbar.setNavigationIcon(AnimatedVectorDrawableCompat.create(this, R.drawable.back_to_cancel_animateable));
        setSupportActionBar(toolbar);

        //set Toolbar overflow icon color
        Drawable drawable = toolbar.getOverflowIcon();
        if (drawable != null) {
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable.mutate(),
                    ContextCompat.getColor(this, R.color.grey_900_translucent));
            toolbar.setOverflowIcon(drawable);
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.file_explorer));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final ViewGroup rootView = (ViewGroup) findViewById(R.id.root_view);
        if (rootView instanceof SwipeBackCoordinatorLayout) {
            ((SwipeBackCoordinatorLayout) rootView).setOnSwipeListener(this);
        }

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setTag(ParallaxImageView.RECYCLER_VIEW_TAG);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        if (savedInstanceState != null && savedInstanceState.containsKey(CURRENT_FILE)) {
            recyclerViewAdapter = new RecyclerViewAdapter(this)
                    .setFiles((File_POJO) savedInstanceState.getParcelable(CURRENT_FILE));
        } else {
            recyclerViewAdapter = new RecyclerViewAdapter(this)
                    .setFiles(files);
        }
        recyclerViewAdapter.notifyDataSetChanged();
        recyclerView.setAdapter(recyclerViewAdapter);

        //setting window insets manually
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
                        recyclerView.getPaddingTop() /*+ insets.getSystemWindowInsetTop()*/,
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

        //needed to achieve transparent navBar
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        //load files
        if (savedInstanceState == null) {
            loadFiles();
        }
    }

    public void loadFiles() {
        final Snackbar snackbar = Snackbar.make(findViewById(R.id.root_view),
                getString(R.string.loading), Snackbar.LENGTH_INDEFINITE);
        Util.showSnackbar(snackbar);

        loader = new FilesLoader();
        loader.loadFiles(this, new FilesLoader.LoaderCallback() {
            @Override
            public void onMediaLoaded(final File_POJO files) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loader.onDestroy();
                        loader = null;
                        FileExplorerActivity.this.files = files;
                        if (recyclerViewAdapter != null) {
                            recyclerViewAdapter.setFiles(files);
                            recyclerViewAdapter.notifyDataSetChanged();
                            onDataChanged();
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
                                getString(R.string.loading), Snackbar.LENGTH_INDEFINITE);
                        snackbar.setAction(getString(R.string.retry), new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                loadFiles();
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
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(FILES, files);
        outState.putParcelable(CURRENT_FILE, recyclerViewAdapter.getFiles());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_explorer, menu);
        this.menu = menu;
        //hide menu items; items are made visible, when a folder gets selected
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (recyclerViewAdapter.isModeActive()
                        || recyclerViewAdapter.getMode() == RecyclerViewAdapter.PICK_TARGET_MODE) {
                    recyclerViewAdapter.cancelMode();
                } else {
                    onBackPressed();
                }
                break;
            case R.id.paste:
                recyclerViewAdapter.cancelMode();
                if (FileAction.action == FileAction.MOVE
                        | FileAction.action == FileAction.COPY) {
                    fileAction.execute(this,
                            recyclerViewAdapter.getFiles(),
                            new FileAction.Callback() {
                                @Override
                                public void done() {
                                    loadFiles();
                                }
                            });
                }
                break;
            case R.id.move:
                FileAction.action = FileAction.MOVE;
                recyclerViewAdapter.cancelMode();
                break;
            case R.id.copy:
                FileAction.action = FileAction.COPY;
                recyclerViewAdapter.cancelMode();
                break;
            case R.id.delete:
                FileAction.action = FileAction.DELETE;
                recyclerViewAdapter.cancelMode();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (recyclerViewAdapter.isModeActive()) {
            recyclerViewAdapter.cancelMode();
        } else if (recyclerViewAdapter != null && files != null
                && !recyclerViewAdapter.getFiles().getPath().equals(files.getPath())) {
            recyclerViewAdapter.setFiles(files);
            recyclerViewAdapter.notifyDataSetChanged();
            this.onDataChanged();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (loader != null) {
            loader.onDestroy();
        }
    }

    @Override
    public boolean canSwipeBack(int dir) {
        return SwipeBackCoordinatorLayout.canSwipeBackForThisView(recyclerView, dir);
    }

    @Override
    public void onSwipeProcess(float percent) {
        getWindow().getDecorView().setBackgroundColor(SwipeBackCoordinatorLayout.getBackgroundColor(percent));
    }

    @Override
    public void onSwipeFinish(int dir) {
        if (recyclerViewAdapter != null && files != null
                && !recyclerViewAdapter.getFiles().getPath().equals(files.getPath())) {
            recyclerViewAdapter.setFiles(files);
            recyclerViewAdapter.notifyDataSetChanged();
            this.onDataChanged();
        }
        getWindow().setReturnTransition(new TransitionSet()
                .setOrdering(TransitionSet.ORDERING_TOGETHER)
                .addTransition(new Slide(dir > 0 ? Gravity.TOP : Gravity.BOTTOM))
                .addTransition(new Fade())
                .setInterpolator(new AccelerateDecelerateInterpolator()));
        onBackPressed();
    }

    @Override
    public void onSelectorModeEnter() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.transparent));
        toolbar.setActivated(true);
        toolbar.animate().translationY(0.0f).start();

        Util.setDarkStatusBarIcons(findViewById(R.id.root_view));

        ColorFade.fadeBackgroundColor(toolbar,
                ContextCompat.getColor(this, R.color.black_translucent2),
                ContextCompat.getColor(this, R.color.colorAccent));

        ((Animatable) toolbar.getNavigationIcon()).start();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                toolbar.setNavigationIcon(AnimatedVectorDrawableCompat
                        .create(FileExplorerActivity.this, R.drawable.cancel_to_back_vector_animateable));
                toolbar.setTitleTextColor(ContextCompat.getColor(FileExplorerActivity.this, R.color.grey_900_translucent));

                //make menu items visible
                for (int i = 0; i < menu.size(); i++) {
                    if (menu.getItem(i).getItemId() != R.id.paste) {
                        menu.getItem(i).setVisible(true);
                    }
                }
            }
        }, 300);
    }

    @Override
    public void onSelectorModeExit(File_POJO[] selected_items) {
        fileAction = new FileAction(selected_items);
        if (FileAction.action == FileAction.DELETE) {
            fileAction.execute(this, null,
                    new FileAction.Callback() {
                        @Override
                        public void done() {
                            loadFiles();
                        }
                    });
            resetToolbar();
        } else if (FileAction.action == FileAction.MOVE
                | FileAction.action == FileAction.COPY) {
            recyclerViewAdapter.pickTarget();
        } else {
            resetToolbar();
        }
    }

    @Override
    public void onItemSelected(int count) {
        String title = String.valueOf(count) + (count > 1 ?
                getString(R.string.items) : getString(R.string.item));
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(title);
    }

    @Override
    public void onPickTargetModeEnter() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (fileAction != null) {
            int count = fileAction.getFiles().length;
            toolbar.setTitle(FileAction.getModeString(this) + " "
                    + String.valueOf(count)
                    + (count > 1 ? getString(R.string.items) : getString(R.string.item)));
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //hide menu items
                for (int i = 0; i < menu.size(); i++) {
                    if (menu.getItem(i).getItemId() != R.id.paste) {
                        menu.getItem(i).setVisible(false);
                    } else {
                        menu.getItem(i).setVisible(true);
                        Drawable icon = menu.getItem(i).getIcon().mutate();
                        icon.setTint(ContextCompat.getColor(FileExplorerActivity.this,
                                R.color.grey_900_translucent));
                        menu.getItem(i).setIcon(icon);
                    }
                }
            }
        }, 300);
    }

    @Override
    public void onPickTargetModeExit() {
        resetToolbar();
    }

    @Override
    public void onDataChanged() {
        TextView emptyState = (TextView) findViewById(R.id.empty_state);
        emptyState.setVisibility(
                recyclerViewAdapter.getFiles().getChildren().size() == 0 ?
                        View.VISIBLE : View.GONE);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null && recyclerViewAdapter.getMode() != RecyclerViewAdapter.PICK_TARGET_MODE) {
            actionBar.setTitle(recyclerViewAdapter.getFiles().getPath());
        }
    }

    public void resetToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.transparent));
        toolbar.setActivated(false);
        ColorFade.fadeBackgroundColor(toolbar,
                ContextCompat.getColor(this, R.color.colorAccent),
                ContextCompat.getColor(this, R.color.black_translucent2));
        toolbar.setTitle(recyclerViewAdapter.getFiles().getPath());

        ((Animatable) toolbar.getNavigationIcon()).start();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                toolbar.setNavigationIcon(AnimatedVectorDrawableCompat
                        .create(FileExplorerActivity.this, R.drawable.back_to_cancel_animateable));
                toolbar.setTitleTextColor(ContextCompat.getColor(FileExplorerActivity.this, R.color.white));

                Util.setLightStatusBarIcons(findViewById(R.id.root_view));

                //hide menu items
                for (int i = 0; i < menu.size(); i++) {
                    menu.getItem(i).setVisible(false);
                }
            }
        }, 300);
    }

    public static class FileAction {

        public interface Callback {
            void done();
        }

        static final int EMPTY = 0;
        static final int MOVE = 1;
        static final int COPY = 2;
        static final int DELETE = 3;

        static int action = EMPTY;

        private File_POJO[] files;

        FileAction(File_POJO[] files) {
            this.files = files;
        }

        File_POJO[] getFiles() {
            return files;
        }

        void execute(final Activity context, final File_POJO target, final Callback callback) {

            if ((FileAction.action == FileAction.EMPTY)
                    || (target == null && action == FileAction.MOVE)
                    || (target == null && action == FileAction.COPY)) {
                return;
            }

            if (FileAction.action == FileAction.MOVE) {
                if (target == null) {
                    return;
                }

                int success_count = 0;
                for (int i = 0; i < files.length; i++) {
                    boolean result = moveFile(files[i].getPath(), target.getPath());
                    success_count += result ? 1 : 0;
                    if (result) {
                        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                Uri.parse(files[i].getPath())));
                        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                Uri.parse(target.getPath())));
                    }
                }

                Toast.makeText(context, context.getString(R.string.successfully_moved)
                        + String.valueOf(success_count) + "/"
                        + String.valueOf(files.length), Toast.LENGTH_SHORT).show();

            } else if (FileAction.action == FileAction.COPY) {
                if (target == null) {
                    return;
                }

                int success_count = 0;
                for (int i = 0; i < files.length; i++) {
                    boolean result = copyFile(files[i].getPath(), target.getPath());
                    success_count += result ? 1 : 0;
                    if (result) {
                        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                Uri.parse(files[i].getPath())));
                        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                Uri.parse(target.getPath())));
                    }
                }

                Toast.makeText(context, context.getString(R.string.successfully_copied)
                        + String.valueOf(success_count) + "/"
                        + String.valueOf(files.length), Toast.LENGTH_SHORT).show();

            } else if (FileAction.action == FileAction.DELETE) {

                int success_count = 0;
                for (int i = 0; i < files.length; i++) {
                    boolean result = deleteFile(files[i].getPath());
                    success_count += result ? 1 : 0;
                    if (result) {
                        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                Uri.parse(files[i].getPath())));
                    }
                }

                Toast.makeText(context, context.getString(R.string.successfully_deleted)
                        + String.valueOf(success_count) + "/"
                        + String.valueOf(files.length), Toast.LENGTH_SHORT).show();
            }
            FileAction.action = FileAction.EMPTY;

            if (callback != null) {
                callback.done();
            }
        }

        private static boolean moveFile(String path, String destination) {
            /*boolean result = copyFile(path, destination);

            //delete original file
            result = result && deleteFile(path);*/

            File file = new File(path);
            return file.renameTo(new File(destination, file.getName()));
        }

        private static boolean copyFile(String path, String destination) {
            //create output directory if it doesn't exist
            File dir = new File(destination);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            try {
                InputStream inputStream = new FileInputStream(path);
                OutputStream outputStream = new FileOutputStream(
                        new File(destination, new File(path).getName()));

                byte[] buffer = new byte[1024];

                int length;
                //copy the file content in bytes
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }

                inputStream.close();
                outputStream.close();

                // write the output file
                outputStream.flush();
                outputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        }

        private static boolean deleteFile(String path) {
            File file = new File(path);
            if (file.exists()) {
                return file.delete();
            }
            return false;
        }

        static String getModeString(Context context) {
            switch (action) {
                case EMPTY:
                    return "empty";
                case MOVE:
                    return context.getString(R.string.move);
                case COPY:
                    return context.getString(R.string.copy);
                case DELETE:
                    return context.getString(R.string.delete);
            }
            return "";
        }
    }
}
