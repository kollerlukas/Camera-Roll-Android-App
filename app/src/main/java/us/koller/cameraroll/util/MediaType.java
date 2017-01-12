package us.koller.cameraroll.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

public class MediaType {
    public static boolean isImage(Context context, String path) {
        if (path == null) {
            return false;
        }

        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(path);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
        if (mimeType == null) {
            mimeType = context.getContentResolver().getType(Uri.parse(path));
        }
        return mimeType != null && mimeType.contains("image");
    }

    public static boolean isVideo(Context context, String path) {
        if (path == null) {
            return false;
        }

        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(path);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
        if (mimeType == null) {
            mimeType = context.getContentResolver().getType(Uri.parse(path));
        }
        return mimeType != null && mimeType.contains("video");
    }

    public static boolean isGif(Context context, String path) {
        if (path == null || !isImage(context, path)) {
            return false;
        }
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(path);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
        if (mimeType == null) {
            mimeType = context.getContentResolver().getType(Uri.parse(path));
        }
        return mimeType != null && mimeType.contains("gif");
    }
}
