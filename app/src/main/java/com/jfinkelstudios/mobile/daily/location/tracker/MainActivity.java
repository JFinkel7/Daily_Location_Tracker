/*
 * Project: << Daily Location Tracker >>
 * Software Developer: Denis J Finkel
 * Start Date: May 5th, 2020
 * Description: Track Clients GPS Location Daily In The Background
 * Tools: Realtime Firebase, FusedLocation Provider & Google Maps API
 * App Uses: Multi-Threading, Background Service , JobScheduler
 * INFO: Client Data Is Only Sent When Phone Is Charging (Security Purposes & Realtime Database Compatibility)
 * NOTE: It Can Sync The Data For About An Hour
 */

package com.jfinkelstudios.mobile.daily.location.tracker;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private BackgroundService dailyServerService;
    private ProgressBar progressBar;
    private Intent serviceIntent;
    private static final int FIFTEEN_MINUTES = (15 * 60 * 1000);
    private static final int JOB_ID = 1298745;
    private static boolean isLocationPermissionGranted;
    private boolean isBounded;
    private JobScheduler jobScheduler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /***** FIND VIEW BY ID's ****/
        this.progressBar = findViewById(R.id.progressBar);
        Toolbar toolbar = findViewById(R.id.toolbar);
        this.setSupportActionBar(toolbar);

        /***** SETS UP Google Map Fragment ****/
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        /***** CHECKS IF User Has Location Permission ****/
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            isLocationPermissionGranted = true;
        } else {
            /***** ASK User For Location Permission ****/
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // Prompts Dialog Location Permission
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                recreate();
            }, 3000);
        }

        /***** SETS UP THE JOB-SCHEDULER ****/
        ComponentName componentName = new ComponentName(MainActivity.this, LocationJob.class);
        JobInfo jobInfo = new JobInfo.Builder(JOB_ID, componentName)
                .setPersisted(true)
                .setPeriodic(FIFTEEN_MINUTES)
                .build();
        this.jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) jobScheduler.schedule(jobInfo);

    }// END OF ON-CREATE


    @Override
    public void onMapReady(GoogleMap googleMap) {
        new Handler(Looper.getMainLooper()).post(() -> {
            LocationTask.readData(new ILocationCallBack() {
                private LatLng current_location;

                @Override
                public void onLatLng(String lat, String lng) {
                    current_location = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
                }

                @Override
                public void onTime(String time) {
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(current_location);
                    // Adds Tittle
                    markerOptions.title(time);
                    // Adds Subtitle
                    markerOptions.snippet("Location");
                    // Adds - Color Icon
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                    googleMap.addMarker(markerOptions);
                    googleMap.animateCamera(CameraUpdateFactory.newLatLng(current_location));
                }
            });
        });
    }


    /**** [ON-APP-START] ASK User For Location Permission ****/
    @Override
    protected void onStart() {
        super.onStart();
        if (isLocationPermissionGranted) {
            serviceIntent = new Intent(this, BackgroundService.class);
            startService(serviceIntent);
            bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }


    /*** [BUTTON] STOP THE SERVICE & JobScheduler ***/
    public void btn_StopTracking(View view) {
        if (isBounded) {
            stopService(serviceIntent);
            this.jobScheduler.cancel(JOB_ID);
        }
    }

    /**** [ON-APP-DESTROY] Unbinds THE SERVICE ****/
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBounded) {
            unbindService(mConnection);
        }
    }

    /**** Defines callbacks for service binding, passed to bindService() ****/
    private ServiceConnection mConnection = new ServiceConnection() {
        // * When Service Is Connected
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BackgroundService.LocationBinder binder = (BackgroundService.LocationBinder) service;
            dailyServerService = binder.getServices();
            // Call The New Method
            binder.setProgress(progressBar);
            isBounded = true;
        }

        // * When Service Is Disconnected
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBounded = false;
        }
    };


}// CLASS ENDS