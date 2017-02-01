package us.koller.cameraroll.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import us.koller.cameraroll.R;

public class Util {
    public static int getAlbumActivityGridColumnCount(Context context) {
        boolean landscape = context.getResources().getBoolean(R.bool.landscape);
        return !landscape ? 3 : 4;
    }

    public static int getScreenWidth(Activity context) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        return displaymetrics.widthPixels;
    }

    public static int[] getImageDimensions(Context context, String path) {
        int[] dimensions = new int[2];
        dimensions[0] = 1;
        dimensions[1] = 1;

        if (!path.startsWith("content")) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            BitmapFactory.decodeFile(path, options);
            dimensions[0] = options.outWidth > 0 ? options.outWidth : 1;
            dimensions[1] = options.outHeight > 0 ? options.outHeight : 1;
        }
        //Fix performance
        /*else {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), Uri.parse(path));
                dimensions[0] = bitmap.getWidth() > 0 ? bitmap.getWidth() : 1;
                dimensions[1] = bitmap.getHeight() > 0 ? bitmap.getHeight() : 1;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/

        return dimensions;
    }

    public static int[] getVideoDimensions(String path) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        retriever.setDataSource(path);
        Bitmap bitmap = retriever.getFrameAtTime();

        int[] dimensions = new int[2];

        dimensions[0] = bitmap.getWidth() > 0 ? bitmap.getWidth() : 1;
        dimensions[1] = bitmap.getHeight() > 0 ? bitmap.getHeight() : 1;
        return dimensions;

    }

    public static void setDarkStatusBarIcons(final View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            v.post(new Runnable() {
                @Override
                public void run() {
                    v.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
            });
        }
    }

    public static void setLightStatusBarIcons(final View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            v.post(new Runnable() {
                @Override
                public void run() {
                    v.setSystemUiVisibility(0);
                }
            });
        }
    }

    public static String getParentPath(String path) {
        return new File(path).getParent();
    }

    public static void showSnackbar(Snackbar snackbar) {
        snackbar.show();
        TextView textView = (TextView) (snackbar.getView())
                .findViewById(android.support.design.R.id.snackbar_text);
        textView.setTypeface(Typeface.create("sans-serif-monospace", Typeface.NORMAL));
    }

    public static Snackbar getPermissionDeniedSnackbar(final View rootView) {
        Snackbar snackbar = Snackbar.make(rootView,
                R.string.read_permission_denied,
                Snackbar.LENGTH_INDEFINITE);
        snackbar.getView().setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Toast.makeText(rootView.getContext(), R.string.read_permission_denied, Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        return snackbar;
    }

    public static long getDateAdded(Activity context, String path) {

        long dateAdded = new File(path).lastModified();

        String[] projection = {MediaStore.Images.Media.DATE_TAKEN};

        // Make the query.
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(Uri.parse(path),
                projection, // Which columns to return
                null,       // Which rows to return (all rows)
                null,       // Selection arguments (none)
                MediaStore.Images.Media.DATE_TAKEN        // Ordering
        );

        if (cursor == null) {
            return dateAdded;
        }

        cursor.moveToFirst();
        String dateTaken = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN));
        cursor.close();

        dateAdded = Long.parseLong(dateTaken);

        return dateAdded;
    }
}
