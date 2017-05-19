package us.koller.cameraroll.data.FileOperations;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import java.lang.ref.WeakReference;

public class FileOperationManager {

    private static FileOperationManager manager;

    public static FileOperationManager getInstance() {
        if (manager == null) {
            manager = new FileOperationManager();
        }
        return manager;
    }

    private ProgressUpdater progressUpdater;

    public void setProgressUpdater(ProgressUpdater progressUpdater) {
        this.progressUpdater = progressUpdater;
    }

    public void onProgress(String action, int progress, int totalNumber) {
        if (progressUpdater != null) {
            progressUpdater.onProgress(action, progress, totalNumber);
        }
    }


    public interface ProgressUpdater {
        void onProgress(String action, int progress, int totalNumber);
    }

    public static class ToastUpdater implements ProgressUpdater {

        private WeakReference<Toast> toastWeakReference;

        @SuppressLint("ShowToast")
        public ToastUpdater(Context context) {
            if (toastWeakReference == null) {
                toastWeakReference = new WeakReference<>(
                        Toast.makeText(context, "", Toast.LENGTH_SHORT));
            }
        }

        @Override
        public void onProgress(String action, int progress, int totalNumber) {
            final String text = action + String.valueOf(progress) + "/"
                    + String.valueOf(totalNumber);

            Toast toast = toastWeakReference.get();
            if (toast != null && toast.getView() != null
                    && toast.getView().getContext() instanceof Activity) {
                Activity a = (Activity) toast.getView().getContext();
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast toast = toastWeakReference.get();
                        toast.setText(text);
                        toast.show();
                    }
                });
            }
        }
    }
}
