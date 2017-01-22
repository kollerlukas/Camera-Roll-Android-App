package us.koller.cameraroll.util;

import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

public class MediaType {
    public static String getMimeType(Context context, String path) {
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(path);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
        if (mimeType == null) {
            mimeType = context.getContentResolver().getType(Uri.parse(path));
        }
        return mimeType;
    }

    public static boolean isImage(Context context, String path) {
        if (path != null) {
            String mimeType = getMimeType(context, path);
            return mimeType != null && mimeType.contains("image");
        }
        return false;
    }

    public static boolean isVideo(Context context, String path) {
        if (path != null) {
            String mimeType = getMimeType(context, path);
            return mimeType != null && mimeType.contains("video");
        }
        return false;
    }

    public static boolean isGif(Context context, String path) {
        if (path == null || isImage(context, path)) {
            String mimeType = getMimeType(context, path);
            return mimeType != null && mimeType.contains("gif");
        }
        return false;
    }
}
