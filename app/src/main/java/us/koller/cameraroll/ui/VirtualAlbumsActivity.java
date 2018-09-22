package us.koller.cameraroll.ui;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.data.models.VirtualAlbum;
import us.koller.cameraroll.data.provider.Provider;
import us.koller.cameraroll.themes.Theme;
import us.koller.cameraroll.util.Util;

public class VirtualAlbumsActivity extends ThemeableActivity {

    private ArrayList<VirtualAlbum> virtualAlbums;

    private RecyclerViewAdapter adapter;
    private RecyclerViewAdapter.OnVirtualAlbumChangedListener onVirtualAlbumChangedListener;

    private Menu menu;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_virtual_albums);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        virtualAlbums = Provider.getVirtualAlbums(this);
        final TextView emptyStateText = findViewById(R.id.empty_state_text);
        if (virtualAlbums.size() == 0) {
            emptyStateText.setText(R.string.no_virtual_albums);
            emptyStateText.setVisibility(View.VISIBLE);
        }

        final int accentColor = theme.getAccentColor(this);
        final int toolbarTitleColor = theme.getTextColorPrimary(this);
        final String toolbarTitle = String.valueOf(toolbar.getTitle());

        final RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecyclerViewAdapter(virtualAlbums);
        onVirtualAlbumChangedListener = album -> {
            menu.findItem(R.id.add_virtual_album).setVisible(album == null);

            if (album != null) {
                toolbar.setTitle(album.getName());
                toolbar.setTitleTextColor(accentColor);
            } else {
                toolbar.setTitle(toolbarTitle);
                toolbar.setTitleTextColor(toolbarTitleColor);
            }

            if (album == null) {
                if (virtualAlbums.size() == 0) {
                    emptyStateText.setText(R.string.no_virtual_albums);
                    emptyStateText.setVisibility(View.VISIBLE);
                } else {
                    emptyStateText.setVisibility(View.GONE);
                }
            } else {
                if (album.getDirectories().size() == 0) {
                    emptyStateText.setText(R.string.no_paths);
                    emptyStateText.setVisibility(View.VISIBLE);
                } else {
                    emptyStateText.setVisibility(View.GONE);
                }
            }
        };
        adapter.setOnVirtualAlbumChangedListener(onVirtualAlbumChangedListener);
        recyclerView.setAdapter(adapter);

        final ViewGroup rootView = findViewById(R.id.root_view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            rootView.setOnApplyWindowInsetsListener((view, insets) -> {
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
            });
        } else {
            rootView.getViewTreeObserver()
                    .addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    //hacky way of getting window insets on pre-Lollipop
                                    int[] screenSize = Util.getScreenSize(VirtualAlbumsActivity.this);

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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.virtual_albums, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.add_virtual_album:
                AlertDialog dialog = VirtualAlbum.Util.getCreateVirtualAlbumDialog(this, virtualAlbum -> {
                    virtualAlbums = Provider.getVirtualAlbums(VirtualAlbumsActivity.this);
                    adapter.notifyDataSetChanged();
                    onVirtualAlbumChangedListener.onVirtualAlbumChanged(null);
                });
                dialog.show();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (adapter.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Provider.saveVirtualAlbums(this);
    }

    @Override
    public int getDarkThemeRes() {
        return R.style.CameraRoll_Theme_VirtualDirectories;
    }

    @Override
    public int getLightThemeRes() {
        return R.style.CameraRoll_Theme_Light_VirtualDirectories;
    }

    @Override
    public void onThemeApplied(Theme theme) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(toolbarColor);
        toolbar.setTitleTextColor(textColorPrimary);

        if (theme.darkStatusBarIcons() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Util.setDarkStatusBarIcons(findViewById(R.id.root_view));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int statusBarColor = getStatusBarColor();
            getWindow().setStatusBarColor(statusBarColor);
        }
    }

    private static class RecyclerViewAdapter extends RecyclerView.Adapter {

        private static final int VIRTUAL_ALBUM_VIEW_TYPE = 1;
        private static final int PATH_VIEW_TYPE = 2;

        private ArrayList<VirtualAlbum> virtualAlbums;
        private VirtualAlbum currentAlbum;

        private OnVirtualAlbumChangedListener listener;

        interface OnVirtualAlbumChangedListener {
            void onVirtualAlbumChanged(VirtualAlbum album);
        }

        private static abstract class ViewHolder extends RecyclerView.ViewHolder {

            private Theme theme;

            TextView textView;
            ImageView deleteButton;
            ImageView folderIndicator;

            public ViewHolder(View itemView) {
                super(itemView);
                Context context = itemView.getContext();
                theme = Settings.getInstance(context).getThemeInstance(context);

                textView = itemView.findViewById(R.id.text);
                deleteButton = itemView.findViewById(R.id.delete_button);
                int secondaryTextColor = theme.getTextColorSecondary(context);
                deleteButton.setColorFilter(secondaryTextColor, PorterDuff.Mode.SRC_IN);

                folderIndicator = itemView.findViewById(R.id.folder_indicator);
            }

            public Theme getTheme() {
                return theme;
            }
        }

        private static class VirtualAlbumHolder extends ViewHolder {

            VirtualAlbumHolder(View itemView) {
                super(itemView);
                Context context = itemView.getContext();
                Theme theme = getTheme();
                int accentColor = theme.getAccentColor(context);
                textView.setTextColor(accentColor);
                folderIndicator.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
            }

            void bind(VirtualAlbum album) {
                textView.setText(album.getName());
            }
        }

        private static class PathHolder extends ViewHolder {

            PathHolder(View itemView) {
                super(itemView);
                Theme theme = getTheme();

                int secondaryTextColor = theme.getTextColorSecondary(itemView.getContext());
                folderIndicator.setColorFilter(secondaryTextColor, PorterDuff.Mode.SRC_IN);
            }

            void bind(String path) {
                textView.setText(path);
            }
        }

        public RecyclerViewAdapter(ArrayList<VirtualAlbum> virtualAlbums) {
            this.virtualAlbums = virtualAlbums;
        }

        void setOnVirtualAlbumChangedListener(OnVirtualAlbumChangedListener listener) {
            this.listener = listener;
        }

        @Override
        public int getItemViewType(int position) {
            if (currentAlbum != null) {
                return PATH_VIEW_TYPE;
            }
            return VIRTUAL_ALBUM_VIEW_TYPE;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v;
            switch (viewType) {
                case VIRTUAL_ALBUM_VIEW_TYPE:
                    v = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.virtual_album_cover, parent, false);
                    return new VirtualAlbumHolder(v);
                case PATH_VIEW_TYPE:
                    v = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.virtual_album_path_cover, parent, false);
                    return new PathHolder(v);
                default:
                    return null;
            }

        }

        @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
            if (holder instanceof VirtualAlbumHolder) {
                final VirtualAlbum virtualAlbum = virtualAlbums.get(position);
                ((VirtualAlbumHolder) holder).bind(virtualAlbum);
                holder.itemView.setOnClickListener(view -> {
                    currentAlbum = virtualAlbum;
                    //Handler to keep ripple animation
                    new Handler().postDelayed(() -> {
                        notifyDataSetChanged();
                        if (listener != null) {
                            listener.onVirtualAlbumChanged(currentAlbum);
                        }
                    }, /*300*/0);
                });
                ((VirtualAlbumHolder) holder).deleteButton.
                        setOnClickListener(view -> {
                            final int index = virtualAlbums.indexOf(virtualAlbum);
                            Provider.removeVirtualAlbum(view.getContext(), virtualAlbum);
                            virtualAlbums = Provider.getVirtualAlbums(view.getContext());
                            //Handler to keep ripple animation
                            new Handler().postDelayed(() -> {
                                //notifyDataSetChanged();
                                notifyItemRemoved(index);
                                if (listener != null) {
                                    listener.onVirtualAlbumChanged(currentAlbum);
                                }

                                String message = view.getContext()
                                        .getString(R.string.virtual_album_deleted, virtualAlbum.getName());
                                Toast.makeText(view.getContext(), message, Toast.LENGTH_SHORT).show();
                            }, /*300*/0);

                            if (virtualAlbum.pinned()) {
                                //remove virtualAlbum from pinnedPaths
                                Provider.unpinPath(view.getContext(), virtualAlbum.getPath());
                                Provider.savePinnedPaths(view.getContext());
                            }
                        });
            } else {
                final String path = currentAlbum.getDirectories().get(position);
                ((PathHolder) holder).bind(path);
                ((PathHolder) holder).deleteButton.
                        setOnClickListener(view -> {
                            final int index = currentAlbum.getDirectories().indexOf(path);
                            currentAlbum.removeDirectory(path);
                            //Handler to keep ripple animation
                            new Handler().postDelayed(() -> {
                                //notifyDataSetChanged();
                                notifyItemRemoved(index);
                                if (listener != null) {
                                    listener.onVirtualAlbumChanged(currentAlbum);
                                }
                                Toast.makeText(view.getContext(), R.string.path_removed, Toast.LENGTH_SHORT).show();
                            }, /*300*/0);
                        });
            }
        }

        @Override
        public int getItemCount() {
            if (currentAlbum != null) {
                return currentAlbum.getDirectories().size();
            }
            return virtualAlbums.size();
        }

        public boolean onBackPressed() {
            if (currentAlbum != null) {
                currentAlbum = null;
                notifyDataSetChanged();
                listener.onVirtualAlbumChanged(null);
                return false;
            }
            return true;
        }
    }
}
