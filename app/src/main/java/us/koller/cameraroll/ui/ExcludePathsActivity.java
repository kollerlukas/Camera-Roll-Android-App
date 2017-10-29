package us.koller.cameraroll.ui;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.models.File_POJO;
import us.koller.cameraroll.data.models.StorageRoot;
import us.koller.cameraroll.data.provider.FilesProvider;
import us.koller.cameraroll.data.provider.Provider;
import us.koller.cameraroll.themes.Theme;
import us.koller.cameraroll.util.Util;

public class ExcludePathsActivity extends ThemeableActivity {

    public static final String ROOTS = "ROOTS";
    public static final String CURRENT_DIR = "CURRENT_DIR";
    public static final String STORAGE_ROOTS = "Storage Roots";

    private File_POJO roots;
    private File_POJO currentDir;

    private RecyclerView recyclerView;
    private RecyclerViewAdapter recyclerViewAdapter;

    private FilesProvider filesProvider;

    private OnDirectoryChangeCallback directoryChangeCallback =
            new OnDirectoryChangeCallback() {
                @Override
                public void changeDir(String path) {
                    loadDirectory(path);
                }
            };
    private OnExcludedPathChange excludedPathChangeCallback =
            new OnExcludedPathChange() {
                @Override
                public void onExcludedPathChange(String path, boolean exclude) {
                    Context context = ExcludePathsActivity.this;
                    if (exclude) {
                        Provider.addExcludedPath(context, path);
                    } else {
                        Provider.removeExcludedPath(context, path);
                    }
                }
            };

    interface OnDirectoryChangeCallback {
        void changeDir(String path);
    }

    interface OnExcludedPathChange {
        void onExcludedPathChange(String path, boolean exclude);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_excluded_paths);

        currentDir = new File_POJO("", false);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAdapter = new RecyclerViewAdapter(
                directoryChangeCallback, excludedPathChangeCallback);
        recyclerViewAdapter.notifyDataSetChanged();
        recyclerView.setAdapter(recyclerViewAdapter);

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

                    recyclerView.setPadding(recyclerView.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                            recyclerView.getPaddingTop(),
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
                                    //hacky way of getting window insets on pre-Lollipop
                                    int[] screenSize = Util.getScreenSize(ExcludePathsActivity.this);

                                    int[] windowInsets = new int[]{
                                            Math.abs(screenSize[0] - rootView.getLeft()),
                                            Math.abs(screenSize[1] - rootView.getTop()),
                                            Math.abs(screenSize[2] - rootView.getRight()),
                                            Math.abs(screenSize[3] - rootView.getBottom())};

                                    toolbar.setPadding(toolbar.getPaddingStart() + windowInsets[0],
                                            toolbar.getPaddingTop() + windowInsets[1],
                                            toolbar.getPaddingEnd() + windowInsets[2],
                                            toolbar.getPaddingBottom());

                                    recyclerView.setPadding(recyclerView.getPaddingStart() + windowInsets[0],
                                            recyclerView.getPaddingTop(),
                                            recyclerView.getPaddingEnd() + windowInsets[2],
                                            recyclerView.getPaddingBottom() + windowInsets[3]);

                                    rootView.getViewTreeObserver()
                                            .removeOnGlobalLayoutListener(this);
                                }
                            });
        }

        //needed to achieve transparent statusBar in landscape; don't ask me why, but its working
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
            onPathChanged();
        } else {
            loadRoots();
        }

        Log.d("ExcludedPathsActivity", "onCreate: " + Provider.getExcludedPaths());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void loadRoots() {
        StorageRoot[] storageRoots = FilesProvider.getRoots(this);
        roots = new StorageRoot(STORAGE_ROOTS);
        for (int i = 0; i < storageRoots.length; i++) {
            roots.addChild(storageRoots[i]);
        }

        currentDir = roots;
        if (recyclerViewAdapter != null) {
            recyclerViewAdapter.setFiles(currentDir);
            recyclerViewAdapter.notifyDataSetChanged();
            onPathChanged();
        }
    }

    public void loadDirectory(final String path) {
        final Snackbar snackbar = Snackbar.make(findViewById(R.id.root_view),
                getString(R.string.loading), Snackbar.LENGTH_INDEFINITE);
        Util.showSnackbar(snackbar);

        filesProvider = new FilesProvider(this);
        final FilesProvider.Callback callback = new FilesProvider.Callback() {
            @Override
            public void onDirLoaded(final File_POJO dir) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        filesProvider.onDestroy();
                        snackbar.dismiss();
                        if (dir == null) {
                            return;
                        }
                        File_POJO currentDir = removeFiles(dir);
                        if (currentDir == null) {
                            return;
                        }
                        ExcludePathsActivity.this.currentDir = currentDir;
                        if (recyclerViewAdapter != null) {
                            recyclerViewAdapter.setFiles(currentDir);
                            recyclerViewAdapter.notifyDataSetChanged();
                            onPathChanged();
                        }
                    }
                });
            }

            @Override
            public void timeout() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                });
            }

            @Override
            public void needPermission() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                });
            }
        };
        filesProvider.loadDir(this, path, callback);
    }

    private File_POJO removeFiles(File_POJO dir) {
        ArrayList<File_POJO> files = dir.getChildren();
        for (int i = files.size() - 1; i >= 0; i--) {
            File_POJO file = files.get(i);
            if (new File(file.getPath()).isFile()) {
                files.remove(i);
            }
        }
        if (dir.getChildren().size() == 0) {
            return null;
        }
        return dir;
    }

    public void onPathChanged() {
        TextView currentPath = findViewById(R.id.current_path);
        currentPath.setText(currentDir.getPath());
    }

    @Override
    public void onBackPressed() {
        if (currentDir != null && !currentDir.getPath().equals(STORAGE_ROOTS)) {
            if (!isCurrentFileARoot()) {
                String path = currentDir.getPath();
                int index = path.lastIndexOf("/");
                String parentPath = path.substring(0, index);

                loadDirectory(parentPath);
            } else {
                loadRoots();
            }
        } else {
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ROOTS, roots);
        if (currentDir != null) {
            outState.putParcelable(CURRENT_DIR, currentDir);
        }
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
    public int getDarkThemeRes() {
        return R.style.CameraRoll_Theme_ExcludePaths;
    }

    @Override
    public int getLightThemeRes() {
        return R.style.CameraRoll_Theme_Light_ExcludePaths;
    }

    @Override
    public void onThemeApplied(Theme theme) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(toolbarColor);
        toolbar.setTitleTextColor(textColorPrimary);

        if (theme.darkStatusBarIcons() &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Util.setDarkStatusBarIcons(findViewById(R.id.root_view));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int statusBarColor = getStatusBarColor();
            getWindow().setStatusBarColor(statusBarColor);
        }

    }

    private static class RecyclerViewAdapter extends RecyclerView.Adapter
            implements CompoundButton.OnCheckedChangeListener {

        private File_POJO files;

        private OnDirectoryChangeCallback directoryChangeCallback;
        private OnExcludedPathChange excludedPathChangeCallback;

        private static class FileHolder extends
                us.koller.cameraroll.adapter.fileExplorer.viewHolder.FileHolder {

            FileHolder(View itemView) {
                super(itemView);
            }

            @Override
            public void setFile(File_POJO file) {
                super.setFile(file);
                CheckBox checkBox = itemView.findViewById(R.id.checkbox);
                checkBox.setTag(file.getPath());
                setOnCheckedChangeListener(null);
                checkBox.setChecked(file.excluded);
                ArrayList<String> excludedPaths = Provider.getExcludedPaths();
                boolean enabled = !Provider.isDirExcludedBecauseParentDirIsExcluded(
                        file.getPath(), excludedPaths);
                checkBox.setEnabled(enabled);
            }

            void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener onCheckedChangeListener) {
                CheckBox checkBox = itemView.findViewById(R.id.checkbox);
                checkBox.setOnCheckedChangeListener(onCheckedChangeListener);
            }
        }

        public RecyclerViewAdapter(OnDirectoryChangeCallback directoryChangeCallback,
                                   OnExcludedPathChange excludedPathChangeCallback) {
            this.directoryChangeCallback = directoryChangeCallback;
            this.excludedPathChangeCallback = excludedPathChangeCallback;
        }

        public RecyclerViewAdapter setFiles(File_POJO files) {
            this.files = files;
            return this;
        }

        public File_POJO getFiles() {
            return files;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.excluded_paths_file_cover, parent, false);
            return new FileHolder(v);
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            final File_POJO file = files.getChildren().get(position);

            ((FileHolder) holder).setFile(file);
            ((FileHolder) holder).setOnCheckedChangeListener(this);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    directoryChangeCallback.changeDir(file.getPath());
                }
            });
        }

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            String path = (String) compoundButton.getTag();
            excludedPathChangeCallback.onExcludedPathChange(path, b);
            File_POJO file = null;
            for (int i = 0; i < files.getChildren().size(); i++) {
                File_POJO f = files.getChildren().get(i);
                if (f.getPath().equals(path)) {
                    file = f;
                }
            }
            if (file != null) {
                file.excluded = b;
            }
        }

        @Override
        public int getItemCount() {
            if (files != null) {
                return files.getChildren().size();
            }
            return 0;
        }
    }
}
