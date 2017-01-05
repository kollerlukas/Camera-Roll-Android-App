package us.koller.cameraroll.ui;

import android.content.pm.PackageManager;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import us.koller.cameraroll.R;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        ImageView headerImage = (ImageView) findViewById(R.id.header_image);
        Glide.with(this)
                .load("http://koller.us/Lukas/camera_roll/logo_outline_guidelines.png")
                .skipMemoryCache(true)
                .into(headerImage);

        TextView version = (TextView) findViewById(R.id.version);
        try {
            String versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
            version.setText(versionName);
        } catch (PackageManager.NameNotFoundException e) {
        }

        final TextView aboutText = (TextView) findViewById(R.id.about_text);
        aboutText.setText(Html.fromHtml(getString(R.string.about_text)));
        aboutText.setMovementMethod(new LinkMovementMethod());

        final View rootView = findViewById(R.id.root_view);
        rootView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                toolbar.setPadding(toolbar.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                        toolbar.getPaddingTop() + insets.getSystemWindowInsetTop(),
                        toolbar.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                        toolbar.getPaddingBottom());

                aboutText.setPadding(aboutText.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                        aboutText.getPaddingTop(),
                        aboutText.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                        aboutText.getPaddingBottom());

                // clear this listener so insets aren't re-applied
                rootView.setOnApplyWindowInsetsListener(null);
                return insets.consumeSystemWindowInsets();
            }
        });

        //set status bar icon color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            rootView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        for (int i = 0; i < toolbar.getChildCount(); i++) {
            if (toolbar.getChildAt(i) instanceof ImageView) {
                ((ImageView) toolbar.getChildAt(i)).setColorFilter(ContextCompat
                        .getColor(this, R.color.black_translucent2), PorterDuff.Mode.SRC_IN);
                break;
            }
        }

        setSystemUiFlags();
    }

    private void setSystemUiFlags() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
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
}
