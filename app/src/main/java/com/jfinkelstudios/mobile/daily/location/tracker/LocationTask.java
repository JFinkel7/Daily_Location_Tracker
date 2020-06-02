package com.jfinkelstudios.mobile.daily.location.tracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;


public final class LocationTask extends Thread {
    //***>
    private static final String TAG = "[LocationTask]";
    private static final String PARENT_KEY = "Location";
    private static final String DATE_KEY = "Date";
    private static final String TIME_KEY = "Time";
    private static final String LATITUDE_KEY = "Latitude";
    private static final String LONGITUDE_KEY = "Longitude";
    private HashMap<String, Object> dailyCoordinates;
    private static final int ELEVEN_PM = 23;
    private LocationManager locationManager;
    private Context context;
    private String currentHour;
    private String currentTime;
    private String currentDate;

    //***>

    public LocationTask(Context context) {
        /**** SETS LOCATION MANAGER ****/
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        /**** SETS CALENDER DATE ****/
        Calendar calendar = Calendar.getInstance();
        this.currentDate = DateFormat.getDateInstance().format(calendar.getTime());
        this.currentHour = new SimpleDateFormat("HH", Locale.US).format(new Date());
        this.currentTime = new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
        this.context = context;
    }


    public boolean isGPSEnabled() {
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
    }


    @SuppressLint("MissingPermission")
    public void getLocation() {
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(context);
        client.getLastLocation().addOnSuccessListener(location -> {
            // Here We Will Add Store The Coordinates Within The Cloud Database
            if (location != null) {
                dailyCoordinates = new HashMap<>();
                dailyCoordinates.put(TIME_KEY, currentTime);
                dailyCoordinates.put(LATITUDE_KEY, location.getLatitude());
                dailyCoordinates.put(LONGITUDE_KEY, location.getLongitude());
                // ** Debugger Log Information **
                //Log.d(TAG, "onSuccess: Latitude " + location.getLatitude());
                //Log.d(TAG, "onSuccess: Longitude " + location.getLongitude());
                location.reset();
            }
        });
    }


    /**** Add Data To The Database ****/
    public void addData() {
        Calendar calendar = Calendar.getInstance();
        final int CURRENT_HOUR = calendar.get(Calendar.HOUR_OF_DAY);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        // * Create A Parent Key *
        DatabaseReference reference = database.getReference(PARENT_KEY);
        reference.keepSynced(true);
        // * Check To See If Time Isn't 11PM (Close To Midnight)
        if (CURRENT_HOUR != ELEVEN_PM) {
            reference.child(String.valueOf(currentHour)).setValue(dailyCoordinates);

        } else {
            // Delete All Data If Time Reaches (11:00 PM)
            Objects.requireNonNull(reference.getParent()).removeValue();
        }
    }


    public static void readData(ILocationCallBack locationCallBack) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference reference = database.getReference(PARENT_KEY);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                /** Gets A List Of ALl The [Children] From The Parent **/
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    /** Gets A List Of ALl [CHILD KEYS] **/
                    String key = Objects.requireNonNull(child.getKey());
                    /** Gets A List Of ALl [CHILD VALUES] **/
                    String value = Objects.requireNonNull(child.getValue()).toString();
                    String latitude = Objects.requireNonNull(dataSnapshot.child(key).child(LATITUDE_KEY).getValue()).toString();
                    String longitude = Objects.requireNonNull(dataSnapshot.child(key).child(LONGITUDE_KEY).getValue()).toString();
                    String time = Objects.requireNonNull(dataSnapshot.child(key).child(TIME_KEY).getValue()).toString();
                    locationCallBack.onLatLng(latitude, longitude);
                    locationCallBack.onTime(time);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Add Something If Reading The Database Has Failed
            }
        });

    }


    /**** Start Ringtone ****/
    public void setRingtone() {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone ringtone = RingtoneManager.getRingtone(context.getApplicationContext(), notification);
        ringtone.play();
    }

}// CLASS ENDS
