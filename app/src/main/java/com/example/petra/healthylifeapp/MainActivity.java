package com.example.petra.healthylifeapp;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.example.petra.healthylifeapp.DBMaintenance.FeedReaderDbHelper;
import com.example.petra.healthylifeapp.DBMaintenance.FirebaseLogin;
import com.example.petra.healthylifeapp.DBMaintenance.FirebaseUtility;
import com.example.petra.healthylifeapp.DBMaintenance.HTTPHelper;
import com.example.petra.healthylifeapp.DBMaintenance.UserActivityProperties;
import com.example.petra.healthylifeapp.Helpers.CaloriesCalculator;
import com.example.petra.healthylifeapp.Services.BackgroundService;
import com.example.petra.healthylifeapp.Services.SensorService;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.snapshot.WeatherResult;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DataSourcesResult;
import com.google.android.gms.fitness.result.ListSubscriptionsResult;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements OnDataPointListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    public static final String TAG = "StepCounter";
    public static final String FENCE_RECEIVER_ACTION =
            "com.hitherejoe.aware.ui.fence.FenceReceiver.FENCE_RECEIVER_ACTION";
    private static final int REQUEST_OAUTH = 1;
    private static final String AUTH_PENDING = "auth_state_pending";
    private static final int REQUEST_OAUTH_REQUEST_CODE = 0x1001;
    private final static int REQUEST_PERMISSION_RESULT_CODE = 42;

    public static final String ExerciseTag = "Exercise";
    public static MainActivity instance;
    public GoogleApiClient mApiClient;
    LocationRequest mLocationRequest;

//    private static String PreviousLocation = "";
    LocationServices mLastLocation;
    Intent mServiceIntent;
    Context ctx;
    private boolean authInProgress = false;
    private ResultCallback<Status> mSubscribeResultCallback;
    private ResultCallback<Status> mCancelSubscriptionResultCallback;
    private ResultCallback<ListSubscriptionsResult> mListSubscriptionsResultCallback;
    //firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private ArrayList<String> userLocations;
    private SensorService mSensorService;

    private int notificationCounter = 0;

    private int StepsGoal = 10000;

    public  Context getCtx() {
        return this.getApplicationContext();
    }


    public static CheckBox squats;
    public static CheckBox jumping;
    public static CheckBox climbers;
    public static  CheckBox burpees;
    public static CheckBox pushups;
    public static CheckBox situps;
    public static TextView textViewSteps;

    String userWeight;
    String userHeight;
    private boolean numberOfExercisesSet = false;

    public static CaloriesCalculator calculator;

    private static int weathercondition = 0;
    FeedReaderDbHelper mDbHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorService = new SensorService(this);

        mApiClient = new GoogleApiClient.Builder(MainActivity.this)
                .addApi(Fitness.SENSORS_API)
                .addApi(Fitness.RECORDING_API)
                .addApi(ActivityRecognition.API)
                .addApi(Fitness.HISTORY_API)
                .addApi(Awareness.API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .enableAutoManage(this, 0, this)
                .build();
        mApiClient.connect();

        //send all data from sqllite, and clear sqllite


        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }


        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
     //   mDbHelper = new FeedReaderDbHelper(getApplicationContext());
     //   mDbHelper.DeleteTable();
   //     mDbHelper.CreateTable();
      //  InitializeDB();
//        BackgroundService service = new BackgroundService();
//        BackgroundService.FenceReceiver receiver = service.new FenceReceiver();
//        BackgroundService.FenceReceiver receiver = new BackgroundService.FenceReceiver();
        //registerReceiver(receiver, new IntentFilter(FENCE_RECEIVER_ACTION));
        // unregisterReceiver(receiver);

        if (currentUser != null) {



        /**********************STEPS**************************/
        FitnessOptions fitnessOptions =
                FitnessOptions.builder()
                        .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                        .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
                        .build();
        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this,
                    REQUEST_OAUTH_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(this),
                    fitnessOptions);
        } else {
            subscribe();
        }
        /******call read steps every  2sec(optional), can be implemented to call show passed steps only once on create *********/
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                readData();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        t.start();
//        /************************END STEPS*********************************/
//

        }

        //get user locations from firebase
        if(FirebaseUtility.getUser() == null)
        {
            Intent intent = new Intent(this, FirebaseLogin.class);
            startActivity(intent);
        }
        else
        {
            mDatabase = FirebaseDatabase.getInstance().getReference();
            if(mDatabase != null) {
                mDatabase.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //getUserLocations(dataSnapshot);

                        String steps = FirebaseUtility.getUserProperty(dataSnapshot, "stepsGoal");
                        userWeight = FirebaseUtility.getUserProperty(dataSnapshot, "weight");
                        userHeight = FirebaseUtility.getUserProperty(dataSnapshot, "height");


                        if(!steps.equals(""))
                        {
                            StepsGoal = Integer.parseInt(steps);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w("Canceled", "loadPost:onCancelled", databaseError.toException());
                        // ...
                    }
                });
            }
        }

        Button buttonHistory = (Button) findViewById(R.id.button_viewHistory);
        buttonHistory.setOnClickListener(this);

        /****************OPEN CALENDAR CLICK********************************/
        Button buttonCalendar = (Button) findViewById(R.id.button_showCalendar);
        buttonCalendar.setOnClickListener(this);


        textViewSteps = (TextView) findViewById(R.id.title_text_view);



        SetSharedPreference(false);



//        if(!numberOfExercisesSet) {
//            SetNumberOfExercises();
//            numberOfExercisesSet = true;
//        }

    }

    private void SetSharedPreference(Boolean value)
    {
        SharedPreferences.Editor editor = getSharedPreferences("StepsGoalNotification", MODE_PRIVATE).edit();
        editor.putBoolean("FiredToday", value);
        editor.apply();
    }

    private void SetSharedPreferenceLong(String key, Long value)
    {
        SharedPreferences.Editor editor = getSharedPreferences(key, MODE_PRIVATE).edit();
        editor.putLong(key, value);
        editor.apply();
    }

    private Location getLocationDetails(Context mContext) {
        Location location = null;
        if (mApiClient != null) {
            if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG,"Location Permission Denied");
                return null;
            }else {
                location = LocationServices.FusedLocationApi.getLastLocation(mApiClient);
                if(location != null)
                    Log.w(TAG, "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
                else
                    Log.w(TAG,"Location is null");

            }
        }
        return location;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mApiClient.connect();

        Intent intent = new Intent(this, BackgroundService.class);
        startService(intent);

//        Intent intentActivity = new Intent(this, ActivityRecognizedService.class);
//        startService(intentActivity);

//        SaveUserLocation("45.32;12.23");
//        SaveUserLocation("45:33;12:34");



    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
//        Intent intent = new Intent(this, ActivityRecognizedService.class);
//        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, 3000, pendingIntent);

        DataSourcesRequest dataSourceRequest = new DataSourcesRequest.Builder()
                .setDataTypes(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .setDataSourceTypes(DataSource.TYPE_RAW)
                .build();

        ResultCallback<DataSourcesResult> dataSourcesResultCallback = new ResultCallback<DataSourcesResult>() {
            @Override
            public void onResult(DataSourcesResult dataSourcesResult) {
                for (DataSource dataSource : dataSourcesResult.getDataSources()) {
                    if (DataType.TYPE_STEP_COUNT_CUMULATIVE.equals(dataSource.getDataType())) {
                        registerFitnessDataListener(dataSource, DataType.TYPE_STEP_COUNT_CUMULATIVE);
                    }
                }
            }
        };

        Fitness.SensorsApi.findDataSources(mApiClient, dataSourceRequest)
                .setResultCallback(dataSourcesResultCallback);

        Fitness.RecordingApi.subscribe(mApiClient, DataType.TYPE_STEP_COUNT_DELTA)
                .setResultCallback(mSubscribeResultCallback);

        if (mApiClient.isConnected()) {
            Log.i("mApiClient", "Google_Api_Client: It was connected on (onConnected) function, working as it should.");
        } else {
            Log.i("mApiClient", "Google_Api_Client: It was NOT connected on (onConnected) function, It is definetly bugged.");
        }

        detectWeather();


        mServiceIntent = new Intent(getApplicationContext(), mSensorService.getClass());
        if (!isMyServiceRunning(mSensorService.getClass())) {
            startService(mServiceIntent);

        }

//        if(!userWeight.equals("") && !userHeight.equals(""))
//            calculator = new CaloriesCalculator(this, Double.valueOf(userWeight), Double.valueOf(userHeight));
//        else
//            calculator = new CaloriesCalculator(this, 1, 1);
//
          squats = (CheckBox)findViewById(R.id.rbtn_squat);
          squats.setOnClickListener(this);
//
          jumping = (CheckBox)findViewById(R.id.rbtn_jumping);
          jumping.setOnClickListener(this);
//
          climbers = (CheckBox)findViewById(R.id.rbtn_mountain);
          climbers.setOnClickListener(this);
//
          burpees = (CheckBox)findViewById(R.id.rbtn_burpees);
          burpees.setOnClickListener(this);
//
          pushups = (CheckBox)findViewById(R.id.rbtn_pushups);
          pushups.setOnClickListener(this);

          situps = (CheckBox)findViewById(R.id.rbtn_situps);
          situps.setOnClickListener(this);

        InitializeCheckBoxes();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }
        Log.i ("isMyServiceRunning?", false+"");
        return false;
    }

    public void detectWeather() {
        if (!checkLocationPermission()) {
            return;
        }

        Awareness.SnapshotApi.getWeather(mApiClient)
                .setResultCallback(new ResultCallback<WeatherResult>() {
                    @Override
                    public void onResult(@NonNull WeatherResult weatherResult) {
                        Weather weather = weatherResult.getWeather();

                        if(weather != null) {
                            TextView txtWeather = (TextView) findViewById(R.id.txtWeather);
                            txtWeather.setText("Temp: " + weather.getTemperature(Weather.CELSIUS) + System.getProperty("line.separator"));
                            txtWeather.append("Feels like: " + weather.getFeelsLikeTemperature(Weather.CELSIUS) + System.getProperty("line.separator"));
                            txtWeather.append("Humidity: " + weather.getHumidity() + System.getProperty("line.separator"));

                            weathercondition = weather.getConditions()[0];

                            switch (weather.getConditions()[0]) {
                                case Weather.CONDITION_CLOUDY:
                                    txtWeather.append("It's cloudy! It might start raining!");
                                    break;
                                case Weather.CONDITION_CLEAR:
                                    txtWeather.append("It's clear! Go out!");
                                    break;
                                case Weather.CONDITION_FOGGY:
                                    txtWeather.append("It's foggy! Watch out!");
                                    break;
                                case Weather.CONDITION_RAINY:
                                    txtWeather.append("It's rainy! Bring the umbrella!");
                                    break;
                                case Weather.CONDITION_SNOWY:
                                    txtWeather.append("It's snowy! Snowman time!");
                                    break;
                                case Weather.CONDITION_ICY:
                                    txtWeather.append("It's icy! Carefull ride!");
                                    break;
                            }
                            //txtWeather.append("Calories: " + String.valueOf(calculator.CalculateCaloriesBurnedByDoingExercises()));
                        }

                    }
                });
    }


    public static int returnWeatherConditon()
    {
        return weathercondition;
    }

    public boolean checkLocationPermission() {
        if (!hasLocationPermission()) {
            Log.e("Tuts+", "Does not have location permission granted");
            requestLocationPermission();
            return false;
        }

        return true;
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_PERMISSION_RESULT_CODE);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_RESULT_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //granted
                } else {
                    Log.e("Tuts+", "Location permission denied.");
                }
            }
        }
    }


    private void registerFitnessDataListener(DataSource dataSource, DataType dataType) {

        SensorRequest request = new SensorRequest.Builder()
                .setDataSource(dataSource)
                .setDataType(dataType)
                .setSamplingRate(3, TimeUnit.SECONDS)
                .build();

        Fitness.SensorsApi.add(mApiClient, request, this)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.e("GoogleFit", "SensorApi successfully added");
                        }
                    }
                });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("HistoryAPI", "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("HistoryAPI", "onConnectionFailed");
        if (!authInProgress) {
            try {
                authInProgress = true;
                connectionResult.startResolutionForResult(MainActivity.this, REQUEST_OAUTH);
            } catch (IntentSender.SendIntentException e) {

            }
        } else {
            Log.e("GoogleFit", "authInProgress");
        }
    }

    @Override
    public void onDataPoint(DataPoint dataPoint) {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OAUTH) {
            authInProgress = false;
            if (resultCode == RESULT_OK) {
                if (!mApiClient.isConnecting() && !mApiClient.isConnected()) {
                    mApiClient.connect();
                }
            } else if (resultCode == RESULT_CANCELED) {
                Log.e("GoogleFit", "RESULT_CANCELED");
            }
        } else {
            Log.e("GoogleFit", "requestCode NOT request_oauth");
        }


        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_OAUTH_REQUEST_CODE) {
                subscribe();
            }
        }
    }


    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(AUTH_PENDING, authInProgress);
    }


    /**************************STEPS METHODS*************************/


    /**
     * Records step data by requesting a subscription to background step data.
     */
    public void subscribe() {
        // To create a subscription, invoke the Recording API. As soon as the subscription is
        // active, fitness data will start recording.
        Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .subscribe(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .addOnCompleteListener(
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.i(TAG, "Successfully subscribed!");
                                } else {
                                    Log.w(TAG, "There was a problem subscribing.", task.getException());
                                }
                            }
                        });
    }
//
//    /**
//     * Reads the current daily step total, computed from midnight of the current day on the device's
//     * current timezone.
//     */
    private String readData() {
        final String[] totalSteps = {""};

        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                .addOnSuccessListener(
                        new OnSuccessListener<DataSet>() {
                            @Override
                            public void onSuccess(DataSet dataSet) {
                                long total =
                                        dataSet.isEmpty()
                                                ? 0
                                                : dataSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
                                //   Log.i(TAG, "Total steps: " + total);
                                ShowNumberOfSteps(total);
                                mSensorService.calculator.SetNumberOfSteps(total, getCtx());
                                //notificationCounter is here to provide sending notfication for this event only once
                                SharedPreferences prefs = getSharedPreferences("StepsGoalNotification", MODE_PRIVATE);
                                Boolean firedToday = prefs.getBoolean("FiredToday", false);
                                if(total >= StepsGoal && !firedToday)
                                {
                                    CreateNotification("Well Done! You achived your steps goal for today!");
                                    //notificationCounter ++;
                                    SetSharedPreference(true);
                                }

                                SetSharedPreferenceLong("StepsCount", total);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "There was a problem getting the step count.", e);
                            }
                        });
        return totalSteps[0];

    }

    public void ShowNumberOfSteps(long total) {
        TextView textSteps = (TextView) findViewById(R.id.title_text_view);
        String steps = String.valueOf(total);
        if (textSteps.getText() != steps && steps != "")
            textSteps.setText(steps);
    }
/****************************************END STEP METHODS**************************************************/


    /***************************************ON CLICK EVENT FOR HISOTRY API************************************/

    public void OpenHistoryActivity() {
        Intent history = new Intent(MainActivity.this, ViewHistoryAPI.class);
        startActivity(history);
    }


    /***************************************ON CLICK EVENT FOR OPENING CALENDAR************************************/

    public void OpenCalenadrActivity() {
        TextView textViewSteps = (TextView) findViewById(R.id.title_text_view);
        Intent calendar = new Intent(MainActivity.this, CalendarActivity.class);
        Bundle b = new Bundle();
        b.putString("steps", textViewSteps.getText().toString());
//        b.putString("weight", userWeight);
//        b.putString("height", userHeight);
        calendar.putExtras(b); //parameter to next Intent
        startActivity(calendar);
    }

    /***************************************STORE LOCATIONS************************************/
    public void GetAndStoreCurrentLocation()
    {
        if (ContextCompat.checkSelfPermission(
                MainActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    12345
            );
        }

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
        NotificationManagerCompat.from(getApplicationContext()).notify(0, builder.build());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //return super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.action_buttons, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //return super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.googleSetup:
                // User chose the "google setup in FirebaseLogin" item, show the app settings UI...
                Intent firebaseLogin = new Intent(this, FirebaseLogin.class);
                startActivity(firebaseLogin);
                return true;

            case R.id.configureProfile:
                // User chose the "configure profile, this page" action, mark the current item
                // as a favorite...
                Intent configure = new Intent(this, ConfigureProfile.class);
                startActivity(configure);
                return true;

            case R.id.logout:
                // User chose the "configure profile, this page" action, mark the current item
                // as a favorite...
                mAuth.getInstance().signOut();

                //google sign out
                if (mApiClient.isConnected()) {
                    //Auth.GoogleSignInApi.signOut(mApiClient);
                    //Plus.AccountApi.clearDefaultAccount(mApiClient);
                    mApiClient.disconnect();
//                    mApiClient.connect();
                }

                Intent login = new Intent(this, FirebaseLogin.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(login);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }
//    public void onCheckboxClicked(View view) {
//        // Is the view now checked?
//        boolean checked = ((CheckBox) view).isChecked();
//
//        // Check which checkbox was clicked
//        switch(view.getId()) {
//            case R.id.rbtn_squat:
//                if (checked)
//                    mSensorService.calculator.SetSharedPreferencesForExcersise(CaloriesCalculator.squats, true);
//                else
//                    mSensorService.calculator.SetSharedPreferencesForExcersise(CaloriesCalculator.squats, false);
//                break;
//            case R.id.rbtn_situps:
//                if (checked)
//                    mSensorService.calculator.SetSharedPreferencesForExcersise(CaloriesCalculator.situps, true);
//                else
//                    mSensorService.calculator.SetSharedPreferencesForExcersise(CaloriesCalculator.situps, false);
//                break;
//            case R.id.rbtn_jumping:
//                if (checked)
//                    mSensorService.calculator.SetSharedPreferencesForExcersise(CaloriesCalculator.jumpingJack, true);
//                else
//                    mSensorService.calculator.SetSharedPreferencesForExcersise(CaloriesCalculator.jumpingJack, false);
//                break;
//            case R.id.rbtn_burpees:
//                if (checked)
//                    mSensorService.calculator.SetSharedPreferencesForExcersise(CaloriesCalculator.burpees, true);
//                else
//                    mSensorService.calculator.SetSharedPreferencesForExcersise(CaloriesCalculator.burpees, false);
//                break;
//            case R.id.rbtn_mountain:
//                if (checked)
//                    mSensorService.calculator.SetSharedPreferencesForExcersise(CaloriesCalculator.mountainClimbers, true);
//                else
//                    mSensorService.calculator.SetSharedPreferencesForExcersise(CaloriesCalculator.mountainClimbers, false);
//                break;
//            case R.id.rbtn_pushups:
//                if (checked)
//                    mSensorService.calculator.SetSharedPreferencesForExcersise(CaloriesCalculator.pushups, true);
//                else
//                    mSensorService.calculator.SetSharedPreferencesForExcersise(CaloriesCalculator.pushups, false);
//                break;
//            // TODO: Veggie sandwich
//        }
//    }
    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.button_showCalendar) {
            OpenCalenadrActivity();
        } else if (i == R.id.button_viewHistory) {
            OpenHistoryActivity();
        }
        else if (i == R.id.rbtn_squat) {
         //   CheckBox squats = (CheckBox)findViewById(R.id.rbtn_squat);
            if(squats.isChecked()) {
                mSensorService.calculator.SetSharedPreferencesForExcersise(CaloriesCalculator.squats, true, this);
            }
            else
            {
                mSensorService.calculator.SetSharedPreferencesForExcersise(CaloriesCalculator.squats,false,  this);
            }
        }
        else if (i == R.id.rbtn_jumping) {
            //   CheckBox squats = (CheckBox)findViewById(R.id.rbtn_squat);
            if(jumping.isChecked()) {
                mSensorService.calculator.SetSharedPreferencesForExcersise(CaloriesCalculator.jumpingJack, true, this);
            }
            else
            {
                mSensorService.calculator.SetSharedPreferencesForExcersise(CaloriesCalculator.jumpingJack, false, this);
            }
        }
        else if (i == R.id.rbtn_mountain) {
            //   CheckBox squats = (CheckBox)findViewById(R.id.rbtn_squat);
            if(climbers.isChecked()) {
                mSensorService.calculator.SetSharedPreferencesForExcersise(CaloriesCalculator.mountainClimbers, true, this);
            }
            else
            {
                mSensorService.calculator.SetSharedPreferencesForExcersise(CaloriesCalculator.mountainClimbers, false, this);
            }
        }
        else if (i == R.id.rbtn_burpees) {
            //   CheckBox squats = (CheckBox)findViewById(R.id.rbtn_squat);
            if(burpees.isChecked()) {
                mSensorService.calculator.SetSharedPreferencesForExcersise(CaloriesCalculator.burpees, true, this);
            }
            else
            {
                mSensorService.calculator.SetSharedPreferencesForExcersise(CaloriesCalculator.burpees, false, this);
            }
        }
        else if (i == R.id.rbtn_pushups) {
            //   CheckBox squats = (CheckBox)findViewById(R.id.rbtn_squat);
            if(pushups.isChecked()) {
                mSensorService.calculator.SetSharedPreferencesForExcersise(CaloriesCalculator.pushups, true, this);
            }
            else
            {
                mSensorService.calculator.SetSharedPreferencesForExcersise(CaloriesCalculator.pushups, false, this);
            }
        }
        else if (i == R.id.rbtn_situps) {
            //   CheckBox squats = (CheckBox)findViewById(R.id.rbtn_squat);
            if(situps.isChecked()) {
                mSensorService.calculator.SetSharedPreferencesForExcersise(CaloriesCalculator.situps, true, this);
            }
            else
            {
                mSensorService.calculator.SetSharedPreferencesForExcersise(CaloriesCalculator.situps, false, this);
            }
        }


    }
//    public void SetNumberOfExercises()
//    {
//        SharedPreferences.Editor editor = getSharedPreferences(ExerciseTag, MODE_PRIVATE).edit();
//        editor.putInt(CaloriesCalculator.numberExcercises, CaloriesCalculator.numberOfExercises);
//        editor.apply();
//    }
//    public int GetNumberOfExercises()
//    {
//        SharedPreferences pref = getSharedPreferences(ExerciseTag, Context.MODE_PRIVATE);
//        int value = pref.getInt(CaloriesCalculator.numberExcercises, 0);
//        return value;
//    }
//
//    public void SetSharedPreferencesForExcersise(String name, Boolean value)
//    {
//        SharedPreferences.Editor editor = getSharedPreferences(ExerciseTag, MODE_PRIVATE).edit();
//        editor.putBoolean(name, value);
//        editor.apply();
//    }
//    public boolean GetSharedPreferencesForExercise(String name)
//    {
//        SharedPreferences pref = getSharedPreferences(ExerciseTag, Context.MODE_PRIVATE);
//        boolean value = pref.getBoolean(name, false);
//        return value;
//    }
    private void InitializeDB()
    {
        final boolean[] checker = {false};
        final AsyncTask task = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                //send all objects from sqllite to node server
                Cursor cursor = mDbHelper.ReadDataFromDatabase();
                if(cursor.getCount() > 0) {
                    final List<UserActivityProperties> properties = mDbHelper.GetObjectsFromCursor(cursor);
                    if (properties != null) {
                        if(HTTPHelper.setProperties(properties)) {
                            checker[0] = true;
                        }
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                //delete all objects from sqllite if there is connection to the server
//                if(checker[0]) {
//                    mDbHelper.DeleteDataFromDatabase();
//                }
            }
        };
        task.execute();
    }

    private void InitializeCheckBoxes()
    {

        boolean checked = mSensorService.calculator.GetSharedPreferencesForExercise(CaloriesCalculator.squats, this);
        //CheckBox squats = (CheckBox)findViewById(R.id.rbtn_squat);
        squats.setChecked(checked);

        checked = mSensorService.calculator.GetSharedPreferencesForExercise(CaloriesCalculator.jumpingJack, this);
        //CheckBox squats = (CheckBox)findViewById(R.id.rbtn_squat);
        jumping.setChecked(checked);

        checked = mSensorService.calculator.GetSharedPreferencesForExercise(CaloriesCalculator.mountainClimbers, this);
        //CheckBox squats = (CheckBox)findViewById(R.id.rbtn_squat);
        climbers.setChecked(checked);

        checked = mSensorService.calculator.GetSharedPreferencesForExercise(CaloriesCalculator.burpees, this);
        //CheckBox squats = (CheckBox)findViewById(R.id.rbtn_squat);
        burpees.setChecked(checked);

        checked = mSensorService.calculator.GetSharedPreferencesForExercise(CaloriesCalculator.pushups, this);
        //CheckBox squats = (CheckBox)findViewById(R.id.rbtn_squat);
        pushups.setChecked(checked);

        checked = mSensorService.calculator.GetSharedPreferencesForExercise(CaloriesCalculator.situps, this);
        //CheckBox squats = (CheckBox)findViewById(R.id.rbtn_squat);
        situps.setChecked(checked);
    }
}
