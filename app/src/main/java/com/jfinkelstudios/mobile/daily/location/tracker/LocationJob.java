package com.jfinkelstudios.mobile.daily.location.tracker;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

public final class LocationJob extends JobService {
    private final String TAG = "TAG: JOB";

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        final LocationTask LOCATION_TASK = new LocationTask(this);
        Thread locationThread = new Thread(() -> {
            if (LOCATION_TASK.isGPSEnabled()) {
                try {
                    LOCATION_TASK.getLocation();
                    Thread.sleep(1000);
                    LOCATION_TASK.addData();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                jobFinished(jobParameters, false);
            }
        });
        locationThread.start();
        Log.d(TAG, "JOB STARTED");
        return (true);
    }


    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "JOB STOPPED");
        return (true);
    }


}// CLASS ENDS