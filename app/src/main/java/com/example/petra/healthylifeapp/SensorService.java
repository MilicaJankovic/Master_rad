package com.example.petra.healthylifeapp;

/**
 * Created by Milica on 2/18/2018.
 */

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.snapshot.DetectedActivityResult;
import com.google.android.gms.awareness.snapshot.LocationResult;
import com.google.android.gms.awareness.snapshot.WeatherResult;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
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
import java.text.SimpleDateFormat;
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
    public int counter = 0;
    public GoogleApiClient mApiClient;
    //firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private ArrayList<String> userLocations;
    private HashMap<String, Double> userCalories;
    private String PreviousLat = "";
    private String PreviousLon = "";
    private Timer timer;
    private TimerTask timerTask;
    private Timer timerActivity;
    private TimerTask timerTaskActivity;
    private Boolean locationReset;

    //region UserProperties for database
    public int TimeStill = 0;
    public int TimeWalking = 0;
    public long userSleepTime = 0;
    private int userAge = 0;
    //it needs to be double, because it adds 0.5 minutes all the time
    //in the end we will cast it to int
    private double todayStill = 0;
    private Date userSleepTimeStarted = null;
    private Date userSleepTimeEnded = null;
    //endregion

    private static final String SNOOZE_ACTION = "Snooze";
    private static final String ACCEPT_ACTION = "Accept";

    private SensorManager mSensorManager;
    private Sensor mLightSensor;
    private float mLightQuantity;

    private Boolean vehicleNotification;

    public SensorService(Context applicationContext) {
        super();
        Log.i("HERE", "here I am!");
    }

    public SensorService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (FirebaseUtility.getUser() != null) {
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
            mDatabase.keepSynced(true);
            if (mDatabase != null) {
                mDatabase.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        userLocations = FirebaseUtility.getUserLocations(dataSnapshot);
                        userCalories = FirebaseUtility.getUserCalories(dataSnapshot);
                        userAge = FirebaseUtility.CalculateAge(dataSnapshot);
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
            //CheckUserActivity();
            GetCurrentActivity();

            MyBroadCastReciever receiver = new MyBroadCastReciever();
            IntentFilter screenStateFilter = new IntentFilter();
            screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
            screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
            registerReceiver(receiver, screenStateFilter);

            locationReset = false;
            vehicleNotification = false;
        }

        SetLightSensor();
    }


    protected void SetLightSensor() {
        // Obtain references to the SensorManager and the Light Sensor
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        // Implement a listener to receive updates
        SensorEventListener listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                mLightQuantity = event.values[0];
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        // Register the listener with the light sensor -- choosing
        // one of the SensorManager.SENSOR_DELAY_* constants.
        mSensorManager.registerListener(
                listener, mLightSensor, SensorManager.SENSOR_DELAY_UI);
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
        stoptimertasks();
    }

    public void startTimer() {
        //set a new Timer
        timer = new Timer();
        //initialize the TimerTask's job
        initializeTimerTask();

        int hour = countHourOfTheDay();
        //if time is between 00:00 and 01:00 set shared pref to false so notification will be fired tomorow again
        if (hour >= 24 && hour <= 1) {
            SetSharedPreference(false);
        }

        //schedule the timer, to wake up every 60 seconds
        timer.schedule(timerTask, TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(1)); //
    }

    private void SetSharedPreference(Boolean value) {
        SharedPreferences.Editor editor = getSharedPreferences("StepsGoalNotification", MODE_PRIVATE).edit();
        editor.putBoolean("FiredToday", value);
        editor.apply();
    }

    public void startTimerActiviy() {
        timerActivity = new Timer();
        initializeTimerTaskActivity();
        timer.schedule(timerTaskActivity, 30000, 30000); //
    }

    /**
     * it sets the timer to print the counter every x seconds
     */
    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                Log.i("in timer", "in timer ++++  " + (counter++));
                int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY); //Current hour
                int currentMinute = Calendar.getInstance().get(Calendar.MINUTE); //Current minute

                if (currentHour == 24 && !locationReset) {
                    FirebaseUtility.ResetUserLocations();
                    MainActivity.calculator.ResetSharedPreferences();
                    locationReset = true;

                    //reset today still
                    todayStill = 0;
                }
                if (currentHour != 24 && locationReset) {
                    locationReset = false;
                }


                if (currentHour == 23 && currentMinute == 55) {

                    //region AddCalories for todays date to database

                    SharedPreferences prefs = getSharedPreferences("StepsCount", MODE_PRIVATE);
                    if (prefs != null) {
                        Long stepsCount = prefs.getLong("StepsCount", 0);

                        if (stepsCount != null && stepsCount > 0) {
                            //CaloriesCalculator calculator = new CaloriesCalculator(90, 184, Double.valueOf(stepsCount));
                            //Date yesterday = new Date(System.currentTimeMillis() - 1000L * 60L * 60L * 24L);
                            String date = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
                            FirebaseUtility.saveUserCaolries(date, MainActivity.calculator.CalculateCaloriesBurnedBySteps(), userCalories);
                        }
                    }
                    //endregion
                }

                locationReset = true;

                // we are calling here activity's method
                GetAndStoreCurrentLocation();
            }
        };
    }

    public void initializeTimerTaskActivity() {
        timerTaskActivity = new TimerTask() {
            public void run() {
                Log.i("in timer", "in timer ++++  " + (counter++));
                GetCurrentActivity();
            }
        };
    }

    /**
     * not needed
     */
    public void stoptimertasks() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        //stop the timer, if it's not already null
        if (timerActivity != null) {
            timerActivity.cancel();
            timerActivity = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



    @SuppressLint("MissingPermission")
    public void GetAndStoreCurrentLocation() {
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

                        if (!lat.equals(PreviousLat) || !lon.equals(PreviousLon)) {
                            FirebaseUtility.SaveUserLocation(location.getLatitude() + "|" + location.getLongitude(), userLocations);
                            PreviousLat = String.format("%.3f", location.getLatitude());
                            PreviousLon = String.format("%.3f", location.getLongitude());
                        }
                        CreateNotification("location set");
                    }
                });

    }

    private int countHourOfTheDay() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        return cal.get(Calendar.HOUR_OF_DAY);
    }

    private void CreateNotification(String message) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
        builder.setContentText(message);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle(getString(R.string.app_name));
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        builder.setSound(alarmSound);
        long[] vibrate = {0, 100};
        builder.setVibrate(vibrate);

        Intent notificationIntentSnooze = new Intent(this, SensorService.class);
        notificationIntentSnooze.setAction(SNOOZE_ACTION);
        PendingIntent pendingIntentSnooze = PendingIntent.getBroadcast(this, 0, notificationIntentSnooze, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(R.mipmap.snooze, getString(R.string.snooze), pendingIntentSnooze);
        NotificationReceiver receiver = new NotificationReceiver();
        receiver.onReceive(this, notificationIntentSnooze);

        Intent notificationIntentAction = new Intent(this, SensorService.class);
        notificationIntentAction.setAction(ACCEPT_ACTION);
        PendingIntent pendingIntentAction = PendingIntent.getBroadcast(this, 0, notificationIntentAction, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(R.mipmap.accept, getString(R.string.snooze), pendingIntentAction);
        NotificationReceiver receiver1 = new NotificationReceiver();
        receiver1.onReceive(this, notificationIntentAction);

        NotificationManagerCompat.from(getApplicationContext()).notify(0, builder.build());
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startTimer();
        startTimerActiviy();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @SuppressLint("MissingPermission")
    public void GetCurrentActivity() {
        Log.w("GetActivity", "Getting activity started...");
        Awareness.SnapshotApi.getDetectedActivity(mApiClient)
                .setResultCallback(new ResultCallback<DetectedActivityResult>() {
                    @Override
                    public void onResult(@NonNull DetectedActivityResult detectedActivityResult) {
                        if (!detectedActivityResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Could not get the current activity.");
                            return;
                        }
                        ActivityRecognitionResult ar = detectedActivityResult.getActivityRecognitionResult();
                        DetectedActivity probableActivity = ar.getMostProbableActivity();
                        Log.i(TAG, probableActivity.toString());

                        int weatherCondition = detectWeather();

                        switch (probableActivity.getType()) {
                            case DetectedActivity.IN_VEHICLE: {
                                Log.e("ActivityRecognition", "In Vehicle: " + probableActivity.getConfidence());

                                if (vehicleNotification == false) {
                                    vehicleNotification = true;
                                    if (weatherCondition != -1) {
                                        switch (weatherCondition) {
                                            case Weather.CONDITION_CLOUDY:
                                                CreateNotification("It's cloudy! It's good you are in vehicle!");
                                                break;
                                            case Weather.CONDITION_CLEAR:
                                                CreateNotification("It's clear outside! You should be walking instead!");
                                                break;
                                            case Weather.CONDITION_FOGGY:
                                                CreateNotification("It's foggy outside! Drive carefully!");
                                                break;
                                            case Weather.CONDITION_RAINY:
                                                CreateNotification("It's rainy! It's good you are in vehicle!");
                                                break;
                                            case Weather.CONDITION_SNOWY:
                                                CreateNotification("It's snowy outside! Drive carefully!");
                                                break;
                                            case Weather.CONDITION_ICY:
                                                CreateNotification("It's icy outside! Drive slow!");
                                                break;
                                        }
                                    }
                                }
//                                else {
//                                    CreateNotification("You are in vehicle? Play some good music and drive carefully!");
//                                }
                                break;
                            }
                            case DetectedActivity.ON_BICYCLE: {
                                Log.e("ActivityRecognition", "On Bicycle: " + probableActivity.getConfidence());
                                break;
                            }
                            case DetectedActivity.ON_FOOT: {
                                vehicleNotification = false;
                                Log.e("ActivityRecognition", "On Foot: " + probableActivity.getConfidence());
                                if (probableActivity.getConfidence() >= 75) {
                                    TimeStill = 0;
                                }
                                break;
                            }
                            case DetectedActivity.RUNNING: {
                                Log.e("ActivityRecognition", "Running: " + probableActivity.getConfidence());
                                if (probableActivity.getConfidence() >= 75) {
                                    TimeStill = 0;
                                }
                                break;
                            }
                            case DetectedActivity.STILL: {
                                Log.e("ActivityRecognition", "Still: " + probableActivity.getConfidence());

                                vehicleNotification = false;

                                if (probableActivity.getConfidence() >= 75) {
                                    //today still is reseting in the midnite, it counts minutes
                                    todayStill += 0.5;
                                    TimeStill++;
                                    TimeWalking = 0;
                                    Log.e("TimeStill", "Time: " + TimeStill);
                                }

                                //this is one hour(120 (because it is taking activity on 30 secons))
                                if (TimeStill >= 120) {

                                    int hourOfTheDay = countHourOfTheDay();
                                    //send notification if it's not in the middle of the night when sleeping
                                    if (hourOfTheDay <= 22 && hourOfTheDay >= 9) {
                                        CreateNotification("You are sitting for too long! Please take a walk little bit.");
                                    }
                                    TimeStill = 0;
                                }
                                break;
                            }
                            case DetectedActivity.TILTING: {
                                Log.e("ActivityRecognition", "Tilting: " + probableActivity.getConfidence());
                                break;
                            }
                            case DetectedActivity.WALKING: {
                                vehicleNotification = false;

                                Log.e("ActivityRecognition", "Walking: " + probableActivity.getConfidence());
                                if (probableActivity.getConfidence() >= 75) {
                                    TimeWalking++;
                                    TimeStill = 0;
                                }

                                if (TimeWalking >= 120) {
                                    CreateNotification("You are walking for so long! Well done!");
                                    TimeWalking = 0;
                                }
                                break;
                            }
                            case DetectedActivity.UNKNOWN: {
                                Log.e("ActivityRecognition", "Unknown: " + probableActivity.getConfidence());
                                break;
                            }
                        }
                    }
                });
    }

    private int detectWeather() {
        LocationPermissionUtility ac = new LocationPermissionUtility(this);
        if (!ac.checkLocationPermission()) {
            return -1;
        }

        final int[] weatherCondition = {0};
        Awareness.SnapshotApi.getWeather(mApiClient)
                .setResultCallback(new ResultCallback<WeatherResult>() {
                    @Override
                    public void onResult(@NonNull WeatherResult weatherResult) {
                        Weather weather = weatherResult.getWeather();

                        if (weather != null) {

                            weatherCondition[0] = weather.getConditions()[0];

                        }
                    }
                });

        return weatherCondition[0];
    }

    public class MyBroadCastReciever extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                Log.i("Check", "Screen went OFF");
                if (FirebaseUtility.getPartOfTheDay().equals("Night")) {
                    if (mLightQuantity < 30.0) {
                        userSleepTimeStarted = new Date();
                    }
                }
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                Log.i("Check", "Screen went ON");
//                String userage = getString(userAge);
//                Log.i("Age", userage);
//                String part = FirebaseUtility.getPartOfTheDay();
//                Log.i("Time of the day", part);

                // && FirebaseUtility.getPartOfTheDay().equals("Morning")
                if (userSleepTimeStarted != null && FirebaseUtility.getPartOfTheDay().equals("Morning")) {
                    userSleepTimeEnded = new Date();
                    final int MILLI_TO_HOUR = 1000 * 60 * 60;
                    userSleepTime = (userSleepTimeEnded.getTime() - userSleepTimeStarted.getTime()) / MILLI_TO_HOUR;
                    FirebaseUtility.saveUserProperty(String.valueOf(userSleepTime), "sleep");

                    userSleepTimeEnded = null;
                    userSleepTimeStarted = null;
                }
                Log.i("Light: ", String.valueOf(mLightQuantity));
            }
        }
    }


    public class NotificationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();
            if (SNOOZE_ACTION.equals(action)) {
                Toast.makeText(context, "SNOOZE CALLED", Toast.LENGTH_SHORT).show();
            } else if (ACCEPT_ACTION.equals(action)) {
                Toast.makeText(context, "ACCEPT CALLED", Toast.LENGTH_SHORT).show();
            }
        }
    }
}