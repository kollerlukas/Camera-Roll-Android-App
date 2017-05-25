package us.koller.cameraroll.data.FileOperations;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.File_POJO;

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

        for (int i = files.length - 1; i >= 0; i--) {
            boolean result = copyFilesRecursively(getApplicationContext(), files[i].getPath(), target.getPath(), true);
            success_count += result ? 1 : 0;
            onProgress(s, success_count, files.length);
        }

        if (success_count == 0) {
            onProgress(s, success_count, files.length);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public int getType() {
        return FileOperation.COPY;
    }

    private static boolean copyFilesRecursively(Context context, String path,
                                                String destination, boolean result) {
        File file = new File(path);
        String destinationFileName
                = getCopyFileName(new File(destination, new File(path).getName()).getPath());
        try {
            result = result && copyFile(path, destinationFileName);

            FileOperation.Util.scanPaths(context, new String[]{path, destinationFileName});
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                copyFilesRecursively(context, files[i].getPath(),
                        destination + "/" + new File(destinationFileName).getName() + "/", result);
            }
        }
        return result;
    }

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
}
