package us.koller.cameraroll.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.ImageViewState;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import us.koller.cameraroll.R;
import us.koller.cameraroll.imageDecoder.GlideImageDecoder;
import us.koller.cameraroll.imageDecoder.RAWImageBitmapRegionDecoder;
import us.koller.cameraroll.util.Util;

public class CropImageView extends SubsamplingScaleImageView implements View.OnTouchListener {

    private static final int MIN_CROP_RECT_SIZE_DP = 50;

    private static final int STROKE_WIDTH_DP = 2;
    private static final int STROKE_COLOR_RES = R.color.white_translucent1;

    private static final int CORNER_STROKE_WIDTH_DP = 3;
    private static final int CORNER_LENGTH_DP = 16;
    private static final int CORNER_COLOR_RES = R.color.white;

    private static final int GUIDELINE_STROKE_WIDTH_DP = 1;
    private static final int GUIDELINE_COLOR_RES = R.color.white;

    private static final int TOUCH_DELTA_DP = 50;

    private static final int NO_CORNER = -1;
    private static final int TOP_LEFT = 1;
    private static final int TOP_RIGHT = 2;
    private static final int BOTTOM_RIGHT = 3;
    private static final int BOTTOM_LEFT = 4;

    private Uri imageUri;

    private Rect cropRect;
    private Paint cropRectPaint;
    private Paint cropRectCornerPaint;
    private Paint guidelinePaint;
    private Paint backgroundPaint;

    private int touchedCorner = NO_CORNER;
    private boolean touching = false;

    private int minCropRectSize;
    private int strokeWidth;
    private int cornerStrokeWidth;
    private int cornerLength;
    private int touchDelta;

    private int[] padding = new int[]{0, 0, 0, 0};

    public interface OnResultListener {
        void onResult(Result result);
    }

    public class Result {

        private Uri uri;
        private Bitmap bitmap;

        Result(Uri uri, Bitmap bitmap) {
            this.uri = uri;
            this.bitmap = bitmap;
        }

        public Uri getImageUri() {
            return uri;
        }

        public Bitmap getCroppedBitmap() {
            return bitmap;
        }
    }

    public static class State extends ImageViewState {

        private Rect cropRect;

        State(ImageViewState imageViewState, Rect cropRect) {
            super(imageViewState.getScale(), imageViewState.getCenter(), imageViewState.getOrientation());
            this.cropRect = cropRect;
        }

        Rect getCropRect() {
            return cropRect;
        }
    }

    public CropImageView(Context context, AttributeSet attr) {
        super(context, attr);
        init();
    }

    public CropImageView(Context context) {
        this(context, null);
    }

    private void init() {
        setZoomEnabled(false);
        setPanEnabled(false);
        setPanLimit(PAN_LIMIT_CENTER);
        setBitmapDecoderClass(GlideImageDecoder.class);
        setRegionDecoderClass(RAWImageBitmapRegionDecoder.class);
        setMinimumTileDpi(196);
        setMinScale(2.0f);

        setOnTouchListener(this);

        minCropRectSize = Util.dpToPx(getContext(), MIN_CROP_RECT_SIZE_DP);
        strokeWidth = Util.dpToPx(getContext(), STROKE_WIDTH_DP);
        cornerStrokeWidth = Util.dpToPx(getContext(), CORNER_STROKE_WIDTH_DP);
        cornerLength = Util.dpToPx(getContext(), CORNER_LENGTH_DP);
        touchDelta = Util.dpToPx(getContext(), TOUCH_DELTA_DP);

        cropRectPaint = new Paint();
        cropRectPaint.setColor(ContextCompat.getColor(getContext(), STROKE_COLOR_RES));
        cropRectPaint.setStrokeWidth(strokeWidth);
        cropRectPaint.setStyle(Paint.Style.STROKE);

        cropRectCornerPaint = new Paint();
        cropRectCornerPaint.setColor(ContextCompat.getColor(getContext(), CORNER_COLOR_RES));
        cropRectCornerPaint.setStrokeWidth(Util.dpToPx(getContext(), CORNER_STROKE_WIDTH_DP));
        cropRectCornerPaint.setStyle(Paint.Style.STROKE);

        guidelinePaint = new Paint();
        guidelinePaint.setColor(ContextCompat.getColor(getContext(), GUIDELINE_COLOR_RES));
        guidelinePaint.setStrokeWidth(Util.dpToPx(getContext(), GUIDELINE_STROKE_WIDTH_DP));
        guidelinePaint.setStyle(Paint.Style.STROKE);
        guidelinePaint.setAlpha(100);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(ContextCompat.getColor(getContext(), R.color.black));
        backgroundPaint.setAlpha(100);
    }

    public void loadImage(Uri uri, State state) {
        imageUri = uri;
        if (state != null) {
            cropRect = state.getCropRect();
        }
        setImage(ImageSource.uri(uri), state);
    }

    public Uri getImageUri() {
        return imageUri;
    }

    @Override
    protected void onImageLoaded() {
        super.onImageLoaded();
        if (cropRect == null) {
            cropRect = new Rect(0, 0, getSWidth(), getSHeight());
        } else {
            autoZoom();
        }
    }

    public void getCroppedBitmap(final OnResultListener onResultListener) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    RAWImageBitmapRegionDecoder decoder = new RAWImageBitmapRegionDecoder();
                    decoder.init(getContext(), getImageUri());
                    Bitmap croppedBitmap = decoder.decodeRegion(cropRect, 1);
                    decoder.recycle();

                    Result result = new Result(imageUri, croppedBitmap);
                    onResultListener.onResult(result);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        boolean consumed = false;
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchedCorner = getTouchedCorner(motionEvent);
                touching = true;
                consumed = true;
                break;
            case MotionEvent.ACTION_MOVE:
                if (touchedCorner != NO_CORNER) {
                    cropRect = getNewRect(motionEvent);
                    consumed = true;
                } else {
                    cropRect = getMovedRect(motionEvent);
                    consumed = true;
                }

                PointF center = getCenterOfCropRect();
                float scale = getScale();
                float newScale = getNewScale();
                setScaleAndCenter(newScale < scale ? newScale : scale, center);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                //auto-zoom
                autoZoom();
                touching = false;
                touchedCorner = NO_CORNER;
                invalidate();
                break;
        }
        // Use parent to handle pinch and two-finger pan.
        return consumed || super.onTouchEvent(motionEvent);
    }

    private PointF getCenterOfCropRect() {
        return new PointF(
                cropRect.centerX(),
                cropRect.centerY());
    }

    private float getNewScale() {
        int width = getWidth() - (padding[0] + padding[2]);
        float scaleWidth = (float) width / (cropRect.right - cropRect.left);

        int height = getHeight() - (padding[1] + padding[3]);
        float scaleHeight = (float) height / (cropRect.bottom - cropRect.top);
        return scaleWidth < scaleHeight ? scaleWidth : scaleHeight;
    }

    private void autoZoom() {
        //auto-zoom
        float scale = getNewScale();
        PointF center = getCenterOfCropRect();
        animateScaleAndCenter(scale, center)
                .withDuration(300)
                .withInterruptible(false)
                .start();
    }

    private int getTouchedCorner(MotionEvent motionEvent) {
        PointF currentTouchPos = viewToSourceCoord(
                motionEvent.getX(), motionEvent.getY());

        if (currentTouchPos.x > cropRect.left - touchDelta
                && currentTouchPos.x < cropRect.left + touchDelta
                && currentTouchPos.y > cropRect.top - touchDelta
                && currentTouchPos.y < cropRect.top + touchDelta) {
            return TOP_LEFT;
        }

        if (currentTouchPos.x > cropRect.right - touchDelta
                && currentTouchPos.x < cropRect.right + touchDelta
                && currentTouchPos.y > cropRect.top - touchDelta
                && currentTouchPos.y < cropRect.top + touchDelta) {
            return TOP_RIGHT;
        }

        if (currentTouchPos.x > cropRect.right - touchDelta
                && currentTouchPos.x < cropRect.right + touchDelta
                && currentTouchPos.y > cropRect.bottom - touchDelta
                && currentTouchPos.y < cropRect.bottom + touchDelta) {
            return BOTTOM_RIGHT;
        }

        if (currentTouchPos.x > cropRect.left - touchDelta
                && currentTouchPos.x < cropRect.left + touchDelta
                && currentTouchPos.y > cropRect.bottom - touchDelta
                && currentTouchPos.y < cropRect.bottom + touchDelta) {
            return BOTTOM_LEFT;
        }

        return NO_CORNER;
    }

    private Rect getNewRect(MotionEvent motionEvent) {
        PointF currentTouchPos = viewToSourceCoord(
                motionEvent.getX(), motionEvent.getY());

        if (touchedCorner == TOP_LEFT) {
            Rect newCropRect = new Rect(
                    (int) currentTouchPos.x,
                    (int) currentTouchPos.y,
                    cropRect.right,
                    cropRect.bottom);
            return checkRectBounds(newCropRect, true);
        }

        if (touchedCorner == TOP_RIGHT) {
            Rect newCropRect = new Rect(
                    cropRect.left,
                    (int) currentTouchPos.y,
                    (int) currentTouchPos.x,
                    cropRect.bottom);
            return checkRectBounds(newCropRect, true);
        }

        if (touchedCorner == BOTTOM_RIGHT) {
            Rect newCropRect = new Rect(
                    cropRect.left,
                    cropRect.top,
                    (int) currentTouchPos.x,
                    (int) currentTouchPos.y);
            return checkRectBounds(newCropRect, true);
        }

        if (touchedCorner == BOTTOM_LEFT) {
            Rect newCropRect = new Rect(
                    (int) currentTouchPos.x,
                    cropRect.top,
                    cropRect.right,
                    (int) currentTouchPos.y);
            return checkRectBounds(newCropRect, true);
        }

        return null;
    }

    private Rect getMovedRect(MotionEvent motionEvent) {
        PointF currentTouchPos = viewToSourceCoord(
                motionEvent.getX(), motionEvent.getY());

        int historySize = motionEvent.getHistorySize();
        if (historySize > 0) {
            PointF oldTouchPos = viewToSourceCoord(
                    motionEvent.getHistoricalX(0),
                    motionEvent.getHistoricalY(0));
            float deltaX = oldTouchPos.x - currentTouchPos.x;
            float deltaY = oldTouchPos.y - currentTouchPos.y;

            Rect newCropRect = new Rect(
                    (int) (cropRect.left + deltaX),
                    (int) (cropRect.top + deltaY),
                    (int) (cropRect.right + deltaX),
                    (int) (cropRect.bottom + deltaY));
            return checkRectBounds(newCropRect, false);
        } else {
            return cropRect;
        }
    }

    private Rect checkRectBounds(Rect cropRect, boolean resize) {
        Rect image = getImageRect();

        Rect newCropRect = cropRect;

        //check if inside image
        int width = newCropRect.right - newCropRect.left,
                height = newCropRect.bottom - newCropRect.top;

        if (image.left > newCropRect.left) {
            newCropRect = new Rect(image.left, newCropRect.top,
                    resize ? newCropRect.right : image.left + width,
                    newCropRect.bottom);
        }

        if (image.top > newCropRect.top) {
            newCropRect = new Rect(
                    newCropRect.left, image.top, newCropRect.right,
                    resize ? newCropRect.bottom : image.top + height);
        }

        if (image.right < newCropRect.right) {
            newCropRect = new Rect(
                    resize ? newCropRect.left : image.right - width,
                    newCropRect.top, image.right, newCropRect.bottom);
        }

        if (image.bottom < newCropRect.bottom) {
            newCropRect = new Rect(newCropRect.left,
                    resize ? newCropRect.top : image.bottom - height,
                    newCropRect.right, image.bottom);
        }

        //check min size
        width = newCropRect.width();
        if (width < minCropRectSize) {
            if (touchedCorner == TOP_LEFT) {
                newCropRect = new Rect(newCropRect.right - minCropRectSize,
                        newCropRect.top, newCropRect.right, newCropRect.bottom);
            } else if (touchedCorner == TOP_RIGHT) {
                newCropRect = new Rect(newCropRect.left, newCropRect.top,
                        newCropRect.left + minCropRectSize,
                        newCropRect.bottom);
            } else if (touchedCorner == BOTTOM_RIGHT) {
                newCropRect = new Rect(newCropRect.left, newCropRect.top,
                        newCropRect.left + minCropRectSize,
                        newCropRect.bottom);
            } else if (touchedCorner == BOTTOM_LEFT) {
                newCropRect = new Rect(newCropRect.right - minCropRectSize,
                        newCropRect.top, newCropRect.right, newCropRect.bottom);
            }
        }

        height = newCropRect.height();
        if (height < minCropRectSize) {
            if (touchedCorner == TOP_LEFT) {
                newCropRect = new Rect(newCropRect.left,
                        newCropRect.bottom - minCropRectSize,
                        newCropRect.right, newCropRect.bottom);
            } else if (touchedCorner == TOP_RIGHT) {
                newCropRect = new Rect(newCropRect.left,
                        newCropRect.bottom - minCropRectSize,
                        newCropRect.right, newCropRect.bottom);
            } else if (touchedCorner == BOTTOM_RIGHT) {
                newCropRect = new Rect(newCropRect.left, newCropRect.top,
                        newCropRect.right,
                        newCropRect.top + minCropRectSize);
            } else if (touchedCorner == BOTTOM_LEFT) {
                newCropRect = new Rect(newCropRect.left,
                        newCropRect.top, newCropRect.right,
                        newCropRect.top + minCropRectSize);
            }
        }

        return newCropRect;
    }

    private Rect getImageRect() {
        return new Rect(0, 0, getSWidth(), getSHeight());
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        //super.setPadding(left, top, right, bottom);
        padding = new int[]{left, top, right, bottom};
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Don't draw anything before image is ready.
        if (!isReady() || cropRect == null) {
            return;
        }

        drawBackground(canvas);
        drawRect(canvas);
        drawCorners(canvas);
        if (touching) {
            drawGuidelines(canvas);
        }
    }

    private void drawRect(Canvas canvas) {
        PointF topLeft = sourceToViewCoord(cropRect.left, cropRect.top);
        PointF bottomRight = sourceToViewCoord(cropRect.right, cropRect.bottom);
        canvas.drawRect(
                topLeft.x + strokeWidth / 2,
                topLeft.y + strokeWidth / 2,
                bottomRight.x - strokeWidth / 2,
                bottomRight.y - strokeWidth / 2,
                cropRectPaint);
    }

    private void drawCorners(Canvas canvas) {
        PointF topLeft = sourceToViewCoord(cropRect.left, cropRect.top);
        PointF bottomRight = sourceToViewCoord(cropRect.right, cropRect.bottom);

        Matrix matrix;
        RectF bounds = new RectF();
        getCornerPath().computeBounds(bounds, true);

        Path tlCorner = getCornerPath();
        matrix = new Matrix();
        matrix.postRotate(0, bounds.centerX(), bounds.centerY());
        matrix.postTranslate(topLeft.x, topLeft.y);
        tlCorner.transform(matrix);
        canvas.drawPath(tlCorner, cropRectCornerPaint);

        Path trCorner = getCornerPath();
        matrix = new Matrix();
        matrix.postRotate(90, bounds.centerX(), bounds.centerY());
        matrix.postTranslate(bottomRight.x - (bounds.width() + cornerStrokeWidth), topLeft.y);
        trCorner.transform(matrix);
        canvas.drawPath(trCorner, cropRectCornerPaint);

        Path brCorner = getCornerPath();
        matrix = new Matrix();
        matrix.postRotate(180, bounds.centerX(), bounds.centerY());
        matrix.postTranslate(bottomRight.x - (bounds.width() + cornerStrokeWidth),
                bottomRight.y - (bounds.height() + cornerStrokeWidth));
        brCorner.transform(matrix);
        canvas.drawPath(brCorner, cropRectCornerPaint);

        Path blCorner = getCornerPath();
        matrix = new Matrix();
        matrix.postRotate(270, bounds.centerX(), bounds.centerY());
        matrix.postTranslate(topLeft.x, bottomRight.y - (bounds.height() + cornerStrokeWidth));
        blCorner.transform(matrix);
        canvas.drawPath(blCorner, cropRectCornerPaint);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private Path getCornerPath() {
        Path corner = new Path();
        /*corner.moveTo(0, 0);
        corner.lineTo(cornerLength, 0);
        corner.lineTo(cornerLength, cornerStrokeWidth);
        corner.lineTo(cornerStrokeWidth, cornerStrokeWidth);
        corner.lineTo(cornerStrokeWidth, cornerLength);
        corner.lineTo(0, cornerLength);
        corner.close();*/
        corner.moveTo(cornerStrokeWidth / 2, cornerLength - cornerStrokeWidth / 2);
        corner.lineTo(cornerStrokeWidth / 2, cornerStrokeWidth / 2);
        corner.lineTo(cornerLength - cornerStrokeWidth / 2, cornerStrokeWidth / 2);
        return corner;
    }

    private void drawBackground(Canvas canvas) {
        PointF topLeft = sourceToViewCoord(cropRect.left, cropRect.top);
        PointF bottomRight = sourceToViewCoord(cropRect.right, cropRect.bottom);

        Path background = new Path();
        background.moveTo(0, 0);
        background.lineTo(getSWidth(), 0);
        background.lineTo(getSWidth(), getSHeight());
        background.lineTo(0, getSHeight());
        background.close();

        background.moveTo(topLeft.x, topLeft.y);
        background.lineTo(bottomRight.x, topLeft.y);
        background.lineTo(bottomRight.x, bottomRight.y);
        background.lineTo(topLeft.x, bottomRight.y);
        background.close();
        background.setFillType(Path.FillType.EVEN_ODD);

        backgroundPaint.setAlpha(touching ? 100 : 200);

        canvas.drawPath(background, backgroundPaint);
    }

    private void drawGuidelines(Canvas canvas) {
        PointF topLeft = sourceToViewCoord(cropRect.left, cropRect.top);
        PointF bottomRight = sourceToViewCoord(cropRect.right, cropRect.bottom);

        float width = bottomRight.x - topLeft.x;
        float height = bottomRight.y - topLeft.y;
        float thirdWidth = width / 3;
        float thirdHeight = height / 3;

        for (int i = 1; i <= 2; i++) {
            Path verticalGuideline = new Path();
            verticalGuideline.moveTo(topLeft.x + thirdWidth * i, topLeft.y + strokeWidth);
            verticalGuideline.lineTo(topLeft.x + thirdWidth * i, bottomRight.y - strokeWidth);
            canvas.drawPath(verticalGuideline, guidelinePaint);

            Path horizontalGuideline = new Path();
            horizontalGuideline.moveTo(topLeft.x + strokeWidth, topLeft.y + thirdHeight * i);
            horizontalGuideline.lineTo(bottomRight.x - strokeWidth, topLeft.y + thirdHeight * i);
            canvas.drawPath(horizontalGuideline, guidelinePaint);
        }
    }

    public State getCropImageViewState() {
        return new State(getState(), cropRect);
    }
}
