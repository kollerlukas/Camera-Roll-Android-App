package us.koller.cameraroll.ui.widget;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

//Solution heavily inspired from: https://github.com/WangDaYeeeeee/Mysplash/blob/master/app/src/main/res/about/layout/activity_about.xml

public class SwipeBackCoordinatorLayout extends CoordinatorLayout {
    // widget
    public OnSwipeListener listener;

    // data
    private int swipeDistance = 0;
    private static float SWIPE_TRIGGER = 100;
    private static final float SWIPE_RADIO = 2.5F;

    private float touchSlop;
    private float oldX;
    private float oldY;

    private int swipeDir = NULL_DIR;
    public static final int NULL_DIR = 0;
    public static final int UP_DIR = 1;
    public static final int DOWN_DIR = -1;

    /**
     * <br> life cycle.
     */

    public SwipeBackCoordinatorLayout(Context context) {
        super(context);
        this.initialize();
    }

    public SwipeBackCoordinatorLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.initialize();
    }

    public SwipeBackCoordinatorLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.initialize();
    }

    private void initialize() {
        SWIPE_TRIGGER = (float) (getResources().getDisplayMetrics().heightPixels / 5.0);
    }

    /**
     * <br> touch.
     */

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        super.dispatchTouchEvent(ev);
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            oldX = ev.getX();
            oldY = ev.getY();
        }
        return isEnabled() && listener != null;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        super.onInterceptTouchEvent(ev);
        switch (ev.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(ev.getY() - oldY) < touchSlop) {
                    oldX = ev.getX();
                    oldY = ev.getY();
                    return false;
                } else if (ev.getY() > oldY
                        && Math.abs(ev.getX() - oldX) < Math.abs(ev.getY() - oldY)) {
                    // swipe down.
                    if (swipeDistance == 0 && !listener.canSwipeBack(DOWN_DIR)) {
                        oldX = ev.getX();
                        oldY = ev.getY();
                        return false;
                    } else {
                        return true;
                    }
                } else if (ev.getY() < oldY
                        && Math.abs(ev.getX() - oldX) < Math.abs(ev.getY() - oldY)) {
                    // swipe up.
                    if (swipeDistance == 0 && !listener.canSwipeBack(UP_DIR)) {
                        oldX = ev.getX();
                        oldY = ev.getY();
                        return false;
                    } else {
                        return true;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                return swipeDistance != 0;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);
        switch (ev.getAction()) {
            case MotionEvent.ACTION_MOVE:
                onSwipe(ev.getY());
                return true;
            case MotionEvent.ACTION_UP:
                if (Math.abs(swipeDistance) >= SWIPE_TRIGGER) {
                    swipeBack();
                } else {
                    reset();
                }
                return true;
        }
        return false;
    }

    /**
     * <br> UI.
     */

    private void onSwipe(float newY) {
        if (swipeDistance * (newY - oldY) < 0) {
            swipeDistance = 0;
            swipeDir = NULL_DIR;
        } else {
            swipeDistance = (int) (newY - oldY);
        }
        if (swipeDistance != 0) {
            swipeDir = swipeDistance > 0 ? DOWN_DIR : UP_DIR;
        } else {
            swipeDir = NULL_DIR;
        }
        setSwipeTranslation();
    }

    private void swipeBack() {
        if (listener != null) {
            listener.onSwipeFinish(swipeDir);
        }
    }

    private void reset() {
        swipeDir = NULL_DIR;
        if (swipeDistance != 0) {
            ResetAnimation a = new ResetAnimation(swipeDistance);
            a.setAnimationListener(resetAnimListener);
            a.setDuration(300);
            startAnimation(a);
        }
    }

    private void setSwipeTranslation() {
        setTranslationY((float) (1.0 * swipeDistance / SWIPE_RADIO));
        if (listener != null) {
            listener.onSwipeProcess(
                    (float) Math.min(
                            1,
                            Math.abs(1.0 * swipeDistance / SWIPE_TRIGGER)));
        }
    }

    public static boolean canSwipeBackForThisView(View v, int dir) {
        return !ViewCompat.canScrollVertically(v, dir);
    }

    /**
     * <br> anim.
     */

    private class ResetAnimation extends Animation {
        // data
        private int fromDistance;

        ResetAnimation(int from) {
            this.fromDistance = from;
        }

        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            swipeDistance = (int) (fromDistance * (1 - interpolatedTime));
            setSwipeTranslation();
        }
    }

    private Animation.AnimationListener resetAnimListener
            = new Animation.AnimationListener() {

        @Override
        public void onAnimationStart(Animation animation) {
            setEnabled(false);
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            setEnabled(true);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
            // do nothing.
        }
    };

    /**
     * <br> interface.
     */

    public interface OnSwipeListener {
        boolean canSwipeBack(int dir);

        void onSwipeProcess(float percent);

        void onSwipeFinish(int dir);
    }

    public void setOnSwipeListener(OnSwipeListener l) {
        this.listener = l;
    }
}
