package us.koller.cameraroll.ui.widget;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.ImageViewState;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import us.koller.cameraroll.R;
import us.koller.cameraroll.imageDecoder.CustomRegionDecoder;
import us.koller.cameraroll.imageDecoder.RAWImageBitmapRegionDecoder;
import us.koller.cameraroll.util.MediaType;
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

    private static final int TOUCH_DELTA_DP = 20;

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
    // aspectRation < 0: free aspect ratio; otherwise: deltaY * aspectRation = deltaX
    private double aspectRatio = -1.0;

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

    /**
     * Result object for the imageView, containing the cropped bitmap.
     **/
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

    /**
     * Store the current state of the ImageView to preserve it across configuration changes.
     **/
    public static class State extends ImageViewState {

        private int[] cropRect;
        private double aspectRatio;

        State(float scale, PointF center, int orientation, Rect cropRect, double aspectRatio) {
            super(scale, center, orientation);
            this.cropRect = new int[]{
                    cropRect.left, cropRect.top,
                    cropRect.right, cropRect.bottom};
            this.aspectRatio = aspectRatio;
        }

        Rect getCropRect() {
            return new Rect(
                    cropRect[0], cropRect[1],
                    cropRect[2], cropRect[3]);
        }

        double getAspectRatio() {
            return aspectRatio;
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
        setOrientation(0);
        setMinScale(0.01f);
        setMinimumScaleType(SCALE_TYPE_CUSTOM);

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

    /**
     * Load an image into the imageView.
     *
     * @param uri   for the image to be loaded
     * @param state the state for the imageView (might be null)
     **/
    public void loadImage(Uri uri, State state) {
        setProgressBarVisibility(VISIBLE);

        imageUri = uri;

        String mimeType = MediaType.getMimeType(getContext(), imageUri);
        if (MediaType.checkRAWMimeType(mimeType)) {
            setRegionDecoderClass(RAWImageBitmapRegionDecoder.class);
        } else {
            setRegionDecoderClass(CustomRegionDecoder.class);
        }

        if (state != null) {
            cropRect = state.getCropRect();
            setAspectRatio(state.getAspectRatio());
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
            cropRect = getMaxCenteredCropRect();
            Log.d("CropImageView", "onImageLoaded: " + cropRect);
        }
        autoZoom(false);

        setProgressBarVisibility(GONE);
    }

    /**
     * Rotate the image by 90°.
     **/
    public void rotate90Degree() {
        cropRect = rotateRect90Degree(cropRect);
        int orientation = getOrientation() + 90;
        if (orientation >= 360) {
            orientation = orientation % 360;
        }
        setOrientation(orientation);
        // invert the aspectRatio
        aspectRatio = 1 / aspectRatio;
        post(new Runnable() {
            @Override
            public void run() {
                autoZoom(false);
            }
        });
    }

    /**
     * Restore the ImageView to initial state.
     **/
    public void restore() {
        setOrientation(0);
        cropRect = getMaxCenteredCropRect();
        autoZoom(false);
    }

    /**
     * Method to call when the cropped bitmap is needed.
     *
     * @param onResultListener listener for the resulting cropped bitmap
     **/
    public void getCroppedBitmap(final OnResultListener onResultListener) {
        setProgressBarVisibility(VISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ContentResolver resolver = getContext().getContentResolver();
                    InputStream inputStream = resolver.openInputStream(imageUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                    //rotate image
                    Matrix matrix = new Matrix();
                    matrix.postRotate(getOrientation() + getRotation());

                    bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                            bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                    byte[] bitmapData = outputStream.toByteArray();
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bitmapData);

                    BitmapRegionDecoder decoder = BitmapRegionDecoder.
                            newInstance(byteArrayInputStream, false);

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 1;
                    options.inJustDecodeBounds = false;

                    Bitmap croppedBitmap = decoder.decodeRegion(cropRect, options);
                    decoder.recycle();

                    final Result result = new Result(imageUri, croppedBitmap);
                    CropImageView.this.post(new Runnable() {
                        @Override
                        public void run() {
                            onResultListener.onResult(result);
                            setProgressBarVisibility(GONE);
                        }
                    });
                } catch (Exception | OutOfMemoryError e) {
                    e.printStackTrace();
                    CropImageView.this.post(new Runnable() {
                        @Override
                        public void run() {
                            onResultListener.onResult(new Result(getImageUri(), null));
                            setProgressBarVisibility(GONE);
                        }
                    });
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
                    Rect newCropRect = getNewRect(motionEvent.getX(), motionEvent.getY());
                    if (newCropRect != null) {
                        cropRect = newCropRect;
                    }
                    consumed = true;
                } else {
                    Rect newCropRect = getMovedRect(motionEvent);
                    if (newCropRect != null) {
                        cropRect = newCropRect;
                    }
                    consumed = true;
                }

                if (cropRect != null) {
                    PointF center = getCenterOfCropRect();
                    float scale = getScale();
                    float newScale = getNewScale();
                    setScaleAndCenter(newScale < scale ? newScale : scale, center);
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                //auto-zoom
                if (cropRect != null) {
                    autoZoom(true);
                    touching = false;
                    touchedCorner = NO_CORNER;
                    invalidate();
                }
                break;
            default:
                break;
        }
        // Use parent to handle pinch and two-finger pan.
        return consumed || super.onTouchEvent(motionEvent);
    }

    /**
     * Get the center of the current cropRect.
     *
     * @return the center of the current cropRect
     **/
    private PointF getCenterOfCropRect() {
        return new PointF(
                cropRect.centerX(),
                cropRect.centerY());
    }

    /**
     * Calculate the new zoom Scale for the image
     *
     * @return new scale
     **/
    private float getNewScale() {
        int width = getWidth() - (padding[0] + padding[2]);
        float scaleWidth = (float) width / (cropRect.right - cropRect.left);

        int height = getHeight() - (padding[1] + padding[3]);
        float scaleHeight = (float) height / (cropRect.bottom - cropRect.top);
        if (getHeight() > getWidth()) {
            return scaleWidth < scaleHeight ? scaleWidth : scaleHeight;
        }
        return scaleWidth < scaleHeight ? scaleWidth : scaleHeight;
    }

    /**
     * Set if the ImageView should zoom in and out according to the current cropRect
     **/
    private void autoZoom(boolean animate) {
        //auto-zoom
        float scale = getNewScale();
        PointF center = getCenterOfCropRect();
        if (animate) {
            animateScaleAndCenter(scale, center)
                    .withDuration(300)
                    .withInterruptible(false)
                    .start();
        } else {
            setScaleAndCenter(scale, center);
        }
    }

    /**
     * Returns the touched corner to the associated motionEvent.
     *
     * @param motionEvent the associated MotionEvent
     * @return one of: NO_CORNER, TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT or BOTTOM_LEFT
     **/
    private int getTouchedCorner(MotionEvent motionEvent) {
        PointF currentTouchPos = new PointF(motionEvent.getX(), motionEvent.getY());
        if (cropRect == null) {
            return NO_CORNER;
        }
        PointF topLeft = sourceToViewCoord(cropRect.left, cropRect.top);
        PointF bottomRight = sourceToViewCoord(cropRect.right, cropRect.bottom);
        Rect cropRect = new Rect((int) topLeft.x, (int) topLeft.y,
                (int) bottomRight.x, (int) bottomRight.y);

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

    /**
     * Get the new cropRect after a motionEvent from the x and y coordinate.
     *
     * @param x x-coordinate of the associated MotionEvent
     * @param y y-coordinate of the associated MotionEvent
     * @return the new cropRect; might be null if cropRect didn't change; bounds need to be checked!
     **/
    private Rect getNewRect(float x, float y) {
        PointF currentTouchPos = viewToSourceCoord(x, y);

        boolean freeAspectRatio = aspectRatio < 0.0;

        if (freeAspectRatio) {
            if (touchedCorner == TOP_LEFT) {
                return checkRectBounds(new Rect((int) currentTouchPos.x, (int) currentTouchPos.y,
                        cropRect.right, cropRect.bottom), true);
            } else if (touchedCorner == TOP_RIGHT) {
                return checkRectBounds(new Rect(cropRect.left, (int) currentTouchPos.y,
                        (int) currentTouchPos.x, cropRect.bottom), true);
            } else if (touchedCorner == BOTTOM_RIGHT) {
                return checkRectBounds(new Rect(cropRect.left, cropRect.top,
                        (int) currentTouchPos.x, (int) currentTouchPos.y), true);
            } else if (touchedCorner == BOTTOM_LEFT) {
                return checkRectBounds(new Rect((int) currentTouchPos.x, cropRect.top,
                        cropRect.right, (int) currentTouchPos.y), true);
            }
        } else {
            // fixed aspectRatio
            if (touchedCorner == TOP_LEFT) {
                int delta = (int) Math.max(currentTouchPos.x - cropRect.left, currentTouchPos.y - cropRect.top);
                return checkRectBounds(new Rect((int) Math.round(cropRect.left + delta * aspectRatio), cropRect.top + delta,
                        cropRect.right, cropRect.bottom), true);
            } else if (touchedCorner == TOP_RIGHT) {
                int delta = (int) Math.max(cropRect.right - currentTouchPos.x, currentTouchPos.y - cropRect.top);
                return checkRectBounds(new Rect(cropRect.left, cropRect.top + delta,
                        (int) Math.round(cropRect.right - delta * aspectRatio), cropRect.bottom), true);
            } else if (touchedCorner == BOTTOM_RIGHT) {
                int delta = (int) Math.max(cropRect.right - currentTouchPos.x, cropRect.bottom - currentTouchPos.y);
                return checkRectBounds(new Rect(cropRect.left, cropRect.top,
                        (int) Math.round(cropRect.right - delta * aspectRatio), cropRect.bottom - delta), true);
            } else if (touchedCorner == BOTTOM_LEFT) {
                int delta = (int) Math.max(currentTouchPos.x - cropRect.left, cropRect.bottom - currentTouchPos.y);
                return checkRectBounds(new Rect((int) Math.round(cropRect.left + delta * aspectRatio), cropRect.top,
                        cropRect.right, cropRect.bottom - delta), true);
            }
        }

        return null;
    }

    /**
     * Get the moved cropRect from the motionEvent.
     *
     * @param motionEvent the associated MotionEvent
     * @return the moved cropRect; might be null if cropRect didn't change
     **/
    private Rect getMovedRect(MotionEvent motionEvent) {
        if (cropRect == null) {
            return null;
        }

        PointF currentTouchPos = viewToSourceCoord(motionEvent.getX(),
                motionEvent.getY());

        int historySize = motionEvent.getHistorySize();
        if (historySize > 0) {
            PointF oldTouchPos = viewToSourceCoord(motionEvent.getHistoricalX(0),
                    motionEvent.getHistoricalY(0));
            int deltaX = (int) (oldTouchPos.x - currentTouchPos.x);
            int deltaY = (int) (oldTouchPos.y - currentTouchPos.y);

            return checkRectBounds(new Rect(
                    cropRect.left + deltaX,
                    cropRect.top + deltaY,
                    cropRect.right + deltaX,
                    cropRect.bottom + deltaY), false);
        } else {
            return cropRect;
        }
    }

    /**
     * Checks the bounds and size of the current cropRect:
     * - checks if inside image
     * - checks that cropRect is bigger than min size
     *
     * @param cropRect the Rect that should be checked
     * @param resize   flag if the cropRect can be resized or only translated to be made valid
     * @return a valid cropRect; might be null if cropRect didn't change
     **/
    private Rect checkRectBounds(Rect cropRect, boolean resize) {
        Rect image = getImageRect();
        Rect newCropRect = cropRect;
        //check if inside image
        int width = newCropRect.width();
        int height = newCropRect.height();

        if (!image.contains(newCropRect)) {
            if (aspectRatio >= 0.0) {
                if (resize) {
                    // new cropRect to big => try and fix size
                    // check corners
                    if (touchedCorner == TOP_LEFT) {
                        if (image.left > newCropRect.left) {
                            int delta = (int) ((image.left - newCropRect.left) / aspectRatio);
                            newCropRect = new Rect(image.left, newCropRect.top + delta,
                                    newCropRect.right, newCropRect.bottom);
                        }
                        if (image.top > newCropRect.top) {
                            int delta = (int) ((image.top - newCropRect.top) * aspectRatio);
                            newCropRect = new Rect(newCropRect.left + delta, image.top,
                                    newCropRect.right, newCropRect.bottom);
                        }
                    } else if (touchedCorner == TOP_RIGHT) {
                        if (image.right < newCropRect.right) {
                            int delta = (int) ((newCropRect.right - image.right) / aspectRatio);
                            newCropRect = new Rect(newCropRect.left, newCropRect.top + delta,
                                    image.right, newCropRect.bottom);
                        }
                        if (image.top > newCropRect.top) {
                            int delta = (int) ((image.top - newCropRect.top) * aspectRatio);
                            newCropRect = new Rect(newCropRect.left, image.top,
                                    newCropRect.right - delta, newCropRect.bottom);
                        }
                    } else if (touchedCorner == BOTTOM_RIGHT) {
                        if (image.right < newCropRect.right) {
                            int delta = (int) ((newCropRect.right - image.right) / aspectRatio);
                            newCropRect = new Rect(newCropRect.left, newCropRect.top,
                                    image.right, newCropRect.bottom - delta);
                        }
                        if (image.bottom < newCropRect.bottom) {
                            int delta = (int) ((newCropRect.bottom - image.bottom) * aspectRatio);
                            newCropRect = new Rect(newCropRect.left, newCropRect.top,
                                    newCropRect.right - delta, image.bottom);
                        }
                    } else if (touchedCorner == BOTTOM_LEFT) {
                        if (image.left > newCropRect.left) {
                            int delta = (int) ((image.left - newCropRect.left) / aspectRatio);
                            newCropRect = new Rect(image.left, newCropRect.top,
                                    newCropRect.right, newCropRect.bottom - delta);
                        }
                        if (image.bottom < newCropRect.bottom) {
                            int delta = (int) ((newCropRect.bottom - image.bottom) * aspectRatio);
                            newCropRect = new Rect(newCropRect.left + delta, newCropRect.top,
                                    newCropRect.right, image.bottom);
                        }
                    }
                } else {
                    // check edges
                    // left edges
                    if (image.left > newCropRect.left) {
                        newCropRect = new Rect(image.left, newCropRect.top,
                                image.left + width, newCropRect.bottom);
                    }
                    // top edge
                    if (image.top > newCropRect.top) {
                        newCropRect = new Rect(newCropRect.left, image.top,
                                newCropRect.right, image.top + height);
                    }
                    // right edge
                    if (image.right < newCropRect.right) {
                        newCropRect = new Rect(image.right - width, newCropRect.top,
                                image.right, newCropRect.bottom);
                    }
                    // bottom edge
                    if (image.bottom < newCropRect.bottom) {
                        newCropRect = new Rect(newCropRect.left, image.bottom - height,
                                newCropRect.right, image.bottom);
                    }
                }
            } else {
                // cropRect not inside => try to fix it
                if (image.left > newCropRect.left) {
                    newCropRect = new Rect(image.left, newCropRect.top,
                            resize ? newCropRect.right : image.left + width,
                            newCropRect.bottom);
                }

                if (image.top > newCropRect.top) {
                    newCropRect = new Rect(newCropRect.left, image.top, newCropRect.right,
                            resize ? newCropRect.bottom : image.top + height);
                }

                if (image.right < newCropRect.right) {
                    newCropRect = new Rect(resize ? newCropRect.left : image.right - width,
                            newCropRect.top, image.right, newCropRect.bottom);
                }

                if (image.bottom < newCropRect.bottom) {
                    newCropRect = new Rect(newCropRect.left,
                            resize ? newCropRect.top : image.bottom - height,
                            newCropRect.right, image.bottom);
                }
            }
        }

        Rect minRect = getMinCropRect();
        //check min size
        width = newCropRect.width();
        if (width < minRect.width()) {
            if (touchedCorner == TOP_LEFT) {
                newCropRect = new Rect(newCropRect.right - minRect.width(),
                        newCropRect.top, newCropRect.right, newCropRect.bottom);
            } else if (touchedCorner == TOP_RIGHT) {
                newCropRect = new Rect(newCropRect.left, newCropRect.top,
                        newCropRect.left + minRect.width(),
                        newCropRect.bottom);
            } else if (touchedCorner == BOTTOM_RIGHT) {
                newCropRect = new Rect(newCropRect.left, newCropRect.top,
                        newCropRect.left + minRect.width(),
                        newCropRect.bottom);
            } else if (touchedCorner == BOTTOM_LEFT) {
                newCropRect = new Rect(newCropRect.right - minRect.width(),
                        newCropRect.top, newCropRect.right, newCropRect.bottom);
            }
        }

        height = newCropRect.height();
        if (height < minRect.height()) {
            if (touchedCorner == TOP_LEFT) {
                newCropRect = new Rect(newCropRect.left,
                        newCropRect.bottom - minRect.height(),
                        newCropRect.right, newCropRect.bottom);
            } else if (touchedCorner == TOP_RIGHT) {
                newCropRect = new Rect(newCropRect.left,
                        newCropRect.bottom - minRect.height(),
                        newCropRect.right, newCropRect.bottom);
            } else if (touchedCorner == BOTTOM_RIGHT) {
                newCropRect = new Rect(newCropRect.left, newCropRect.top,
                        newCropRect.right,
                        newCropRect.top + minRect.height());
            } else if (touchedCorner == BOTTOM_LEFT) {
                newCropRect = new Rect(newCropRect.left,
                        newCropRect.top, newCropRect.right,
                        newCropRect.top + minRect.height());
            }
        }

        return newCropRect;
    }

    private Rect getImageRect() {
        switch (getOrientation()) {
            case 90:
            case 270:
                return new Rect(0, 0, getSHeight(), getSWidth());
            default:
                return new Rect(0, 0, getSWidth(), getSHeight());
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private Rect rotateRect90Degree(Rect cropRect) {
        Rect imageRect = getImageRect();
        int newWidth = cropRect.height();
        int newHeight = cropRect.width();
        Point newTopLeft = new Point(imageRect.height() - (cropRect.top + cropRect.height()), cropRect.left);
        return new Rect(newTopLeft.x, newTopLeft.y,
                newTopLeft.x + newWidth, newTopLeft.y + newHeight);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
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
        if (topLeft == null || bottomRight == null) {
            return;
        }
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
        if (topLeft == null || bottomRight == null) {
            return;
        }

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
        corner.moveTo(cornerStrokeWidth / 2, cornerLength - cornerStrokeWidth / 2);
        corner.lineTo(cornerStrokeWidth / 2, cornerStrokeWidth / 2);
        corner.lineTo(cornerLength - cornerStrokeWidth / 2, cornerStrokeWidth / 2);
        return corner;
    }

    private void drawBackground(Canvas canvas) {
        Rect imageRect = getImageRect();
        PointF topLeftImageRect = sourceToViewCoord(imageRect.left, imageRect.top);
        PointF bottomRightImageRect = sourceToViewCoord(imageRect.right, imageRect.bottom);

        PointF topLeft = sourceToViewCoord(cropRect.left, cropRect.top);
        PointF bottomRight = sourceToViewCoord(cropRect.right, cropRect.bottom);
        if (topLeftImageRect == null || bottomRightImageRect == null ||
                topLeft == null || bottomRight == null) {
            return;
        }

        Path background = new Path();
        background.setFillType(Path.FillType.EVEN_ODD);
        background.moveTo(topLeft.x, topLeft.y);
        background.lineTo(bottomRight.x, topLeft.y);
        background.lineTo(bottomRight.x, bottomRight.y);
        background.lineTo(topLeft.x, bottomRight.y);
        background.close();

        background.moveTo((int) topLeftImageRect.x, (int) topLeftImageRect.y);
        background.lineTo(bottomRightImageRect.x, (int) topLeftImageRect.y);
        background.lineTo(bottomRightImageRect.x, bottomRightImageRect.y);
        background.lineTo((int) topLeftImageRect.x, bottomRightImageRect.y);
        background.close();

        backgroundPaint.setAlpha(touching ? 100 : 200);

        canvas.drawPath(background, backgroundPaint);
    }

    private void drawGuidelines(Canvas canvas) {
        PointF topLeft = sourceToViewCoord(cropRect.left, cropRect.top);
        PointF bottomRight = sourceToViewCoord(cropRect.right, cropRect.bottom);
        if (topLeft == null || bottomRight == null) {
            return;
        }

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
        ImageViewState state = getState();
        if (state != null) {
            return new State(state.getScale(), state.getCenter(), state.getOrientation(),
                    cropRect, aspectRatio);
        }
        return null;
    }

    private ProgressBar getProgressBar() {
        ViewGroup parent = (ViewGroup) getParent();
        return parent.findViewById(R.id.progress_bar);
    }

    private void setProgressBarVisibility(int visibility) {
        ProgressBar progressBar = getProgressBar();
        if (progressBar != null) {
            progressBar.setVisibility(visibility);
        }
    }

    @SuppressWarnings("unused")
    public double getAspectRatio() {
        return aspectRatio;
    }

    /**
     * Set the an aspectRatio for the cropRect.
     *
     * @param aspectRatio smaller 0: free aspectRatio
     *                    greater 0: a = aspectRation * b; where a and b are the sides of the cropping rect
     **/
    public void setAspectRatio(double aspectRatio) {
        if (this.aspectRatio == aspectRatio) {
            return;
        }
        this.aspectRatio = aspectRatio;
        // update the cropRect
        if (isImageLoaded() && aspectRatio > 0.0) {
            cropRect = getMaxCenteredCropRect();
            autoZoom(false);
        }
    }

    /**
     * Sets the aspect ratio to free.
     **/
    public void setFreeAspectRatio() {
        setAspectRatio(-1.0);
    }

    /**
     * Set the original aspect ratio of the image as fixed aspect ratio.
     **/
    public void setOriginalAspectRatioFixed() {
        if (!isImageLoaded()) {
            // if no image was loaded set the aspect ratio to free
            setAspectRatio(-1.0);
            return;
        }
        Rect imageRect = getImageRect();
        setAspectRatio((double) (imageRect.width()) / (double) (imageRect.height()));
    }

    /**
     * Returns the max possible cropRect that is inside the image.
     *
     * @return max centered cropRect; might be null if no image was loaded
     **/
    private Rect getMaxCenteredCropRect() {
        if (!isImageLoaded()) {
            return null;
        }
        if (aspectRatio < 0.0) {
            return getImageRect();
        } else {
            Rect imageRect = getImageRect();
            int imageHeight = imageRect.bottom - imageRect.top, imageWidth = imageRect.right - imageRect.left;
            if (imageHeight * aspectRatio <= imageWidth) {
                int padding = (int) ((imageWidth - (imageHeight * aspectRatio)) / 2);
                return new Rect(padding,
                        0,
                        (int) ((imageHeight * aspectRatio) + padding),
                        imageHeight);
            } else {
                int padding = (int) ((imageHeight - (imageWidth / aspectRatio)) / 2);
                return new Rect(0,
                        padding,
                        imageWidth,
                        (int) ((imageWidth / aspectRatio) + padding));
            }
        }
    }

    private Rect getMinCropRect() {
        return new Rect(0, 0,
                aspectRatio < 0.0 ? minCropRectSize : (int) (minCropRectSize * aspectRatio),
                minCropRectSize);
    }
}