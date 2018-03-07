package com.example.petra.healthylifeapp;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
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
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
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
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements OnDataPointListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    public static final String TAG = "StepCounter";
    public static final String FENCE_RECEIVER_ACTION =
            "com.hitherejoe.aware.ui.fence.FenceReceiver.FENCE_RECEIVER_ACTION";
    private static final int REQUEST_OAUTH = 1;
    private static final String AUTH_PENDING = "auth_state_pending";
    private static final int REQUEST_OAUTH_REQUEST_CODE = 0x1001;
    private final static int REQUEST_PERMISSION_RESULT_CODE = 42;
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

    public Context getCtx() {
        return ctx;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }


        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();


//        BackgroundService service = new BackgroundService();
//        BackgroundService.FenceReceiver receiver = service.new FenceReceiver();
//        BackgroundService.FenceReceiver receiver = new BackgroundService.FenceReceiver();
        //registerReceiver(receiver, new IntentFilter(FENCE_RECEIVER_ACTION));
        // unregisterReceiver(receiver);

        if (currentUser != null) {

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
        /************************END STEPS*********************************/
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


        SetSharedPreference(false);
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

        mSensorService = new SensorService(getApplicationContext());
        mServiceIntent = new Intent(getApplicationContext(), mSensorService.getClass());
        if (!isMyServiceRunning(mSensorService.getClass())) {
            startService(mServiceIntent);
        }
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

    private void detectWeather() {
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

                        }
                    }
                });
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

    /**
     * Reads the current daily step total, computed from midnight of the current day on the device's
     * current timezone.
     */
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
                                ShowNumberOfSteps(String.valueOf(total));

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

    public void ShowNumberOfSteps(String steps) {
        TextView textViewSteps = (TextView) findViewById(R.id.title_text_view);
        if (textViewSteps.getText() != steps && steps != "")
            textViewSteps.setText(steps);

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

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.button_showCalendar) {
            OpenCalenadrActivity();
        } else if (i == R.id.button_viewHistory) {
            OpenHistoryActivity();
        }
    }
}
