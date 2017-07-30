package us.koller.cameraroll.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.data.fileOperations.FileOperation;
import us.koller.cameraroll.util.ExifUtil;
import us.koller.cameraroll.util.StorageUtil;
import us.koller.cameraroll.util.Util;

public class EditImageActivity extends AppCompatActivity {

    public static final String IMAGE_PATH = "IMAGE_PATH";

    private static final int JPEG_QUALITY = 90;

    private String imagePath;
    private boolean animating90DegreeRotation = false;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_image);

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

        final CropImageView cropImageView = findViewById(R.id.cropImageView);

        Uri uri = intent.getData();
        if (uri == null) {
            finish();
        }

        imagePath = intent.getStringExtra(IMAGE_PATH);

        if (savedInstanceState == null) {
            cropImageView.setImageUriAsync(uri);
        }

        final Button doneButton = findViewById(R.id.done_button);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                done(view);
            }
        });

        final View actionArea = findViewById(R.id.action_area);

        //setting window insets manually
        final ViewGroup rootView = findViewById(R.id.root_view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            rootView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
                public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                    // clear this listener so insets aren't re-applied
                    rootView.setOnApplyWindowInsetsListener(null);

                    toolbar.setPadding(toolbar.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                            toolbar.getPaddingTop() + insets.getSystemWindowInsetTop(),
                            toolbar.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                            toolbar.getPaddingBottom());

                    actionArea.setPadding(actionArea.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                            actionArea.getPaddingTop(),
                            actionArea.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                            actionArea.getPaddingBottom() + insets.getSystemWindowInsetBottom());

                    /*cropImageView.setPadding(cropImageView.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                            cropImageView.getPaddingTop(),
                            cropImageView.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                            cropImageView.getPaddingBottom());*/

                    return insets.consumeSystemWindowInsets();
                }
            });
        } else {
            rootView.getViewTreeObserver()
                    .addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                    // hacky way of getting window insets on pre-Lollipop
                                    // somewhat works...
                                    int[] screenSize = Util.getScreenSize(EditImageActivity.this);

                                    int[] windowInsets = new int[]{
                                            Math.abs(screenSize[0] - rootView.getLeft()),
                                            Math.abs(screenSize[1] - rootView.getTop()),
                                            Math.abs(screenSize[2] - rootView.getRight()),
                                            Math.abs(screenSize[3] - rootView.getBottom())};

                                    toolbar.setPadding(toolbar.getPaddingStart() + windowInsets[0],
                                            toolbar.getPaddingTop() + windowInsets[1],
                                            toolbar.getPaddingEnd() + windowInsets[2],
                                            toolbar.getPaddingBottom());

                                    actionArea.setPadding(actionArea.getPaddingStart() + windowInsets[0],
                                            actionArea.getPaddingTop(),
                                            actionArea.getPaddingEnd() + windowInsets[2],
                                            actionArea.getPaddingBottom() + windowInsets[3]);

                                    /*cropImageView.setPadding(cropImageView.getPaddingStart() + windowInsets[0],
                                            cropImageView.getPaddingTop(),
                                            cropImageView.getPaddingEnd() + windowInsets[2],
                                            cropImageView.getPaddingBottom());*/
                                }
                            });
        }

        /*rootView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        cropImageView.setPadding(cropImageView.getPaddingStart(),
                                cropImageView.getPaddingTop() + toolbar.getHeight(),
                                cropImageView.getPaddingEnd(),
                                cropImageView.getPaddingBottom() + actionArea.getHeight());
                    }
                });*/

        //needed to achieve transparent navBar
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    public void done(View v) {
        CropImageView cropImageView = findViewById(R.id.cropImageView);
        final ExifUtil.ExifItem[] exifData = ExifUtil.retrieveExifData(this, cropImageView.getImageUri());
        cropImageView.setOnCropImageCompleteListener(new CropImageView.OnCropImageCompleteListener() {
            @Override
            public void onCropImageComplete(CropImageView view, CropImageView.CropResult result) {
                saveCroppedImage(result.getOriginalUri(), result.getBitmap(), exifData);
            }
        });
        cropImageView.getCroppedImageAsync();
    }

    private void saveCroppedImage(final Uri uri, final Bitmap bitmap, final ExifUtil.ExifItem[] exifData) {
        if (uri == null || bitmap == null) {
            Toast.makeText(EditImageActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
            return;
        }

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String newPath = null;
                    OutputStream outputStream;
                    if (imagePath != null) {
                        boolean removableStorage = FileOperation.Util.isOnRemovableStorage(imagePath);
                        //replace fileExtension with .jpg
                        int index = imagePath.lastIndexOf(".");
                        newPath = imagePath.substring(0, index) + ".jpg";
                        if (!removableStorage) {
                            outputStream = new FileOutputStream(newPath);
                        } else {
                            Settings s = Settings.getInstance(getApplicationContext());
                            Uri treeUri = s.getRemovableStorageTreeUri();
                            DocumentFile file = StorageUtil.createDocumentFile(EditImageActivity.this,
                                    treeUri, imagePath, "image/jpeg");
                            if (file != null) {
                                outputStream = getContentResolver().openOutputStream(file.getUri());
                            } else {
                                outputStream = null;
                            }
                        }
                    } else {
                        outputStream = getContentResolver().openOutputStream(uri);
                    }

                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream);
                        outputStream.flush();
                        outputStream.close();
                    }

                    //save Exif-Data
                    if (exifData != null) {
                        ExifUtil.saveExifData(newPath, exifData);
                    }

                    //scan path
                    if (imagePath != null) {
                        FileOperation.Util.scanPaths(EditImageActivity.this, new String[]{newPath},
                                new FileOperation.Util.MediaScannerCallback() {
                                    @Override
                                    public void onAllPathsScanned() {
                                        Intent intent = new Intent(FileOperation.RESULT_DONE);
                                        LocalBroadcastManager.getInstance(EditImageActivity.this).sendBroadcast(intent);
                                    }
                                });
                    }

                    EditImageActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(EditImageActivity.this, R.string.success, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.image_edit, menu);
        MenuItem rotate = menu.findItem(R.id.rotate);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AnimatedVectorDrawable avd = (AnimatedVectorDrawable)
                    ContextCompat.getDrawable(this, R.drawable.ic_rotate_90_avd);
            rotate.setIcon(avd);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.rotate:
                if (!animating90DegreeRotation) {
                    Drawable d = item.getIcon();
                    if (d instanceof Animatable && !((Animatable) d).isRunning()) {
                        ((Animatable) d).start();
                    }
                    animate90DegreeRotation();
                }
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("unused")
    private void animate90DegreeRotation() {
        final CropImageView cropImageView = findViewById(R.id.cropImageView);
        final Rect cropRect = cropImageView.getCropRect();
        final int oldDegree = cropImageView.getRotatedDegrees();
        int newDegree = oldDegree + 90;
        if (newDegree > 360) {
            newDegree = newDegree % 360;
        }

        ValueAnimator animator = ValueAnimator.ofInt(oldDegree, newDegree);
        animator.setDuration(600);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int degree = (int) valueAnimator.getAnimatedValue();
                cropImageView.setRotatedDegrees(degree);
            }
        });

        animator.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                        animating90DegreeRotation = true;

                        cropImageView.setFixedAspectRatio(true);
                        cropImageView.setShowCropOverlay(false);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        animating90DegreeRotation = false;

                        cropImageView.setFixedAspectRatio(false);
                        cropImageView.setShowCropOverlay(true);
                    }
                });
        animator.start();
    }
}
