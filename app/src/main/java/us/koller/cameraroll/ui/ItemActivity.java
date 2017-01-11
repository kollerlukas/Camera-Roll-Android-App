package us.koller.cameraroll.ui;

import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
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
import java.io.IOException;
import java.util.List;
import java.util.Map;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.item.ViewHolder.GifViewHolder;
import us.koller.cameraroll.adapter.item.ViewHolder.PhotoViewHolder;
import us.koller.cameraroll.adapter.item.ViewHolder.ViewHolder;
import us.koller.cameraroll.adapter.item.ViewPagerAdapter;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.Gif;
import us.koller.cameraroll.data.MediaLoader;
import us.koller.cameraroll.data.Photo;
import us.koller.cameraroll.data.Video;
import us.koller.cameraroll.util.TransitionListenerAdapter;

public class ItemActivity extends AppCompatActivity {

    public static final String ALBUM_ITEM = "ALBUM_ITEM";
    public static final String ALBUM = "ALBUM";
    public static final String ITEM_POSITION = "ITEM_POSITION";
    public static final String VIEW_ONLY = "VIEW_ONLY";
    public static final String HIDDEN_PHOTO = "HIDDEN_PHOTO";
    private static final String WAS_SYSTEM_UI_HIDDEN = "WAS_SYSTEM_UI_HIDDEN";
    private static final String IMAGE_VIEW_SAVED_STATE = "IMAGE_VIEW_SAVED_STATE";
    private static final String INFO_DIALOG_SHOWN = "INFO_DIALOG_SHOWN";

    private boolean isReturning;
    private final SharedElementCallback sharedElementCallback = new SharedElementCallback() {
        @Override
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
            if (albumItem instanceof Photo && isReturning) {
                ViewHolder viewHolder = ((ViewPagerAdapter)
                        viewPager.getAdapter()).findViewHolderByTag(albumItem.getPath());
                if (viewHolder instanceof PhotoViewHolder) {
                    ((PhotoViewHolder) viewHolder).swapView(isReturning);
                }
            }

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
            if (albumItem instanceof Photo
                    && viewHolder instanceof PhotoViewHolder && !isReturning) {
                ((PhotoViewHolder) viewHolder).swapView(false);
            } else if (!isReturning && albumItem instanceof Gif
                    && viewHolder instanceof GifViewHolder) {
                ((GifViewHolder) viewHolder).reloadGif();
            }

            showUI(!isReturning);
            if (transition != null) {
                super.onTransitionEnd(transition);
            }
            albumItem.isSharedElement = false;
        }
    };

    private Toolbar toolbar;
    private View bottomBar;
    private ViewPager viewPager;

    private AlertDialog infoDialog;

    private boolean systemUiVisible = true;

    private Album album;
    private AlbumItem albumItem;

    public boolean view_only;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_view);

        view_only = getIntent().getBooleanExtra(VIEW_ONLY, false);

        if (!view_only) {
            postponeEnterTransition();
            setEnterSharedElementCallback(sharedElementCallback);
            getWindow().getSharedElementEnterTransition().addListener(transitionListener);
        }

        if (savedInstanceState != null) {
            album = savedInstanceState.getParcelable(ALBUM);
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
            album = getIntent().getExtras().getParcelable(AlbumActivity.ALBUM);
            int position = getIntent().getIntExtra(ITEM_POSITION, 0);
            albumItem = album.getAlbumItems().get(position);
            albumItem.isSharedElement = true;
        }

        if (album == null || albumItem == null) {
            return;
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.black_translucent2));

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(albumItem.getName() != null ? albumItem.getName() : "");
            actionBar.setDisplayHomeAsUpEnabled(!view_only);
        }

        viewPager = (ViewPager) findViewById(R.id.view_pager);
        viewPager.setAdapter(new ViewPagerAdapter(album));
        viewPager.setCurrentItem(album.getAlbumItems().indexOf(albumItem), false);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                albumItem = album.getAlbumItems().get(position);
                if (actionBar != null) {
                    actionBar.setTitle(albumItem.getName() != null ? albumItem.getName() : "");
                }
            }
        });

        bottomBar = findViewById(R.id.bottom_bar);
        ImageView delete_button = (ImageView) bottomBar.findViewById(R.id.delete_button);
        delete_button.setImageDrawable(AnimatedVectorDrawableCompat
                .create(this, R.drawable.ic_delete_vector_animateable));

        final ViewGroup rootView = (ViewGroup) findViewById(R.id.root_view);
        rootView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
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

        if (savedInstanceState != null && savedInstanceState.containsKey(WAS_SYSTEM_UI_HIDDEN)) {
            systemUiVisible = !savedInstanceState.getBoolean(WAS_SYSTEM_UI_HIDDEN);
        }

        setupTaskDescription();

        if (view_only) {
            viewPager.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    viewPager.getViewTreeObserver().removeOnPreDrawListener(this);
                    transitionListener.onTransitionEnd(null);
                    return false;
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.photo, menu);
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
            case R.id.edit:
                editPhoto();
                break;
            case R.id.delete:
                showDeleteDialog();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setPhotoAs() {
        File file = new File(albumItem.getPath());
        Uri uri = FileProvider.getUriForFile(this,
                this.getApplicationContext().getPackageName() + ".provider", file);

        Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("jpg", "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivityForResult(Intent.createChooser(intent,
                    getString(R.string.set_as)), 13);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No App found to edit your photo", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void sharePhoto() {
        File file = new File(albumItem.getPath());
        Uri uri = FileProvider.getUriForFile(this,
                this.getApplicationContext().getPackageName() + ".provider", file);

        Intent shareIntent = ShareCompat.IntentBuilder.from(this)
                .addStream(uri)
                .setType("image*/*")
                .getIntent();

        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (shareIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_photo)));
        }
    }

    public void editPhoto() {
        if (!(albumItem instanceof Video)) {
            return;
        }

        File file = new File(albumItem.getPath());
        Uri uri = FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".provider", file);

        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("jpg", "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No App found to edit your Photo", Toast.LENGTH_SHORT).show();
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
        if (!MediaLoader.checkPermission(this)) {
            return;
        }

        if (albumItem == null) {
            return;
        }

        this.finish();

        Intent intent = new Intent(this, AlbumActivity.class);
        intent.setAction(AlbumActivity.DELETE_PHOTO);
        intent.putExtra(AlbumActivity.ALBUM, album);
        intent.putExtra(ALBUM_ITEM, albumItem);
        intent.putExtra(HIDDEN_PHOTO, album.hiddenAlbum);
        intent.putExtra(VIEW_ONLY, view_only);
        startActivity(intent);
    }

    public void showInfoDialog() {
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(albumItem.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (exif == null) {
            Snackbar.make(findViewById(R.id.root_view), R.string.error, Snackbar.LENGTH_LONG).show();
            return;
        }

        File file = new File(albumItem.getPath());

        String name = albumItem.getName();
        String path = albumItem.getPath();
        String size = getFileSize(file);
        String height = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
        String width = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
        String date = exif.getAttribute(ExifInterface.TAG_DATETIME);
        String focal_length = parseFocalLength(exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)) + " mm";
        String exposure = parseExposureTime(exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)) + " sec";
        String model = exif.getAttribute(ExifInterface.TAG_MAKE) + " " + exif.getAttribute(ExifInterface.TAG_MODEL);
        String aperture = "", iso = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            aperture = "f/" + exif.getAttribute(ExifInterface.TAG_F_NUMBER);
            iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS);
        }

        String[] values = {name, path, size, width + " x " + height,
                date, model, focal_length, exposure, aperture, iso};

        RecyclerView recyclerView = new RecyclerView(this);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(new InfoRecyclerViewAdapter(values));

        /*recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                scrollbar_top.setVisibility(
                        linearLayoutManager.findFirstCompletelyVisibleItemPosition() == 0 ?
                                View.INVISIBLE : View.VISIBLE);

                scrollbar_bottom.setVisibility(
                        linearLayoutManager.findLastCompletelyVisibleItemPosition()
                                != recyclerView.getAdapter().getItemCount() -1 ?
                                View.VISIBLE : View.INVISIBLE);
            }
        });*/

        infoDialog = new AlertDialog.Builder(this, R.style.Theme_CameraRoll_Dialog)
                .setTitle(getString(R.string.info))
                .setPositiveButton(R.string.done, null)
                .setView(recyclerView)
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
                ((AnimatedVectorDrawableCompat) delete_button.getDrawable()).start();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showDeleteDialog();
                    }
                }, 400);
                break;
        }
    }

    public void imageOnClick() {
        systemUiVisible = !systemUiVisible;
        showSystemUI(systemUiVisible);
    }

    private void showUI(boolean show) {
        float toolbar_translationY = show ? 0 : -(toolbar.getHeight());
        float bottomBar_translationY = show ? 0 : ((View) bottomBar.getParent()).getHeight();
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
        new Handler().postDelayed(new Runnable() {
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
                                | View.SYSTEM_UI_FLAG_IMMERSIVE);
            }
        }, 1);

        showUI(show);
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
                    //deletePhoto();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Snackbar.make(findViewById(R.id.root_view), R.string.write_permission_denied, Snackbar.LENGTH_LONG)
                            .setAction("Retry", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    MediaLoader.checkPermission(ItemActivity.this);
                                }
                            }).show();
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        View view = viewPager.findViewWithTag(albumItem.getPath()).findViewById(R.id.subsampling);
        if (view instanceof SubsamplingScaleImageView) {
            SubsamplingScaleImageView imageView = (SubsamplingScaleImageView) view;
            ImageViewState state = imageView.getState();
            if (state != null) {
                Log.d("ItemActivity", "Saving state");
                outState.putSerializable(IMAGE_VIEW_SAVED_STATE, imageView.getState());
            }
        }
        outState.putParcelable(ALBUM, album);
        outState.putParcelable(ALBUM_ITEM, albumItem);
        outState.putBoolean(WAS_SYSTEM_UI_HIDDEN, !systemUiVisible);
        outState.putBoolean(INFO_DIALOG_SHOWN, infoDialog != null);
    }

    public interface Callback {
        void callback();
    }

    @Override
    public void onBackPressed() {
        showUI(false);
        if (albumItem instanceof Photo) {
            ViewHolder viewHolder = ((ViewPagerAdapter)
                    viewPager.getAdapter()).findViewHolderByTag(albumItem.getPath());
            if (viewHolder instanceof PhotoViewHolder) {
                ((PhotoViewHolder) viewHolder).scaleDown(new Callback() {
                    @Override
                    public void callback() {
                        setResultAndFinish();
                    }
                });
            }
        } else {
            setResultAndFinish();
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
        data.putExtra(AlbumActivity.EXTRA_CURRENT_ALBUM_POSITION, viewPager.getCurrentItem());
        setResult(RESULT_OK, data);
        finishAfterTransition();
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
        if (input == null) {
            return null;
        }
        float f = Float.valueOf(input);
        try {
            int i = Math.round(1 / f);
            return String.valueOf(1 + "/" + i);
        } catch (NumberFormatException e) {
            return input;
        }
    }

    public String parseFocalLength(String input) {
        if (input == null) {
            return null;
        }
        String[] arr = input.split("/");
        if (arr.length != 2) {
            return input;
        }
        try {
            double focalLength = Double.valueOf(arr[0]) / Double.valueOf(arr[1]);
            return String.valueOf(focalLength);
        } catch (NumberFormatException e) {
            return input;
        }
    }

    public static class InfoRecyclerViewAdapter extends RecyclerView.Adapter {
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
