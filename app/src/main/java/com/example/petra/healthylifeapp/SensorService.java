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
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
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

import java.lang.reflect.Array;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class SensorService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public static final String TAG = "StepCounter";
    public int counter=0;
    public GoogleApiClient mApiClient;
    long oldTime=0;
    //firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private ArrayList<String> userLocations;
    private String PreviousLat = "";
    private String PreviousLon = "";
    private Timestamp timeStill = null;
    private Timestamp timeWalking = null;
    private Timer timer;
    private TimerTask timerTask;

    public SensorService(Context applicationContext) {
        super();
        Log.i("HERE", "here I am!");
    }

    public SensorService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if(FirebaseUtility.getUser() != null) {
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

            mDatabase = FirebaseDatabase.getInstance().getReference();
            if (mDatabase != null) {
                mDatabase.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        userLocations = FirebaseUtility.getUserLocations(dataSnapshot);

                        String userprop = FirebaseUtility.getUserProperty(dataSnapshot, "timeStill");

                        if (!userprop.equals("")) {
                            timeStill = Timestamp.valueOf(userprop);
                        }

                        userprop = FirebaseUtility.getUserProperty(dataSnapshot, "timeWalking");

                        if (!userprop.equals("")) {
                            timeWalking = Timestamp.valueOf(userprop);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w("Canceled", "loadPost:onCancelled", databaseError.toException());
                        // ...
                    }
                });
            }

            //get first location when service starts
            GetAndStoreCurrentLocation();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
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
                    CheckUserActivity();
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
                            FirebaseUtility.SaveUserLocation(location.getLatitude() + "|" + location.getLongitude(), userLocations);
                            PreviousLat = String.format("%.3f", location.getLatitude());
                            PreviousLon = String.format("%.3f", location.getLongitude());
                        }
                    }
                });

    }


    //checking the timestamp when user registerd to be Still
    private void CheckUserActivity() {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        //make sure that user don't get noifications of still-ness during the night
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (hour <= 22 && hour >= 9)              // Check if hour is not between 22 pm and 9am
        {
            //check how long it passed until user is still
            if (timeStill != null) {
                // get time difference in seconds
                long milliseconds = now.getTime() - timeStill.getTime();
                int seconds = (int) milliseconds / 1000;

                // calculate hours minutes and seconds
                //int hours = seconds / 3600;
                int minutes = (seconds % 3600) / 60;

                //that means that you're not getting notification twice
                if (minutes > 60 && minutes < 66) {
                    CreateNotification("You are sitting for too long! Please take a walk at least 5 minutes!");
                }
            }
        }

        //checking if the user is walking more than an hour
        if(timeWalking != null)
        {
            long milliseconds = now.getTime() - timeWalking.getTime();
            int seconds = (int) milliseconds / 1000;

            // calculate hours minutes and seconds
            //int hours = seconds / 3600;
            int minutes = (seconds % 3600) / 60;

            //that means that you're not getting notification twice
            if(minutes > 60 && minutes < 66)
            {
                CreateNotification("Well done! You're walking for a quite long time!");
            }
        }
    }

    private void CreateNotification(String message)
    {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
        builder.setContentText( message );
        builder.setSmallIcon( R.mipmap.ic_launcher );
        builder.setContentTitle( getString( R.string.app_name ) );
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        builder.setSound(alarmSound);
        long[] vibrate = { 0, 100 };
        builder.setVibrate(vibrate);
        NotificationManagerCompat.from(getApplicationContext()).notify(0, builder.build());
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
}