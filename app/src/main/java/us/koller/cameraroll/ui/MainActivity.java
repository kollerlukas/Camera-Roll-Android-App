package us.koller.cameraroll.ui;

import android.app.Activity;
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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.MediaLoader;

public class MainActivity extends AppCompatActivity {

    public static final String REFRESH_PHOTOS = "REFRESH_PHOTOS";
    public static final String SHARED_PREF_NAME = "CAMERA_ROLL_SETTINGS";
    public static final String HIDDEN_FOLDERS = "HIDDEN_FOLDERS";

    private ArrayList<Album> albums;

    private RecyclerViewAdapter recyclerViewAdapter;

    private Snackbar snackbar;

    private boolean hiddenFolders = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        albums = new ArrayList<>();

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.black_translucent2));

        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAdapter = new RecyclerViewAdapter().setAlbums(albums);
        recyclerView.setAdapter(recyclerViewAdapter);

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


        final ViewGroup rootView = (ViewGroup) findViewById(R.id.root_view);
        rootView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                toolbar.setPadding(toolbar.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                        toolbar.getPaddingTop() + insets.getSystemWindowInsetTop(),
                        toolbar.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                        toolbar.getPaddingBottom());

                recyclerView.setPadding(recyclerView.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                        recyclerView.getPaddingTop() + insets.getSystemWindowInsetTop(),
                        recyclerView.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                        recyclerView.getPaddingBottom() + insets.getSystemWindowInsetBottom());

                // clear this listener so insets aren't re-applied
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

        hiddenFolders = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
                .getBoolean(HIDDEN_FOLDERS, false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshPhotos();
    }

    @Override
    protected void onStop() {
        getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE).edit()
                .putBoolean(HIDDEN_FOLDERS, hiddenFolders).apply();
        super.onStop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction().equals(REFRESH_PHOTOS)) {
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

    private void setSystemUiFlags() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        //| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    public void refreshPhotos() {
        albums = new MediaLoader().loadPhotos(this, hiddenFolders);
        recyclerViewAdapter.setAlbums(albums).notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.hiddenFolders).setChecked(hiddenFolders);
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
                    snackbar = Snackbar.make(findViewById(R.id.root_view),
                            R.string.read_permission_denied, Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.retry, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    refreshPhotos();
                                }
                            });
                    snackbar.show();
                }
            }
        }
    }

    public static class RecyclerViewAdapter extends RecyclerView.Adapter {

        private ArrayList<Album> albums;

        RecyclerViewAdapter() {
        }

        RecyclerViewAdapter setAlbums(ArrayList<Album> albums) {
            this.albums = albums;
            return this;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.album_cover, parent, false);
            return new AlbumHolder(v);
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            final Album album = albums.get(position);
            ((AlbumHolder) holder).setAlbum(album);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(holder.itemView.getContext(), AlbumActivity.class);
                    intent.setAction(AlbumActivity.VIEW_ALBUM);
                    intent.putExtra(AlbumActivity.ALBUM, album);
                    holder.itemView.getContext().startActivity(intent,
                            ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    (Activity) holder.itemView.getContext()).toBundle());
                }
            });
        }

        @Override
        public int getItemCount() {
            return albums.size();
        }

        static class AlbumHolder extends RecyclerView.ViewHolder {

            private Album album;

            AlbumHolder(View itemView) {
                super(itemView);
            }

            void setAlbum(Album album) {
                this.album = album;
                ((TextView) itemView.findViewById(R.id.name)).setText(album.getName());
                String count = album.getAlbumItems().size()
                        + (album.getAlbumItems().size() > 1 ?
                        itemView.getContext().getString(R.string.items) :
                        itemView.getContext().getString(R.string.item));
                ((TextView) itemView.findViewById(R.id.count)).setText(count);
                itemView.findViewById(R.id.hidden_folder_indicator)
                        .setVisibility(album.hiddenAlbum ? View.VISIBLE : View.INVISIBLE);
                loadImage();
            }

            void loadImage() {
                Glide.clear(itemView.findViewById(R.id.image));
                Glide.with(itemView.getContext())
                        .load(album.getAlbumItems().get(0).getPath())
                        .skipMemoryCache(true)
                        .error(R.drawable.error_placeholder)
                        .listener(new RequestListener<String, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, String model, Target<GlideDrawable> target,
                                                       boolean isFirstResource) {
                                album.getAlbumItems().get(0).error = true;
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target,
                                                           boolean isFromMemoryCache, boolean isFirstResource) {
                                return false;
                            }
                        })
                        .into((ImageView) itemView.findViewById(R.id.image));
            }
        }
    }
}
