package us.koller.cameraroll.util;

import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

public class MediaType {

    public static boolean isMedia(String path) {
        return checkImageExtension(path) ||
                checkRAWExtension(path) ||
                checkGifExtension(path) ||
                checkVideoExtension(path);
    }

    public static String getMimeType(Context context, String path) {
        Uri uri = StorageUtil.getContentUriFromMediaStore(context, path);
        String mimeType = getMimeType(context, uri);
        if (mimeType != null) {
            return mimeType;
        }
        //try fileExtension
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(path);
        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
        return mimeType;
    }

    public static String getMimeType(Context context, Uri uri) {
        return context.getContentResolver().getType(uri);
    }

    //trying to check via mimeType
    public static boolean isImage(String path) {
        return path != null && checkImageExtension(path);
    }

    public static boolean isVideo(String path) {
        return path != null && checkVideoExtension(path);
    }

    public static boolean isGif(String path) {
        return path != null && checkGifExtension(path);
    }

    public static boolean isRAWImage(String path) {
        return path != null && checkRAWExtension(path);
    }


    //checking via extension
    private static String[] imageExtensions = {"jpg", "png", "jpe", "jpeg", "bmp"};
    private static String[] videoExtensions = {"mp4", "mkv", "webm", "avi"};
    private static String[] gifExtensions = {"gif"};
    private static String[] rawExtensions = {"dng"};
    private static String[] exifExtensions = {"jpg", "jpe", "jpeg", "bmp", "dng"};

    private static String[] imageMimeTypes = {"image/jpeg", "image/png", "image/bmp"};
    private static String[] videoMimeTypes = {"video/mp4", "video/x-matroska", "video/webm", "video/avi"};
    private static String[] gifMimeTypes = {"image/gif"};
    private static String[] rawMimeTypes = {"image/x-adobe-dng"};
    private static String[] exifMimeTypes = {"image/jpeg", "image/x-adobe-dng"};

    /*check mimeTypes*/
    public static boolean doesSupportExif_MimeType(String mimeType) {
        return checkExtension(mimeType, exifMimeTypes);
    }

    public static boolean checkImageMimeType(String mimeType) {
        return checkExtension(mimeType, imageMimeTypes);
    }

    public static boolean checkVideoMimeType(String mimeType) {
        return checkExtension(mimeType, videoMimeTypes);
    }

    public static boolean checkGifMimeType(String mimeType) {
        return checkExtension(mimeType, gifMimeTypes);
    }

    public static boolean checkRAWMimeType(String mimeType) {
        return checkExtension(mimeType, rawMimeTypes);
    }

    /*check fileExtensions*/
    public static boolean doesSupportExif_fileExtension(String path) {
        return checkExtension(path, exifExtensions);
    }

    private static boolean checkImageExtension(String path) {
        return checkExtension(path, imageExtensions);
    }

    private static boolean checkVideoExtension(String path) {
        return checkExtension(path, videoExtensions);
    }

    private static boolean checkGifExtension(String path) {
        return checkExtension(path, gifExtensions);
    }

    private static boolean checkRAWExtension(String path) {
        return checkExtension(path, rawExtensions);
    }


    private static boolean checkExtension(String path, String[] extensions) {
        for (int i = 0; i < extensions.length; i++) {
            if (path.toLowerCase().endsWith(extensions[i])) {
                return true;
            }
        }
        return false;
    }
}
