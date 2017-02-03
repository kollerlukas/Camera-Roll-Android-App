package us.koller.cameraroll.ui;

import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.transition.Slide;
import android.transition.TransitionSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.MediaLoader.MediaLoader;
import us.koller.cameraroll.ui.widget.SwipeBackCoordinatorLayout;

public class AboutActivity extends AppCompatActivity implements SwipeBackCoordinatorLayout.OnSwipeListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        getWindow().setEnterTransition(new Slide(Gravity.TOP));
        getWindow().setReturnTransition(new Slide(Gravity.TOP));

        SwipeBackCoordinatorLayout swipeBackView
                = (SwipeBackCoordinatorLayout) findViewById(R.id.swipeBackView);
        swipeBackView.setOnSwipeListener(this);

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
                .into(headerImage);

        TextView version = (TextView) findViewById(R.id.version);
        try {
            String versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
            final int versionCode = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionCode;
            version.setText(getString(R.string.app_name) + " " + versionName);
            version.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    Toast.makeText(view.getContext(), "versionCode: " + String.valueOf(versionCode), Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        } catch (PackageManager.NameNotFoundException e) {
        }

        final TextView aboutText = (TextView) findViewById(R.id.about_text);
        aboutText.setText(Html.fromHtml(getString(R.string.about_text)));
        aboutText.setMovementMethod(new LinkMovementMethod());

        final View rootView = findViewById(R.id.root_view);
        rootView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                toolbar.setPadding(toolbar.getPaddingStart() /*+ insets.getSystemWindowInsetLeft()*/,
                        toolbar.getPaddingTop() + insets.getSystemWindowInsetTop(),
                        toolbar.getPaddingEnd() /*+ insets.getSystemWindowInsetRight()*/,
                        toolbar.getPaddingBottom());

                aboutText.setPadding(aboutText.getPaddingStart(),
                        aboutText.getPaddingTop(),
                        aboutText.getPaddingEnd(),
                        aboutText.getPaddingBottom() + insets.getSystemWindowInsetBottom());

                View viewGroup = findViewById(R.id.swipeBackView);
                ViewGroup.MarginLayoutParams viewGroupParams
                        = (ViewGroup.MarginLayoutParams) viewGroup.getLayoutParams();
                viewGroupParams.leftMargin += insets.getSystemWindowInsetLeft();
                viewGroupParams.rightMargin += insets.getSystemWindowInsetRight();
                viewGroup.setLayoutParams(viewGroupParams);

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

    @Override
    public boolean canSwipeBack(int dir) {
        return SwipeBackCoordinatorLayout
                .canSwipeBackForThisView(findViewById(R.id.scroll_view), dir);
    }

    private Handler handler;
    private Runnable runnable;
    private boolean consumed = false;

    @Override
    public void onSwipeProcess(float percent) {
        getWindow().getDecorView().setBackgroundColor(SwipeBackCoordinatorLayout.getBackgroundColor(percent));

        //hidden ability to change the way media is loaded (either through MediaStore or custom Storage traversal)
        if (percent == 1.0f) {
            if (handler == null) {
                handler = new Handler();
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (!consumed) {
                            MediaLoader.toggleMode(AboutActivity.this);
                            consumed = true;
                        }
                    }
                };
                handler.postDelayed(runnable, 5000);
            }
        } else {
            if (handler != null && runnable != null) {
                handler.removeCallbacks(runnable);
                consumed = false;
            }
            handler = null;
            runnable = null;
        }
    }

    @Override
    public void onSwipeFinish(int dir) {
        getWindow().setReturnTransition(new TransitionSet()
                .addTransition(new Slide(dir > 0 ? Gravity.TOP : Gravity.BOTTOM))
                .setInterpolator(new AccelerateDecelerateInterpolator()));
        onBackPressed();
    }
}
