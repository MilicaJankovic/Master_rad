package com.example.petra.healthylifeapp;

/**
 * Created by Milica on 2/18/2018.
 */

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.snapshot.LocationResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class SensorService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public int counter=0;
    public GoogleApiClient mApiClient;

    //firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;


    private ArrayList<String> userLocations;

    private String PreviousLat = "";
    private String PreviousLon = "";


    public static final String TAG = "StepCounter";
    public SensorService(Context applicationContext) {
        super();
        Log.i("HERE", "here I am!");
    }

    public SensorService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mDatabase = FirebaseDatabase.getInstance().getReference();
        if(mDatabase != null) {
            mDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    getUserLocations(dataSnapshot);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w("Canceled", "loadPost:onCancelled", databaseError.toException());
                    // ...
                }
            });
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();


        if(currentUser != null) {
            //startTimer();
            mApiClient = new GoogleApiClient.Builder(SensorService.this)
                    .addApi(Fitness.SENSORS_API)
                    .addApi(Fitness.RECORDING_API)
                    .addApi(ActivityRecognition.API)
                    .addApi(Fitness.HISTORY_API)
                    .addApi(Awareness.API)
                    .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    // .enableAutoManage(this, 0, this)
                    .build();
            mApiClient.connect();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("EXIT", "ondestroy!");
        Intent broadcastIntent = new Intent("uk.ac.shef.oak.ActivityRecognition.RestartSensor");
        sendBroadcast(broadcastIntent);
        stoptimertask();
    }

    private Timer timer;
    private TimerTask timerTask;
    long oldTime=0;
    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, to wake up every 1 second
        //every 60 seconds
        timer.schedule(timerTask, TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5)); //
    }

    /**
     * it sets the timer to print the counter every x seconds
     */
    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                Log.i("in timer", "in timer ++++  "+ (counter++));
//                MainActivity activity = MainActivity.instance;
//                if (activity != null) {
                    // we are calling here activity's method
                    GetAndStoreCurrentLocation();
//                }

            }
        };
    }

    /**
     * not needed
     */
    public void stoptimertask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @SuppressLint("MissingPermission")
    public void GetAndStoreCurrentLocation()
    {
        Log.w("GetLocation", "Getting location started...");
        Awareness.SnapshotApi.getLocation(mApiClient)
                .setResultCallback(new ResultCallback<LocationResult>() {
                    @Override
                    public void onResult(@NonNull LocationResult locationResult) {
                        if (!locationResult.getStatus().isSuccess()) {
                            Log.w(TAG, "Could not get location.");
                            return;
                        }
                        Location location = locationResult.getLocation();
                        Log.w(TAG, "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
                        // SaveUserLocation(location.toString());
                        // UpdateLocation(location);

                        //save location in database only if it's different from previous
                        String lat = String.format("%.3f", location.getLatitude());
                        String lon = String.format("%.3f", location.getLongitude());

                        if(!lat.equals(PreviousLat) || !lon.equals(PreviousLon)) {
                            SaveUserLocation(location.getLatitude() + "|" + location.getLongitude());
                            PreviousLat = String.format("%.3f", location.getLatitude());
                            PreviousLon = String.format("%.3f", location.getLongitude());
                        }
                    }
                });

//        Awareness.SnapshotApi.getLocation(mApiClient)
//                .setResultCallback(new ResultCallback<LocationResult>() {
//                    @Override
//                    public void onResult(@NonNull LocationResult locationResult) {
//
//                        if (locationResult.getStatus().isSuccess()) {
//                            Location location = locationResult.getLocation();
//                            UpdateLocation(location);
//
//                        }
//                    }
//                });

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startTimer();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    private void getUserLocations(DataSnapshot dataSnapshot)
    {
        for(DataSnapshot ds: dataSnapshot.getChildren() ) {
            String UserID = getUser().getUid();
            User user = new User();

            if (ds.child(UserID).getValue(User.class) != null) {
                user.setLocations(ds.child(UserID).getValue(User.class).getLocations());
            }

            if(user.getLocations() != null)
            {
                userLocations = user.getLocations();
            }
        }
    }

    private void SaveUserLocation(String newLocation)
    {
        ArrayList<String> locations;
        if(userLocations != null) {
            locations = userLocations;
        }
        else{
            locations = new ArrayList<String>();
        }

        locations.add(newLocation);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser != null) {
            mDatabase = FirebaseDatabase.getInstance().getReference();
            if (mDatabase != null) {
                mDatabase.child("users").child(currentUser.getUid()).child("locations").setValue(locations);
                mDatabase.push();
            }
        }
    }

    private FirebaseUser getUser() {
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        return currentUser;
    }
}