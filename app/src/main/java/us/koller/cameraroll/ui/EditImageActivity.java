package us.koller.cameraroll.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImageView;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.util.Util;

public class EditImageActivity extends AppCompatActivity {

    public static final String IMAGE_URI = "IMAGE_URI";
    public static final String IMAGE_PATH = "IMAGE_PATH";

    private AlbumItem albumItem;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_image);

        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        final CropImageView cropImageView = findViewById(R.id.cropImageView);

        Uri uri;
        if (intent.getStringExtra(IMAGE_PATH) != null) {
            String path = intent.getStringExtra(IMAGE_PATH);
            albumItem = AlbumItem.getInstance(this, path);
            uri = albumItem.getUri(this);
        } else {
            String uriString = intent.getStringExtra(IMAGE_URI);
            if (uriString != null) {
                uri = Uri.parse(uriString);
            } else {
                return;
            }
        }
        cropImageView.setImageUriAsync(uri);

        SeekBar seekbar = findViewById(R.id.seekbar);
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                cropImageView.setRotatedDegrees(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

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

                    actionArea.setPadding(actionArea.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                            actionArea.getPaddingTop(),
                            actionArea.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                            actionArea.getPaddingBottom() + insets.getSystemWindowInsetBottom());

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

                                    actionArea.setPadding(actionArea.getPaddingStart() + windowInsets[0],
                                            actionArea.getPaddingTop(),
                                            actionArea.getPaddingEnd() + windowInsets[2],
                                            actionArea.getPaddingBottom() + windowInsets[3]);
                                }
                            });
        }
    }

    public void done(View v) {
        CropImageView cropImageView = findViewById(R.id.cropImageView);
        cropImageView.setOnCropImageCompleteListener(new CropImageView.OnCropImageCompleteListener() {
            @Override
            public void onCropImageComplete(CropImageView view, CropImageView.CropResult result) {
                Toast.makeText(EditImageActivity.this, "Success!", Toast.LENGTH_SHORT).show();

                //TODO invalidate Glide cached image
                finish();
            }
        });
        cropImageView.saveCroppedImageAsync(cropImageView.getImageUri());
    }
}
