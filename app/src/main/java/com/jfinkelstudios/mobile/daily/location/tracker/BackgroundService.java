package com.jfinkelstudios.mobile.daily.location.tracker;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;

// (1) Create A Service
public class BackgroundService extends Service {
    private final IBinder LOCATION_BINDER = new LocationBinder();


    /***** <==========================  INNER CLASS ==========================> *****/
    /***
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     ***/

    public final class LocationBinder extends Binder {
        //***>
        private int count = 0;
        private static final String TAG = "Binder";

        //***>
        public BackgroundService getServices() {
            return (BackgroundService.this);
        }


        /*** Creating A Method That Does Work Within A Binder ***/
        public void setProgress(final ProgressBar PROGRESS_BAR) {
            new Thread(() -> {
                for (int i = 0; i < 10; i++) {
                    try {
                        count += 10;
                        PROGRESS_BAR.setProgress(count);
                        Log.d(TAG, "run: " + count);
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

    }// END

    /***** <========================== END OF INNER CLASS ==========================> *****/

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Make The Service Sticky
        return (Service.START_STICKY);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

    }


    /***
     * This GETS Called from [bindService(intent, mConnection, Context.BIND_AUTO_CREATE)]
     * In X Activity
     ***/
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        /***
         * This Is where we are going
         * communicate MainActivity To The Service
         ***/
        return (LOCATION_BINDER);
    }


}// END OF CLASS
