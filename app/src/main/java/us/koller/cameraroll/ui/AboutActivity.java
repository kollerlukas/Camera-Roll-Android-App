package us.koller.cameraroll.ui;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.transition.Slide;
import android.transition.TransitionSet;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import us.koller.cameraroll.R;
import us.koller.cameraroll.ui.widget.SwipeBackCoordinatorLayout;
import us.koller.cameraroll.util.Util;

public class AboutActivity extends ThemeableActivity
        implements SwipeBackCoordinatorLayout.OnSwipeListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setEnterTransition(new Slide(Gravity.TOP));
            getWindow().setReturnTransition(new Slide(Gravity.TOP));
        }

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
                .load("https://raw.githubusercontent.com/kollerlukas/Camera-Roll-Android-App/master/camera_roll_logo.png")
                .error(R.drawable.error_placeholder)
                .into(headerImage);

        TextView version = (TextView) findViewById(R.id.version);
        try {
            String versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
            final int versionCode = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionCode;
            version.setText(Html.fromHtml(/*getString(R.string.app_name) + "<br/>" +*/ versionName));
            version.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    Toast.makeText(view.getContext(), "versionCode: " + String.valueOf(versionCode), Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        final TextView aboutText = (TextView) findViewById(R.id.about_text);
        aboutText.setText(Html.fromHtml(getString(R.string.about_text)));
        aboutText.setMovementMethod(new LinkMovementMethod());

        final View rootView = findViewById(R.id.root_view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            rootView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
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
        } else {
            rootView.getViewTreeObserver()
                    .addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    // hacky way of getting window insets on pre-Lollipop
                                    // somewhat works...
                                    int[] screenSize = Util.getScreenSize(AboutActivity.this);

                                    int[] windowInsets = new int[]{
                                            Math.abs(screenSize[0] - rootView.getLeft()),
                                            Math.abs(screenSize[1] - rootView.getTop()),
                                            Math.abs(screenSize[2] - rootView.getRight()),
                                            Math.abs(screenSize[3] - rootView.getBottom())};

                                    toolbar.setPadding(toolbar.getPaddingStart(),
                                            toolbar.getPaddingTop() + windowInsets[1],
                                            toolbar.getPaddingEnd(),
                                            toolbar.getPaddingBottom());

                                    View viewGroup = findViewById(R.id.swipeBackView);
                                    ViewGroup.MarginLayoutParams viewGroupParams
                                            = (ViewGroup.MarginLayoutParams) viewGroup.getLayoutParams();
                                    viewGroupParams.leftMargin += windowInsets[0];
                                    viewGroupParams.rightMargin += windowInsets[2];
                                    viewGroup.setLayoutParams(viewGroupParams);

                                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                }
                            });
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

    @Override
    public void onSwipeProcess(float percent) {
        getWindow().getDecorView().setBackgroundColor(SwipeBackCoordinatorLayout.getBackgroundColor(percent));
    }

    @Override
    public void onSwipeFinish(int dir) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setReturnTransition(new TransitionSet()
                    .addTransition(new Slide(dir > 0 ? Gravity.TOP : Gravity.BOTTOM))
                    .setInterpolator(new AccelerateDecelerateInterpolator()));
        }
        onBackPressed();
    }

    @Override
    public int getThemeRes(int style) {
        if (style == DARK) {
            return R.style.Theme_CameraRoll_Translucent_About;
        } else {
            return R.style.Theme_CameraRoll_Translucent_Light_About;
        }
    }
}
