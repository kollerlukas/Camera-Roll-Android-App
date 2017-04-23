package us.koller.cameraroll.jobservices;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import us.koller.cameraroll.ui.MainActivity;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MediaJobService extends JobService {

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d("MediaJobService", "onStartJob() called with: jobParameters = [" + jobParameters + "]");

        MainActivity.refreshMediaWhenVisible = true;
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d("MediaJobService", "onStopJob() called with: jobParameters = [" + jobParameters + "]");
        return false;
    }
}
