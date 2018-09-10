package us.koller.cameraroll.data.fileOperations;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.models.File_POJO;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.StorageUtil;

public class Copy extends FileOperation {

    public static String getCopyFileName(String destinationPath) {
        File dir = new File(destinationPath);
        String copyName;
        if (dir.exists()) {
            copyName = dir.getPath();
            if (copyName.contains(".")) {
                int index = copyName.lastIndexOf(".");
                copyName = copyName.substring(0, index) + " Copy" + copyName.substring(index, copyName.length());
            } else {
                copyName = copyName + " Copy";
            }
        } else {
            copyName = dir.getPath();
        }
        return copyName;
    }

    //for files on non-removable storage
    private static boolean copyFile(String path, String destination) throws IOException {
        //create output directory if it doesn't exist
        File dir = new File(destination);
        if (new File(path).isDirectory()) {
            return dir.mkdirs();
        } else {
            if (dir.createNewFile()) {
                InputStream inputStream = new FileInputStream(path);
                OutputStream outputStream = new FileOutputStream(dir);
                return writeStream(inputStream, outputStream);
            }
        }
        return false;
    }

    //for files on removable storage
    static boolean copyFileOntoRemovableStorage(Context c, Uri treeUri,
                                                String path, String d) throws IOException { /* d = destination */
        String mimeType = MediaType.getMimeType(path);
        DocumentFile file = DocumentFile.fromFile(new File(d));
        if (file.exists()) {
            int index = d.lastIndexOf(".");
            d = d.substring(0, index) + " Copy"
                    + d.substring(index, d.length());
        }
        DocumentFile destinationFile = StorageUtil.createDocumentFile(c, treeUri, d, mimeType);

        if (destinationFile != null) {
            ContentResolver resolver = c.getContentResolver();
            OutputStream outputStream = resolver.openOutputStream(destinationFile.getUri());
            InputStream inputStream = new FileInputStream(path);
            return writeStream(inputStream, outputStream);
        }
        return false;
    }

    private static boolean writeStream(InputStream iS, OutputStream oS) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        //copy the file content in bytes
        while ((bytesRead = iS.read(buffer)) > 0) {
            oS.write(buffer, 0, bytesRead);
        }
        // write the output file
        oS.flush();
        oS.close();
        iS.close();
        return true;
    }

    @Override
    String getNotificationTitle() {
        return getString(R.string.copy);
    }

    @Override
    public int getNotificationSmallIconRes() {
        return R.drawable.ic_content_copy_white;
    }

    @Override
    public void execute(Intent workIntent) {
        File_POJO[] files = getFiles(workIntent);
        File_POJO target = workIntent.getParcelableExtra(TARGET);

        if (target == null) {
            return;
        }

        int success_count = 0;

        onProgress(success_count, files.length);

        boolean copyingOntoRemovableStorage = Util.isOnRemovableStorage(target.getPath());

        Uri treeUri = null;
        if (copyingOntoRemovableStorage) {
            treeUri = getTreeUri(workIntent, target.getPath());
            if (treeUri == null) {
                return;
            }
        }

        for (int i = files.length - 1; i >= 0; i--) {
            boolean result = copyFilesRecursively(getApplicationContext(), treeUri,
                    files[i].getPath(), target.getPath(), true);
            success_count += result ? 1 : 0;
            onProgress(success_count, files.length);
        }

        if (success_count == 0) {
            onProgress(success_count, files.length);
        }
    }

    @Override
    public int getType() {
        return FileOperation.COPY;
    }

    //treeUri only needed for removable storage
    boolean copyFilesRecursively(Context c, Uri treeUri, String path,
                                 String destination, boolean result) {
        Log.d("Copy", "copyFilesRecursively() path = [" + path + "]");
        File f = new File(path);
        String destinationFilePath = getCopyFileName(new File(destination, new File(path).getName()).getPath());
        try {
            if (treeUri == null) {
                //file is on non-removable storage
                result = result && copyFile(path, destinationFilePath);
            } else {
                //file is on removable storage
                if (f.isDirectory()) {
                    result = result && StorageUtil.createDocumentDir(c, treeUri, destinationFilePath) != null;
                } else {
                    result = result && copyFileOntoRemovableStorage(c, treeUri, path, destinationFilePath);
                }
            }
            if (!f.isDirectory()) {
                addPathToScan(destinationFilePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (f.isDirectory()) {
            File[] files = f.listFiles();
            for (int i = 0; i < files.length; i++) {
                copyFilesRecursively(c, treeUri, files[i].getPath(),
                        destination + "/" + new File(destinationFilePath).getName() + "/", result);
            }
        }
        return result;
    }
}
