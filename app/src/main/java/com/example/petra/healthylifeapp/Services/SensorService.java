package com.example.petra.healthylifeapp.Services;

/**
 * Created by Milica on 2/18/2018.
 */

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.example.petra.healthylifeapp.DBMaintenance.FeedReaderDbHelper;
import com.example.petra.healthylifeapp.DBMaintenance.FirebaseUtility;
import com.example.petra.healthylifeapp.Helpers.CaloriesCalculator;
import com.example.petra.healthylifeapp.Helpers.LocationPermissionUtility;
import com.example.petra.healthylifeapp.MainActivity;
import com.example.petra.healthylifeapp.DBMaintenance.NetworkAsyncTask;
import com.example.petra.healthylifeapp.R;
import com.example.petra.healthylifeapp.DBMaintenance.UserActivityProperties;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
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
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;
//import org.tensorflow.DataType;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlow;

public class SensorService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public static final String TAG = "StepCounter";
    private static final String SNOOZE_ACTION = "Snooze";
    private static final String ACCEPT_ACTION = "Accept";
    private static SensorService instance;
    public int counter = 0;
    public GoogleApiClient mApiClient;

    //region UserProperties for database
    public int TimeStill = 0;
    public int TimeWalking = 0;
    public Long userSleepTime = 0L;
    FeedReaderDbHelper mDbHelper;
    //firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private ArrayList<String> userLocations;
    private HashMap<String, Double> userCalories;
    private Double PreviousLat = 0.0;
    private Double PreviousLon = 0.0;
    private Timer timer;
    private TimerTask timerTask;
    private Timer timerActivity;
    private TimerTask timerTaskActivity;
    //endregion

    private Boolean locationReset;
    private int userAge = 0;
    //it needs to be double, because it adds 0.5 minutes all the time
    //in the end we will cast it to int
    private Double todayStill = 0.0;
    private Date userSleepTimeStarted = null;
    private SensorManager mSensorManager;
    private Sensor mLightSensor;
    private float mLightQuantity;
    private Boolean vehicleNotification;
    private int stepsGoal = 10000;
    private String notificationType = "";
    private String userGender = "";
//    private int userSleep = 0;
    private int targetWeight = 0;
    private Double cycling = 0.0;
    private Double driving = 0.0;
    private int weatherConditionGlobal = 0;


    private long stepsCount = 0;
    String userWeight;
    String userHeight;
    private Context appContext;
    //region nootification flags
    boolean flagAtHome = false;
    boolean flagDriving = false;
    boolean flagRunning = false;
    boolean flagCycling = false;
    boolean flagTraining = false;
    boolean flagStepsGoal = false;
    //endregion


    //region TensorFlow
//    static {
//        System.loadLibrary("tensorflow_inference");
//    }

    private static final String MODEL_FILE = "E:\\MASTER\\PythonPrograms\\frozen_har.pb";
    //private static final String INPUT_NODE = "I";
    //private static final String OUTPUT_NODE = "O";

    //private static final int[] INPUT_SIZE = {1,3};

    String INPUT_NODE = "input";
    String[] OUTPUT_NODES = {"y_"};
    String OUTPUT_NODE = "y_";
    //I don't know what is input size
    long[] INPUT_SIZE = {1, 16};
    int OUTPUT_SIZE = 2;

    private TensorFlowInferenceInterface inferenceInterface;

    //endregion

    public CaloriesCalculator calculator = new CaloriesCalculator(this, 1,1) ;

    public SensorService(Context applicationContext) {
        super();
        Log.i("HERE", "here I am!");
      //  appContext = this;

    }

    public SensorService(){

    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        mDbHelper = new FeedReaderDbHelper(getApplicationContext());
        appContext = this;

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

                        if(!FirebaseUtility.getUserProperty(dataSnapshot, "stepsGoal").equals("")) {
                            stepsGoal = Integer.parseInt(FirebaseUtility.getUserProperty(dataSnapshot, "stepsGoal"));
                        }
                        userGender = FirebaseUtility.getUserProperty(dataSnapshot, "gender");

                        if(!FirebaseUtility.getUserProperty(dataSnapshot, "sleep").equals("")) {
                            userSleepTime = Long.valueOf(Integer.parseInt(FirebaseUtility.getUserProperty(dataSnapshot, "sleep"))).longValue();
                        }

                        if(!FirebaseUtility.getUserProperty(dataSnapshot, "weight").equals("")) {
                            targetWeight = Integer.parseInt(FirebaseUtility.getUserProperty(dataSnapshot, "weight"));
                        }

                        userWeight = FirebaseUtility.getUserProperty(dataSnapshot, "weight");
                        userHeight = FirebaseUtility.getUserProperty(dataSnapshot, "height");

                        if(!userWeight.equals("") && !userHeight.equals(""))
                            calculator = new CaloriesCalculator(appContext, Double.valueOf(userWeight), Double.valueOf(userHeight));
                        else
                            calculator = new CaloriesCalculator(appContext, 1, 1);



                     //   ShowNumberOfSteps(MainActivity.textViewSteps);
//                        InitializeCheckBoxes(MainActivity.squats, MainActivity.jumping, MainActivity.climbers, MainActivity.burpees, MainActivity.pushups, MainActivity.situps);
//                        calculator.SetSharedPreferencesForExcersise("BLA", false, appContext);
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
            GetCurrentActivity();

            locationReset = false;
            vehicleNotification = false;
            //weatherConditionGlobal = MainActivity.returnWeatherConditon();
            weatherConditionGlobal = detectWeather();
        }

        //region TensorFlow

        inferenceInterface = new TensorFlowInferenceInterface(appContext.getAssets(), MODEL_FILE);
        //inferenceInterface.initializeTensorFlow(getAssets(), MODEL_FILE);

        //endregion


        SetLightSensor();

        //float[] bla = predictProbabilities();
    }


    public float[] predictProbabilities(float[] data) {
        float[] result = new float[OUTPUT_SIZE];
        inferenceInterface.feed(INPUT_NODE, data, INPUT_SIZE);
        inferenceInterface.run(OUTPUT_NODES);
        inferenceInterface.fetch(OUTPUT_NODE, result);

        //for us it should be 0 or 1
        //Downstairs	Jogging	  Sitting	Standing	Upstairs	Walking
        return result;
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
        if (hour >= 0 && hour <= 1) {
            SetSharedPreference(false);
        }

        //schedule the timer, to wake up every 60 seconds
        timer.schedule(timerTask, TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(1));

        MyBroadCastReciever receiver = new MyBroadCastReciever();
        IntentFilter screenStateFilter = new IntentFilter();
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
//        screenStateFilter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(receiver, screenStateFilter);
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


    public void setTimerTaskWeather()
    {
        Timer timerWeather = new Timer();
        TimerTask timerTaskWeather = new TimerTask() {
            @Override
            public void run() {
                weatherConditionGlobal = detectWeather();
            }
        };
        //initializeTimerTaskWeather();
        timerWeather.schedule(timerTaskWeather, TimeUnit.HOURS.toMillis(1), TimeUnit.HOURS.toMillis(1));
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

//                if (currentHour == 24 && !locationReset) {
//                    FirebaseUtility.ResetUserLocations();
//                    MainActivity.calculator.ResetSharedPreferences();
//                    locationReset = true;
//                    todayStill = 0.0;
//
//                    //reset today still
//
//                }
//                if (currentHour != 24 && locationReset) {
//                    locationReset = false;
//                }
                if (currentHour == 2 && currentMinute == 33) {
                    calculator.ResetSharedPreferences();
                     //MainActivity.calculator.CalculateCaloriesBurnedBySteps();
                    FirebaseUtility.ResetUserLocations();
                }

                Long stepsCount = null;
                SharedPreferences prefs = getSharedPreferences("StepsCount", MODE_PRIVATE);
                if (prefs != null) {
                    stepsCount = prefs.getLong("StepsCount", 0);
                }

                //send notification about steps
                if (FirebaseUtility.getPartOfTheDay().equals("Afternoon") && currentHour == 14 && !flagStepsGoal) {
                    if (stepsCount != null) {
                        if (stepsCount < stepsGoal) {
                            startNotification("Hey! You still didn't achieve your steps goal for today! Please do this!", "StepsGoal");
                            flagStepsGoal = true;
                        }
                    }
                }


                if (currentHour == 23 && (currentMinute > 55 && currentMinute < 59)) {

                    //region AddCalories for todays date to database
                    if (stepsCount != null && stepsCount > 0) {
                        //CaloriesCalculator calculator = new CaloriesCalculator(90, 184, Double.valueOf(stepsCount));
                        //Date yesterday = new Date(System.currentTimeMillis() - 1000L * 60L * 60L * 24L);
                        String date = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
                        FirebaseUtility.saveUserCaolries(date, calculator.CalculateCaloriesBurnedBySteps() + calculator.CalculateCaloriesBurnedByDoingExercises(), userCalories);
                    }
                    FirebaseUtility.ResetUserLocations();
                    calculator.ResetSharedPreferences();

                    locationReset = true;
                    todayStill = 0.0;
                    cycling = 0.0;
                    driving = 0.0;
                    flagTraining = false;
                    flagStepsGoal = false;
                    //endregion
                }

                locationReset = true;

                // we are calling here activity's method
                GetAndStoreCurrentLocation();
               // readData();
                if(calculator.checkExcersises() <= 0 && !flagTraining)
                {
                    startNotification("Hey! It's time for training, do some exercises?", "doExercises");
                    flagTraining = true;
                }

                if(FirebaseUtility.getPartOfTheDay().equals("Morning"))
                {
                    startNotification("Hey! It's morning, are you awake?", "getUp");
                }
                if(FirebaseUtility.getPartOfTheDay().equals("Night") && (currentHour > 22 ||  currentHour < 2))
                {
                    startNotification("Hey! It's evening, are you going to sleep?", "goToBad");
                }
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
                        Double lat = location.getLatitude();
                        Double lon = location.getLongitude();

                        if (!lat.equals(PreviousLat) || !lon.equals(PreviousLon)) {
                            FirebaseUtility.SaveUserLocation(location.getLatitude() + "|" + location.getLongitude(), userLocations);
                            PreviousLat = location.getLatitude();
                            PreviousLon = location.getLongitude();

                            flagAtHome = false;
                        }
                        else{
                            //to send it when register it

                            // AJDE DA IZBACIMO OVO :D
                            if(!flagAtHome) {
                                int weatherCondition = weatherConditionGlobal;
                                if (weatherCondition != -1) {
                                    switch (weatherCondition) {
                                        case 0:
                                            //startNotification("Are you at home?", "atHome");
                                            break;
                                        case Weather.CONDITION_CLOUDY:
                                            startNotification("Are you at home? It is cloudy, it may start raining!", "atHome");
                                            break;
                                        case Weather.CONDITION_CLEAR:
                                            startNotification("Are you at home? It is clear outside! Go to a walk!!", "takeAWalk");
                                            break;
                                        case Weather.CONDITION_RAINY:
                                            startNotification("Are you at home? It's rainy outside, you better be!", "atHome");
                                            break;
                                    }
                                }

                                flagAtHome = true;
                            }
                        }
                    }
                });

    }

    private int countHourOfTheDay() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        return cal.get(Calendar.HOUR_OF_DAY);
    }


    private void startNotification(String notificationMessage, String notType) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(ns);
        notificationType = notType;
        Notification notification = new Notification(R.mipmap.ic_launcher, null,
                System.currentTimeMillis());

        RemoteViews notificationView = new RemoteViews(getPackageName(), R.layout.notification);
        notificationView.setTextViewText(R.id.txtNotificationText, notificationMessage);
        //the intent that is started when the notification is clicked (works)
//        Intent notificationIntent = new Intent(this, SensorService.class);
//        PendingIntent pendingNotificationIntent = PendingIntent.getActivity(this, 0,
//                notificationIntent, 0);

        if (android.os.Build.VERSION.SDK_INT >= 16) {
            notification.bigContentView = notificationView;
        }

        notification.contentView = notificationView;

        //notification.contentIntent = pendingNotificationIntent;
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.flags |= Notification.FLAG_SHOW_LIGHTS;
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notification.sound = alarmSound;
        long[] vibrate = {0, 100};
        notification.vibrate = vibrate;
        notification.ledARGB = 0xff00cc99;
        notification.ledOnMS = 300;
        notification.ledOffMS = 1000;

        //this is the intent that is supposed to be called when the
        //button is clicked
        Intent snoozeIntent = new Intent(this, NotificationReceiver.class);
        snoozeIntent.setAction(SNOOZE_ACTION);
        PendingIntent pendingsnoozeIntent = PendingIntent.getBroadcast(this, 0,
                snoozeIntent, 0);
        notificationView.setOnClickPendingIntent(R.id.btnSnooze,
                pendingsnoozeIntent);

        Intent acceptIntent = new Intent(this, NotificationReceiver.class);
        acceptIntent.setAction(ACCEPT_ACTION);
        PendingIntent pendingacceptIntent = PendingIntent.getBroadcast(this, 0,
                acceptIntent, 0);
        notificationView.setOnClickPendingIntent(R.id.btnAccept,
                pendingacceptIntent);


        notificationManager.notify(1, notification);
        //notificationManager.cancelAll();
    }


    private void CreateNotification(String message) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
        builder.setContentText(message);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle(getString(R.string.app_name));
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        builder.setSound(alarmSound);
        builder.setLights(0xff00cc99, 300, 1000);
        long[] vibrate = {0, 100};
        builder.setVibrate(vibrate);

        NotificationManagerCompat.from(getApplicationContext()).notify(0, builder.build());
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startTimer();
        startTimerActiviy();
        setTimerTaskWeather();
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

                        //int weatherCondition = detectWeather();
                        int weatherCondition = weatherConditionGlobal;

                        switch (probableActivity.getType()) {
                            case DetectedActivity.IN_VEHICLE: {
                                Log.e("ActivityRecognition", "In Vehicle: " + probableActivity.getConfidence());
                                driving += 0.5;

                                if(!flagDriving) {
                                    startNotification("You are in vehicle, are you going to work?", "goToWork");

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
                                                default:
                                                    CreateNotification("You are in vehicle? Play some good music and drive carefully!");
                                                    break;
                                            }
                                        }
                                    }
                                }

                                flagDriving = true;
                                flagRunning = false;
                            }
                            case DetectedActivity.ON_BICYCLE: {
                                Log.e("ActivityRecognition", "On Bicycle: " + probableActivity.getConfidence());
                                if(probableActivity.getConfidence() > 75) {
                                    cycling += 0.5;
                                    if (!flagCycling) {
                                        startNotification("Are you going to cycling?.", "goToCycling");
                                        flagCycling = true;
                                    }
                                }
                                flagRunning = false;
                                break;
                            }
                            case DetectedActivity.RUNNING: {
                                Log.e("ActivityRecognition", "Running: " + probableActivity.getConfidence());
                                if (probableActivity.getConfidence() >= 75) {
                                    TimeStill = 0;
                                }

                                if(!flagRunning) {
                                    startNotification("Are you going to run?.", "goToRunning");
                                    flagRunning = true;
                                }

                                flagCycling = false;
                                break;
                            }
                            case DetectedActivity.STILL: {Log.e("ActivityRecognition", "Still: " + probableActivity.getConfidence());

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
                                        startNotification("You are sitting for too long! Please take a walk little bit.", "takeAWalk");
                                    }
                                    TimeStill = 0;
                                }

                                //after driving, user can be still or walking
                                flagDriving = false;
                                flagCycling = false;
                                flagRunning = false;
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

                                //after driving, user can be still or walking
                                flagDriving = false;
                                flagCycling = false;
                                flagRunning = false;
                                break;
                            }
                        }
                    }
                });
    }


    private String getWeatherCondition() {
        //int weatherCondition = detectWeather();
        int weatherCondition = weatherConditionGlobal;
        String wcond = "";

        if (weatherCondition != -1) {
            switch (weatherCondition) {
                case Weather.CONDITION_CLOUDY:
                    wcond = "Cloudy";
                break;
                case Weather.CONDITION_CLEAR:
                    wcond = "Clear";
                break;
                case Weather.CONDITION_FOGGY:
                    wcond = "Foggy";
                break;
                case Weather.CONDITION_RAINY:
                    wcond = "Rainy";
                break;
                case Weather.CONDITION_SNOWY:
                    wcond = "Snowy";
                break;
                case Weather.CONDITION_ICY:
                    wcond = "Icy";
                break;
                default:
                    wcond = "Cloudy";
                break;
            }
        }

        return wcond;
    }

    private int detectWeather() {
        LocationPermissionUtility ac = new LocationPermissionUtility(this);
        if (!ac.checkLocationPermission()) {
            return -1;
        }

        int weatherCondition = 0;
        Awareness.SnapshotApi.getWeather(mApiClient)
                .setResultCallback(new ResultCallback<WeatherResult>() {
                    @Override
                    public void onResult(@NonNull WeatherResult weatherResult) {
                        Weather weather = weatherResult.getWeather();

                        if (weather != null) {
                            weatherConditionGlobal = weather.getConditions()[0];
                        }
                    }
                });


        weatherCondition = weatherConditionGlobal;
        return weatherCondition;
    }

    private void SetSharedPreferenceLong(String key, Long value)
    {
        SharedPreferences.Editor editor = getSharedPreferences(key, MODE_PRIVATE).edit();
        editor.putLong(key, value);
        editor.apply();
    }
    private void saveNotificationDataToSQLLite(int userInput) {
        UserActivityProperties uap = new UserActivityProperties();
        uap.setAge(userAge);

        //calories, blah
        Double myDouble = calculator.CalculateCaloriesBurnedBySteps();
        Integer val = Integer.valueOf(myDouble.intValue());
        uap.setCalories(Integer.valueOf(val.intValue()));

        val = Integer.valueOf(todayStill.intValue());
        uap.setContinuousStill(Integer.valueOf(val.intValue()));

        uap.setCycling(Integer.valueOf(cycling.intValue()));
        uap.setDriving(Integer.valueOf(driving.intValue()));

        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);

        uap.setDayOfTheWeek(dayOfWeek);
        uap.setStill(TimeStill);
        uap.setPartOfTheDay(FirebaseUtility.getPartOfTheDay());
        uap.setGender(userGender);
        uap.setSleeping(Integer.parseInt(userSleepTime.toString()));
        uap.setTargetWeight(targetWeight);
        uap.setNotificationType(notificationType);
        uap.setUserInput(userInput);

        uap.setWeather(getWeatherCondition());

        Long stepsCount = null;
        SharedPreferences prefs = getSharedPreferences("StepsCount", MODE_PRIVATE);
        if (prefs != null) {
            stepsCount = prefs.getLong("StepsCount", 0);
        }
        uap.setStepsNum(stepsCount.intValue());

        mDbHelper.InsertToDatabase(uap);

        List<UserActivityProperties> props = new ArrayList<>();
        props.add(uap);

        NetworkAsyncTask nat = new NetworkAsyncTask(mDbHelper);
        //nat.doInBackground();
        nat.execute();
        //HTTPHelper.setProperties(props);
    }


    private List<UserActivityProperties> ReadDataFromSQLLite() {
        List<UserActivityProperties> activities = mDbHelper.GetObjectsFromCursor(mDbHelper.ReadDataFromDatabase());

        for (int i = 0; i < activities.size(); i++) {
            Log.i("Activitie " + i + ": ", String.valueOf(activities.get(i)));
        }

        return activities;
    }



    private void SetSharedPreferenceString(String name, String key, String value)
    {
        SharedPreferences.Editor editor = getSharedPreferences(name, MODE_PRIVATE).edit();
        editor.putString(key, value);
        editor.apply();
    }

    public long getStepsCount() {
        return stepsCount;
    }

    public void setStepsCount(long stepsCount) {
        this.stepsCount = stepsCount;
    }



    public static class NotificationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();
            if (SNOOZE_ACTION.equals(action)) {
//                Toast.makeText(context, "SNOOZE CALLED", Toast.LENGTH_SHORT).show();
                instance.saveNotificationDataToSQLLite(0);
                instance.ReadDataFromSQLLite();
            } else if (ACCEPT_ACTION.equals(action)) {
//                Toast.makeText(context, "ACCEPT CALLED", Toast.LENGTH_SHORT).show();
                instance.saveNotificationDataToSQLLite(1);
                instance.ReadDataFromSQLLite();
            }

            String ns = Context.NOTIFICATION_SERVICE;
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);
            mNotificationManager.cancel(1);
        }


    }

    public class MyBroadCastReciever extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                Log.i("Check", "Screen went OFF");
                if (FirebaseUtility.getPartOfTheDay().equals("Night")) {
                    if (mLightQuantity < 5.0) {
                        userSleepTimeStarted = new Date();
                        Log.i("UserSleepTimeStarted: ", String.valueOf(userSleepTimeStarted));
                        SetSharedPreferenceString("UserSleepTimeStarted", "SleepStarted", String.valueOf(userSleepTimeStarted));
                    }
                }
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                Log.i("Check", "Screen went ON");

                SharedPreferences prefs = getSharedPreferences("UserSleepTimeStarted", MODE_PRIVATE);
                String sleepStarted = prefs.getString("SleepStarted", "");


                // && FirebaseUtility.getPartOfTheDay().equals("Morning")
                if ((userSleepTimeStarted != null || !sleepStarted.equals("")) && FirebaseUtility.getPartOfTheDay().equals("Morning")) {
                    Date userSleepTimeEnded = new Date();
                    final int MILLI_TO_HOUR = 1000 * 60 * 60;

                    if(userSleepTimeEnded != null)
                    {
                        userSleepTime = (userSleepTimeEnded.getTime() - userSleepTimeStarted.getTime()) / MILLI_TO_HOUR;
                        userSleepTimeStarted = null;
                    }
                    else{
                        SimpleDateFormat formatter =new SimpleDateFormat("dd-MM-yyyy");
                        try {
                            userSleepTime = (userSleepTimeEnded.getTime() - formatter.parse(sleepStarted).getDate()) / MILLI_TO_HOUR;
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        SetSharedPreferenceString("UserSleepTimeStarted", "SleepStarted", null);
                    }


                    //region test, worked
//                    Calendar calendar = Calendar.getInstance();
//                    calendar.add(Calendar.DAY_OF_YEAR, 1);
//                    Date tomorrow = calendar.getTime();
//                    userSleepTime = (tomorrow.getTime() - userSleepTimeStarted.getTime()) / MILLI_TO_HOUR;
                    //endregion

                    FirebaseUtility.saveUserProperty(String.valueOf(userSleepTime), "sleep");

                }
                Log.i("Light: ", String.valueOf(mLightQuantity));
            }
//            else if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
//                Log.i("Check", "USER PRESENT");
//            }
        }
    }
}