package us.koller.cameraroll.ui;

import android.app.WallpaperManager;
import android.content.Intent;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.Toast;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.ImageViewState;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.io.IOException;
import java.io.InputStream;

import us.koller.cameraroll.R;
import us.koller.cameraroll.imageDecoder.CustomRegionDecoder;
import us.koller.cameraroll.imageDecoder.GlideImageDecoder;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.Util;

public class SetWallpaperActivity extends AppCompatActivity {

    private static final String IMAGE_VIEW_STATE = "IMAGE_VIEW_STATE";

    private Uri imageUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_wallpaper);

        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        imageUri = intent.getData();
        if (!MediaType.suitableAsWallpaper(this, imageUri)) {
            Toast.makeText(this, R.string.wallpaper_file_format_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        final SubsamplingScaleImageView imageView = findViewById(R.id.imageView);
        ImageViewState imageViewState = null;
        if (savedInstanceState != null && savedInstanceState.containsKey(IMAGE_VIEW_STATE)) {
            imageViewState = (ImageViewState) savedInstanceState.getSerializable(IMAGE_VIEW_STATE);
        }

        imageView.setMinimumTileDpi(196);

        // use custom decoders
        imageView.setBitmapDecoderClass(GlideImageDecoder.class);
        imageView.setRegionDecoderClass(CustomRegionDecoder.class);

        imageView.setImage(ImageSource.uri(imageUri), imageViewState);
        imageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP);

        if (imageViewState == null) {
            imageView.setOnImageEventListener(
                    new SubsamplingScaleImageView.DefaultOnImageEventListener() {
                        @Override
                        public void onImageLoaded() {
                            super.onImageLoaded();
                            float scale = imageView.getScale();
                            PointF center = new PointF(imageView.getWidth() / 2, 0.0f);
                            imageView.setScaleAndCenter(scale, center);
                        }
                    });
        }

        //setting window insets manually
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            toolbar.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
                public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                    // clear this listener so insets aren't re-applied
                    toolbar.setOnApplyWindowInsetsListener(null);

                    toolbar.setPadding(toolbar.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                            toolbar.getPaddingTop() + insets.getSystemWindowInsetTop(),
                            toolbar.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                            toolbar.getPaddingBottom());

                    return insets.consumeSystemWindowInsets();
                }
            });
        } else {
            toolbar.getViewTreeObserver()
                    .addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    toolbar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                    // hacky way of getting window insets on pre-Lollipop
                                    // somewhat works...
                                    int[] screenSize = Util.getScreenSize(SetWallpaperActivity.this);

                                    int[] windowInsets = new int[]{
                                            Math.abs(screenSize[0] - toolbar.getLeft()),
                                            Math.abs(screenSize[1] - toolbar.getTop()),
                                            Math.abs(screenSize[2] - toolbar.getRight()),
                                            Math.abs(0)};

                                    toolbar.setPadding(toolbar.getPaddingStart() + windowInsets[0],
                                            toolbar.getPaddingTop() + windowInsets[1],
                                            toolbar.getPaddingEnd() + windowInsets[2],
                                            toolbar.getPaddingBottom());
                                }
                            });
        }

        //needed to achieve transparent navBar
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.set_wallpaper, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.set_wallpaper:
                setWallpaper();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setWallpaper() {
        try {
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Rect croppedRect = getCroppedRect();
                wallpaperManager.setStream(inputStream, croppedRect, true);
            } else {
                wallpaperManager.setStream(inputStream);
            }

            SubsamplingScaleImageView imageView = findViewById(R.id.imageView);
            imageView.recycle();

            this.finish();
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
        }
    }

    private Rect getCroppedRect() {
        SubsamplingScaleImageView imageView = findViewById(R.id.imageView);
        PointF center = imageView.getCenter();
        if (center != null) {
            int left = (int) (center.x - imageView.getWidth() / 2);
            return new Rect(left, 0, imageView.getSWidth(), imageView.getSHeight());
        }
        return new Rect(0, 0, imageView.getSWidth(), imageView.getSHeight());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        SubsamplingScaleImageView imageView = findViewById(R.id.imageView);
        outState.putSerializable(IMAGE_VIEW_STATE, imageView.getState());
    }
}
