package us.koller.cameraroll.ui;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
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
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.MediaLoader;
import us.koller.cameraroll.util.Util;

public class AlbumActivity extends AppCompatActivity {

    public static final String ALBUM = "ALBUM";
    public static final String VIEW_ALBUM = "VIEW_ALBUM";
    public static final String DELETE_PHOTO = "DELETE_PHOTO";
    public static final String EXTRA_STARTING_ALBUM_POSITION = "EXTRA_STARTING_ALBUM_POSITION";
    public static final String EXTRA_CURRENT_ALBUM_POSITION = "EXTRA_CURRENT_ALBUM_POSITION";

    private Bundle mTmpReenterState;

    private final SharedElementCallback mCallback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (mTmpReenterState != null) {
                int startingPosition = mTmpReenterState.getInt(EXTRA_STARTING_ALBUM_POSITION);
                int currentPosition = mTmpReenterState.getInt(EXTRA_CURRENT_ALBUM_POSITION);
                if (startingPosition != currentPosition) {
                    String newTransitionName = album.getAlbumItems().get(currentPosition).getPath();
                    View newSharedElement = recyclerView.findViewWithTag(newTransitionName);
                    if (newSharedElement != null) {
                        names.clear();
                        names.add(newTransitionName);
                        sharedElements.clear();
                        sharedElements.put(newTransitionName, newSharedElement);
                    }
                }
                mTmpReenterState = null;
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

        if (savedInstanceState != null && savedInstanceState.containsKey(ALBUM)) {
            album = savedInstanceState.getParcelable(ALBUM);
        } else {
            album = getIntent().getExtras().getParcelable(ALBUM);
        }

        if (album == null) {
            return;
        }

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.black_translucent2));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(album.getName());
        }

        boolean landscape = getResources().getBoolean(R.bool.landscape);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, !landscape ? 3 : 4));
        recyclerView.setAdapter(new RecyclerViewAdapter(album));

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

        final ViewGroup rootView = (ViewGroup) findViewById(R.id.root_view);
        rootView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
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

                rootView.setOnApplyWindowInsetsListener(null);
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

                recyclerView.scrollToPosition(0);

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
            final Album.Photo photo = intent.getParcelableExtra(ItemActivity.ALBUM_ITEM);
            deletePhoto(photo, intent.getBooleanExtra(ItemActivity.HIDDEN_PHOTO, false),
                    intent.getBooleanExtra(ItemActivity.VIEW_ONLY, false));
        }
    }

    @Override
    public void onActivityReenter(int requestCode, Intent data) {
        super.onActivityReenter(requestCode, data);
        mTmpReenterState = new Bundle(data.getExtras());
        int startingPosition = mTmpReenterState.getInt(EXTRA_STARTING_ALBUM_POSITION);
        int currentPosition = mTmpReenterState.getInt(EXTRA_CURRENT_ALBUM_POSITION);
        if (startingPosition != currentPosition) {
            recyclerView.scrollToPosition(currentPosition);
        }
        postponeEnterTransition();

        recyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                recyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                recyclerView.requestLayout();
                startPostponedEnterTransition();
                return true;
            }
        });
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (getResources().getBoolean(R.bool.landscape)) {
            setSystemUiFlags();
        }
    }

    public void deletePhoto(final Album.Photo photo, final boolean hiddenPhoto, boolean VIEW_ONLY) {
        if (!MediaLoader.checkPermission(this)) {
            return;
        }

        int k = album.getAlbumItems().indexOf(photo);
        for (int i = 0; i < album.getAlbumItems().size(); i++) {
            if (album.getAlbumItems().get(i).getPath().equals(photo.getPath())) {
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
                        album.getAlbumItems().add(index, photo);
                        recyclerView.getAdapter().notifyItemInserted(index);
                    }
                })
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                            if (!hiddenPhoto) {
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
                            }
                        }
                        super.onDismissed(snackbar, event);
                    }
                });
        snackbar.show();

        if (VIEW_ONLY) {
            snackbar.dismiss();
            this.finish();
        }
    }

    public void deletePhotos() {
        if (!MediaLoader.checkPermission(this)) {
            return;
        }

        final Album.Photo[] selected_photos = ((RecyclerViewAdapter) recyclerView.getAdapter()).cancelSelectorMode();
        final int[] indices = new int[selected_photos.length];
        for (int i = 0; i < selected_photos.length; i++) {
            Album.Photo photo = selected_photos[i];
            indices[i] = album.getAlbumItems().indexOf(photo);
            album.getAlbumItems().remove(photo);
            recyclerView.getAdapter().notifyItemRemoved(indices[i]);
        }

        String message = selected_photos.length == 1 ?
                selected_photos.length + " " + getString(R.string.photo_deleted) :
                selected_photos.length + " " + getString(R.string.photos_deleted);

        snackbar = Snackbar.make(findViewById(R.id.root_view), message, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        for (int i = 0; i < selected_photos.length; i++) {
                            Album.Photo photo = selected_photos[i];
                            int index = indices[i];
                            album.getAlbumItems().add(index, photo);
                            recyclerView.getAdapter().notifyItemInserted(index);
                        }
                    }
                })
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                            for (int i = 0; i < selected_photos.length; i++) {
                                if (!album.hiddenAlbum) {
                                    getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                            MediaStore.Images.ImageColumns.DATA + "=?", new String[]{selected_photos[i].getPath()});
                                } else {
                                    File file = new File(selected_photos[i].getPath());
                                    boolean result = file.delete();
                                }

                                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                        Uri.parse(selected_photos[i].getPath())));

                                if (refreshMainActivityAfterPhotoWasDeleted) {
                                    Intent intent = new Intent(AlbumActivity.this, MainActivity.class);
                                    intent.setAction(MainActivity.REFRESH_PHOTOS);
                                    startActivity(intent);
                                }
                            }
                        }
                    }
                });
        snackbar.show();
    }

    public static class RecyclerViewAdapter extends RecyclerView.Adapter {

        int VIEW_TYPE_VIDEO = 1;
        int VIEW_TYPE_PHOTO = 2;

        private Album album;

        private boolean selector_mode = false;
        private boolean[] selected_items;

        RecyclerViewAdapter(Album album) {
            this.album = album;
            selected_items = new boolean[album.getAlbumItems().size()];
        }

        @Override
        public int getItemViewType(int position) {
            return album.getAlbumItems().get(position).isPhoto() ? VIEW_TYPE_PHOTO : VIEW_TYPE_VIDEO;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(viewType == VIEW_TYPE_PHOTO ?
                    R.layout.photo_cover : R.layout.video_cover, parent, false);
            return new AlbumItemHolder(v);
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            final Album.AlbumItem albumItem = album.getAlbumItems().get(position);
            ((AlbumItemHolder) holder).setAlbumItem(albumItem);
            holder.itemView.findViewById(R.id.image)
                    .setSelected(selected_items[album.getAlbumItems().indexOf(albumItem)]);

            holder.itemView.setTag(albumItem.getPath());

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (albumItem.error) {
                        return;
                    }

                    if (selector_mode) {
                        selected_items[album.getAlbumItems().indexOf(albumItem)]
                                = !selected_items[album.getAlbumItems().indexOf(albumItem)];
                        holder.itemView.findViewById(R.id.image)
                                .setSelected(selected_items[album.getAlbumItems().indexOf(albumItem)]);
                        checkForNoSelectedItems(holder.itemView.getContext());
                    } else if (albumItem.isPhoto()) {
                        Intent intent = new Intent(holder.itemView.getContext(), ItemActivity.class);
                        intent.putExtra(ItemActivity.ALBUM_ITEM, albumItem);
                        intent.putExtra(ItemActivity.ALBUM, album);
                        intent.putExtra(ItemActivity.ITEM_POSITION, album.getAlbumItems().indexOf(albumItem));
                        //holder.itemView.getContext().startActivity(intent);
                        ActivityOptionsCompat options =
                                ActivityOptionsCompat.makeSceneTransitionAnimation(
                                        (Activity) holder.itemView.getContext(),
                                        holder.itemView.findViewById(R.id.image),
                                        albumItem.getPath());
                        holder.itemView.getContext().startActivity(intent, options.toBundle());
                    } else {
                        File file = new File(albumItem.getPath());
                        Uri uri = FileProvider.getUriForFile(holder.itemView.getContext(),
                                holder.itemView.getContext().getApplicationContext().getPackageName() + ".provider", file);

                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(uri, "video/*");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        try {
                            holder.itemView.getContext().startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(holder.itemView.getContext(), "No App found to play your video", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    }
                }
            });

            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (((AlbumItemHolder) holder).albumItem.error) {
                        return true;
                    }

                    if (!selector_mode) {
                        selector_mode = true;
                        selected_items = new boolean[album.getAlbumItems().size()];
                        ((AlbumActivity) view.getContext()).animateFab(selector_mode);
                    }

                    selected_items[album.getAlbumItems().indexOf(((AlbumItemHolder) holder).albumItem)]
                            = !selected_items[album.getAlbumItems().indexOf(((AlbumItemHolder) holder).albumItem)];
                    holder.itemView.findViewById(R.id.image)
                            .setSelected(selected_items[album.getAlbumItems().indexOf(((AlbumItemHolder) holder).albumItem)]);
                    checkForNoSelectedItems(holder.itemView.getContext());
                    return true;
                }
            });
        }

        void checkForNoSelectedItems(Context context) {
            int k = 0;
            for (int i = 0; i < selected_items.length; i++) {
                if (selected_items[i]) {
                    k++;
                }
            }
            if (k == 0) {
                selector_mode = false;
                ((AlbumActivity) context).animateFab(false);
                cancelSelectorMode();
            }
        }

        Album.Photo[] cancelSelectorMode() {
            ArrayList<Album.AlbumItem> selected_items = new ArrayList<>();
            selector_mode = false;
            for (int i = 0; i < this.selected_items.length; i++) {
                if (this.selected_items[i]) {
                    notifyItemChanged(i);
                    selected_items.add(album.getAlbumItems().get(i));
                }
            }
            this.selected_items = new boolean[album.getAlbumItems().size()];
            Album.Photo[] arr = new Album.Photo[selected_items.size()];
            /*for (int i = 0; i < selected_photos.size(); i++) {
                arr[i] = selected_photos.get(i);
            }*/
            return selected_items.toArray(arr);
        }

        boolean onBackPressed() {
            if (selector_mode) {
                cancelSelectorMode();
                return true;
            }
            return false;
        }

        @Override
        public int getItemCount() {
            return album.getAlbumItems().size();
        }

        static class AlbumItemHolder extends RecyclerView.ViewHolder {
            Album.AlbumItem albumItem;

            AlbumItemHolder(View itemView) {
                super(itemView);
            }

            void setAlbumItem(Album.AlbumItem albumItem) {
                this.albumItem = albumItem;
                loadImage();
            }

            void loadImage() {
                ImageView imageView = (ImageView) itemView.findViewById(R.id.image);
                int imageWidth = Util.getScreenWidth((Activity) imageView.getContext()) / 3;
                Glide.clear(imageView);
                Glide.with(itemView.getContext())
                        .load(albumItem.getPath())
                        .override(imageWidth, imageWidth)
                        .skipMemoryCache(true)
                        .error(R.drawable.error_placeholder)
                        .listener(new RequestListener<String, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, String model, Target<GlideDrawable> target,
                                                       boolean isFirstResource) {
                                albumItem.error = true;
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target,
                                                           boolean isFromMemoryCache, boolean isFirstResource) {
                                return false;
                            }
                        })
                        .into(imageView);
            }
        }
    }

    public void fabClicked() {
        animateFab(false);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                deletePhotos();
            }
        }, 400);
    }

    public void animateFab(final boolean b) {
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (b) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fabClicked();
                }
            });
        } else {
            fab.setOnClickListener(null);
            Drawable drawable = fab.getDrawable();
            if (drawable instanceof Animatable) {
                ((Animatable) drawable).start();
            }
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                fab.animate()
                        .scaleX(b ? 1.0f : 0.0f)
                        .scaleY(b ? 1.0f : 0.0f)
                        .alpha(b ? 1.0f : 0.0f)
                        .setDuration(250)
                        .start();
            }
        }, !b ? 400 : 0);
    }

    private void setSystemUiFlags() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        //| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
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
            animateFab(false);
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
    }

    private void setupTaskDescription() {
        Bitmap overviewIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.app_name),
                overviewIcon,
                ContextCompat.getColor(this, R.color.colorAccent)));
        overviewIcon.recycle();
    }
}
