package us.koller.cameraroll.data.FileOperations;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.provider.DocumentFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.data.Provider.ItemLoader.AlbumLoader;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.StorageUtil;

public class Copy extends FileOperation {

    @Override
    void execute(Intent workIntent) {
        File_POJO[] files = getFiles(workIntent);
        File_POJO target = workIntent.getParcelableExtra(TARGET);

        if (target == null) {
            return;
        }

        String s = getString(R.string.successfully_copied);

        int success_count = 0;

        onProgress(s, success_count, files.length);

        //check if file is on removable storage
        boolean copyingOntoRemovableStorage =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                        Environment.isExternalStorageRemovable(new File(target.getPath()));

        Uri treeUri = null;
        if (copyingOntoRemovableStorage) {
            treeUri = getTreeUri(workIntent);
            if (treeUri == null) {
                return;
            }
        }

        for (int i = files.length - 1; i >= 0; i--) {
            boolean result = copyFilesRecursively(getApplicationContext(), treeUri,
                    files[i].getPath(), target.getPath(), true);

            success_count += result ? 1 : 0;
            onProgress(s, success_count, files.length);
        }

        if (success_count == 0) {
            onProgress(s, success_count, files.length);
        }
    }

    @Override
    public int getType() {
        return FileOperation.COPY;
    }

    private static String getCopyFileName(String destinationPath) {
        File dir = new File(destinationPath);
        String copyName;
        if (dir.exists()) {
            copyName = dir.getPath();
            if (copyName.contains(".")) {
                int index = copyName.lastIndexOf(".");
                copyName = copyName.substring(0, index) + " Copy"
                        + copyName.substring(index, copyName.length());
            } else {
                copyName = copyName + " Copy";
            }
        } else {
            copyName = dir.getPath();
        }
        return copyName;
    }

    //treeUri only needed for removable storage
    private static boolean copyFilesRecursively(Context context, Uri treeUri, String path,
                                                String destination, boolean result) {
        File file = new File(path);
        String destinationFileName
                = getCopyFileName(new File(destination, new File(path).getName()).getPath());
        try {
            if (treeUri == null) {
                //file is on non-removable storage
                result = result && copyFile(path, destinationFileName);
            } else {
                //file is on removable storage
                if (file.isDirectory()) {
                    result = result && StorageUtil.createDocumentDir(context, treeUri, destinationFileName) != null;
                } else {
                    result = result && copyFileOntoRemovableStorage(context, treeUri, path, destinationFileName);
                }
            }

            if (!file.isDirectory()) {
                AlbumItem oldAlbumItem = AlbumItem.getInstance(context, path);
                AlbumLoader.tryToLoadDateTakenFromMediaStore(context, oldAlbumItem);
                long dateAdded = oldAlbumItem.getDateTaken();

                FileOperation.Util.scanPaths(context,
                        new String[]{path, destinationFileName},
                        new long[]{-1, dateAdded}, null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                copyFilesRecursively(context, treeUri, files[i].getPath(),
                        destination + "/" + new File(destinationFileName).getName() + "/", result);
            }
        }
        return result;
    }

    //for files on non-removable storage
    private static boolean copyFile(String path, String destination) throws IOException {
        //create output directory if it doesn't exist
        File dir = new File(destination);
        if (new File(path).isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        } else {
            //noinspection ResultOfMethodCallIgnored
            dir.createNewFile();
        }

        InputStream inputStream = new FileInputStream(path);
        OutputStream outputStream = new FileOutputStream(dir);
        return writeStream(inputStream, outputStream);
    }

    //for files on removable storage
    static boolean copyFileOntoRemovableStorage(Context context, Uri treeUri,
                                                String path, String destination) throws IOException {
        String mimeType = MediaType.getMimeType(context, path);
        DocumentFile file = DocumentFile.fromFile(new File(destination));
        if (file.exists()) {
            int index = destination.lastIndexOf(".");
            destination = destination.substring(0, index) + " Copy"
                    + destination.substring(index, destination.length());
        }
        DocumentFile destinationFile = StorageUtil.createDocumentFile(context, treeUri, destination, mimeType);

        if (destinationFile != null) {
            ContentResolver resolver = context.getContentResolver();
            OutputStream outputStream = resolver.openOutputStream(destinationFile.getUri());
            InputStream inputStream = new FileInputStream(path);
            return writeStream(inputStream, outputStream);
        }
        return false;
    }


    private static boolean writeStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        //copy the file content in bytes
        while ((bytesRead = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, bytesRead);
        }
        // write the output file
        outputStream.flush();
        outputStream.close();

        inputStream.close();

        return true;
    }
}
