package us.koller.cameraroll.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
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

import java.io.IOException;
import java.io.OutputStream;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.data.fileOperations.FileOperation;
import us.koller.cameraroll.data.provider.retriever.MediaStoreRetriever;
import us.koller.cameraroll.ui.widget.CropImageView;
import us.koller.cameraroll.util.ExifUtil;
import us.koller.cameraroll.util.InfoUtil;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.Util;

public class EditImageActivity extends AppCompatActivity {

    public static final String IMAGE_PATH = "IMAGE_PATH";
    public static final String IMAGE_VIEW_STATE = "IMAGE_VIEW_STATE";
    public static final int STORAGE_FRAMEWORK_REQUEST_CODE = 69;

    public static final int JPEG_QUALITY = 90;

    private String imagePath;

    private CropImageView.Result result;
    private ExifUtil.ExifItem[] exifData;

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

        Uri uri = intent.getData();
        if (uri == null) {
            finish();
            return;
        }

        String mimeType = MediaType.getMimeType(this, uri);
        if (!(MediaType.checkImageMimeType(mimeType) || MediaType.checkRAWMimeType(mimeType))) {
            Toast.makeText(this, R.string.editing_file_format_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        imagePath = intent.getStringExtra(IMAGE_PATH);

        final CropImageView imageView = findViewById(R.id.cropImageView);

        CropImageView.State state = null;
        if (savedInstanceState != null) {
            state = (CropImageView.State) savedInstanceState.getSerializable(IMAGE_VIEW_STATE);
        }
        imageView.loadImage(uri, state);

        final Button doneButton = findViewById(R.id.done_button);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                done(view);
            }
        });

        //setting window insets manually
        final ViewGroup rootView = findViewById(R.id.root_view);
        final View actionArea = findViewById(R.id.action_area);
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

                    imageView.setPadding(imageView.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                            imageView.getPaddingTop()/* + insets.getSystemWindowInsetTop()*/,
                            imageView.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                            imageView.getPaddingBottom()/* + insets.getSystemWindowInsetBottom()*/);

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

                                    imageView.setPadding(imageView.getPaddingStart() + windowInsets[0],
                                            imageView.getPaddingTop()/* + windowInsets[1]*/,
                                            imageView.getPaddingEnd() + windowInsets[2],
                                            imageView.getPaddingBottom()/* + windowInsets[3]*/);
                                }
                            });
        }

        imageView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        imageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        imageView.setPadding(imageView.getPaddingStart(),
                                imageView.getPaddingTop() + toolbar.getHeight(),
                                imageView.getPaddingEnd(),
                                imageView.getPaddingBottom() + actionArea.getHeight());
                    }
                });

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
        cropImageView.getCroppedBitmap(new CropImageView.OnResultListener() {
            @Override
            public void onResult(final CropImageView.Result result) {
                final BottomSheetDialog dialog = new BottomSheetDialog(EditImageActivity.this);
                @SuppressLint("InflateParams")
                View sheetView = EditImageActivity.this.getLayoutInflater()
                        .inflate(R.layout.edit_image_export_dialog, null);

                View save = sheetView.findViewById(R.id.save);
                View export = sheetView.findViewById(R.id.export);

                View.OnClickListener clickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                        switch (view.getId()) {
                            case R.id.save:
                                saveCroppedImage(result.getImageUri(), result.getCroppedBitmap(), exifData);
                                break;
                            case R.id.export:
                                EditImageActivity.this.result = result;
                                EditImageActivity.this.exifData = exifData;

                                Uri imageUri = getIntent().getData();
                                String filename = InfoUtil.retrieveFileName(EditImageActivity.this, imageUri);
                                if (filename == null) {
                                    filename = "image_edit.jpeg";
                                }

                                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                                intent.setType("image/jpeg");
                                intent.putExtra(Intent.EXTRA_TITLE, filename);
                                startActivityForResult(intent, STORAGE_FRAMEWORK_REQUEST_CODE);
                                break;
                            default:
                                break;
                        }
                    }
                };

                save.setOnClickListener(clickListener);
                if (imagePath == null) {
                    save.setEnabled(false);
                    save.setAlpha(0.5f);
                }
                export.setOnClickListener(clickListener);

                dialog.setContentView(sheetView);
                dialog.show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case STORAGE_FRAMEWORK_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    saveCroppedImage(uri, result.getCroppedBitmap(), exifData);
                }
                break;
            default:
                break;
        }
    }

    private void saveCroppedImage(final Uri uri, final Bitmap bitmap, final ExifUtil.ExifItem[] exifData) {
        if (uri == null || bitmap == null) {
            Toast.makeText(EditImageActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
            return;
        }

        final String newPath = MediaStoreRetriever.getPathForUri(EditImageActivity.this, uri);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    OutputStream outputStream;
                    try {
                        outputStream = getContentResolver().openOutputStream(uri);
                    } catch (SecurityException e) {
                        outputStream = null;
                    }

                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream);
                        outputStream.flush();
                        outputStream.close();
                    } else {
                        return;
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
        final CropImageView imageView = findViewById(R.id.cropImageView);
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.rotate:
                boolean showAnimations = Settings.getInstance(this).showAnimations();
                Drawable d = item.getIcon();
                if (showAnimations && d instanceof Animatable && !((Animatable) d).isRunning()) {
                    ((Animatable) d).start();
                }
                rotate90Degrees();
                break;
            case R.id.done:
                done(item.getActionView());
                break;
            case R.id.aspect_ratio_free:
                item.setChecked(true);
                imageView.setFreeAspectRatio();
                break;
            case R.id.aspect_ratio_original:
                item.setChecked(true);
                imageView.setOriginalAspectRatioFixed();
                break;
            case R.id.aspect_ratio_square:
                item.setChecked(true);
                imageView.setAspectRatio(1.0);
                break;
            case R.id.aspect_ratio_3_2:
                item.setChecked(true);
                imageView.setAspectRatio(3.0 / 2.0);
                break;
            case R.id.aspect_ratio_4_3:
                item.setChecked(true);
                imageView.setAspectRatio(4.0 / 3.0);
                break;
            case R.id.aspect_ratio_16_9:
                item.setChecked(true);
                imageView.setAspectRatio(16.0 / 9.0);
                break;
            case R.id.restore:
                imageView.restore();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void rotate90Degrees() {
        CropImageView imageView = findViewById(R.id.cropImageView);
        imageView.rotate90Degree();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        CropImageView imageView = findViewById(R.id.cropImageView);
        outState.putSerializable(IMAGE_VIEW_STATE, imageView.getCropImageViewState());
    }
}
