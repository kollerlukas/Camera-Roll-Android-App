package us.koller.cameraroll.util;

import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

public class MediaType {

    private static String[] imageExtensions = {"jpg", "png", "jpe", "jpeg", "bmp"};
    private static String[] imageMimeTypes = {"image/*", "image/jpeg", "image/jpg", "image/png", "image/bmp"};

    private static String[] videoExtensions = {"mp4", "mkv", "webm", "avi"};
    private static String[] videoMimeTypes = {"video/*", "video/mp4", "video/x-matroska", "video/webm", "video/avi"};

    private static String[] gifExtensions = {"gif"};
    private static String[] gifMimeTypes = {"image/gif"};

    private static String[] rawExtensions = {"dng", "cr2", "arw"};
    private static String[] rawMimeTypes = {"image/x-adobe-dng", "image/x-canon-cr2", "image/arw", "image/x-sony-arw"};

    private static String[] exifExtensions = {"jpg", "jpe", "jpeg", "dng", "cr2"};
    private static String[] exifMimeTypes = {"image/jpeg", "image/jpg", "image/x-adobe-dng", "image/x-canon-cr2"};

    private static String[] exifWritingExtensions = {"jpg", "jpe", "jpeg"};
    private static String[] exifWritingMimeTypes = {"image/jpeg", "image/jpg"};

    private static String[] wallpaperMimeTypes = {"image/jpeg", "image/png"};

    public static boolean isMedia(String path) {
        return checkImageExtension(path) ||
                checkRAWExtension(path) ||
                checkGifExtension(path) ||
                checkVideoExtension(path);
    }

    public static String getMimeType(String path) {
        if (path == null) {
            return null;
        }
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(path);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
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

    /*check mimeTypes*/
    public static boolean doesSupportExifMimeType(String mimeType) {
        return checkExtension(mimeType, exifMimeTypes);
    }

    public static boolean doesSupportWritingExifMimeType(String mimeType) {
        return checkExtension(mimeType, exifWritingMimeTypes);
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
    @SuppressWarnings("unused")
    public static boolean doesSupportExifFileExtension(String path) {
        return checkExtension(path, exifExtensions);
    }

    @SuppressWarnings("unused")
    public static boolean doesSupportWritingExifFileExtension(String path) {
        return checkExtension(path, exifWritingExtensions);
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

    @SuppressWarnings("DefaultLocale")
    private static boolean checkExtension(String path, String[] extensions) {
        if (path == null) {
            return false;
        }
        for (String extension : extensions) {
            if (path.toLowerCase().endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    public static boolean suitableAsWallpaper(Context context, Uri uri) {
        if (uri != null) {
            String mimeType = getMimeType(context, uri);
            return mimeType != null && checkExtension(mimeType, wallpaperMimeTypes);
        }
        return false;
    }
}
