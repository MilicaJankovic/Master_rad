package com.example.petra.healthylifeapp;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.HeadphoneFence;
import com.google.android.gms.awareness.snapshot.HeadphoneStateResult;
import com.google.android.gms.awareness.snapshot.WeatherResult;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Subscription;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.fitness.result.DataSourcesResult;
import com.google.android.gms.fitness.result.ListSubscriptionsResult;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements OnDataPointListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String TAG = "StepCounter";
    private static final int REQUEST_OAUTH = 1;
    private static final String AUTH_PENDING = "auth_state_pending";
    private static final int REQUEST_OAUTH_REQUEST_CODE = 0x1001;
    public GoogleApiClient mApiClient;
    private boolean authInProgress = false;
    private ResultCallback<Status> mSubscribeResultCallback;
    private ResultCallback<Status> mCancelSubscriptionResultCallback;
    private ResultCallback<ListSubscriptionsResult> mListSubscriptionsResultCallback;


    public static final String FENCE_RECEIVER_ACTION =
            "com.hitherejoe.aware.ui.fence.FenceReceiver.FENCE_RECEIVER_ACTION";

    //firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;


    // Declare variables for pending intent and fence receiver.
    private PendingIntent myPendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        //pending intent for headphones FENCES api
        Intent intent1 = new Intent(FENCE_RECEIVER_ACTION);
        myPendingIntent = PendingIntent.getBroadcast(this, 0, intent1, 0);
        FenceReceiver receiver = new FenceReceiver();
        registerReceiver(receiver, new IntentFilter(FENCE_RECEIVER_ACTION));


        if(currentUser != null) {
//            mDatabase = FirebaseDatabase.getInstance().getReference();
//            if(mDatabase != null)
//            {
//            }

            if (savedInstanceState != null) {
                authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
            }

            mApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Fitness.SENSORS_API)
                    .addApi(Fitness.RECORDING_API)
                    .addApi(ActivityRecognition.API)
                    .addApi(Fitness.HISTORY_API)
                    .addApi(Awareness.API)
                    .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .enableAutoManage(this, 0, this)
                    .build();
            mApiClient.connect();
            initCallbacks();

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


            /****************OPEN HISTORY CLICK********************************/
            Button buttonHistory = (Button) findViewById(R.id.button_viewHistory);
            buttonHistory.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    OpenHistoryActivity(view);
                }
            });

            /****************OPEN CALENDAR CLICK********************************/
            Button buttonCalendar = (Button) findViewById(R.id.button_showCalendar);
            buttonCalendar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    OpenCalenadrActivity(view);
                }
            });

        }
        else{
            Intent intent = new Intent(this, FirebaseLogin.class);
            startActivity(intent);
        }

        //removeFence("headphoneFenceKey");
    }

    @Override
    protected void onStart() {
        super.onStart();
        mApiClient.connect();
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


//        //check if the headphones are connected
//        Awareness.SnapshotApi.getHeadphoneState(mApiClient)
//                .setResultCallback(new ResultCallback<HeadphoneStateResult>() {
//                    @Override
//                    public void onResult(@NonNull HeadphoneStateResult headphoneStateResult) {
//                        if (headphoneStateResult.getStatus().isSuccess()) {
//                            HeadphoneState headphoneState =
//                                    headphoneStateResult.getHeadphoneState();
//                            int state = headphoneState.getState();
//
//                            TextView textViewHeadphones = (TextView) findViewById(R.id.textViewHeadphones);
//
//                            if (state == HeadphoneState.PLUGGED_IN) {
//                                // Headphones plugged in
//                                textViewHeadphones.setText("Headphones plugged in...");
//                            } else if (state == HeadphoneState.UNPLUGGED) {
//                                // Headphones unplugged
//                                textViewHeadphones.setText("Headphones unplugged...");
//                            }
//                        }
//                    }
//                });

        detectWeather();



        // Create a fence.
        AwarenessFence headphoneFence = HeadphoneFence.during(HeadphoneState.PLUGGED_IN);
        AwarenessFence activityFence = DetectedActivityFence.during(DetectedActivity.WALKING);
        //AwarenessFence startedFence = DetectedActivityFence.starting(DetectedActivity.WALKING);
//        AwarenessFence endedFence = DetectedActivityFence.stopping(DetectedActivity.WALKING);

        AwarenessFence orFence = AwarenessFence.or(headphoneFence, activityFence);

        createFence("orFenceKey", orFence);
    }


    private void createFence(final String fenceKey, AwarenessFence fence){
        // Register the fence to receive callbacks.
        // The fence key uniquely identifies the fence.
        Awareness.FenceApi.updateFences(
                mApiClient,
                new FenceUpdateRequest.Builder()
                        //.addFence("headphoneFenceKey", headphoneFence, myPendingIntent)
                        .addFence(fenceKey, fence, myPendingIntent)
                        .build())
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Fence was successfully registered.");
                        } else {
                            Log.e(TAG, "Fence could not be registered: " + status);
                        }
                    }
                });
    }

    private void removeFence(final String fenceKey) {
        Awareness.FenceApi.updateFences(
                mApiClient,
                new FenceUpdateRequest.Builder()
                        .removeFence(fenceKey)
                        .build()).setResultCallback(new ResultCallbacks() {

            @Override
            public void onSuccess(@NonNull Result result) {
                String info = "Fence " + fenceKey + " successfully removed.";
                Log.i(TAG, info);
                Toast.makeText(MainActivity.this, info, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(@NonNull Status status) {
                String info = "Fence " + fenceKey + " could NOT be removed.";
                Log.i(TAG, info);
                Toast.makeText(MainActivity.this, info, Toast.LENGTH_LONG).show();
            }
        });
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
//                        Log.e("Tuts+", "Temp: " + weather.getTemperature(Weather.FAHRENHEIT));
//                        Log.e("Tuts+", "Feels like: " + weather.getFeelsLikeTemperature(Weather.FAHRENHEIT));
//                        Log.e("Tuts+", "Dew point: " + weather.getDewPoint(Weather.FAHRENHEIT));
//                        Log.e("Tuts+", "Humidity: " + weather.getHumidity() );

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


    private boolean checkLocationPermission() {
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

    private final static int REQUEST_PERMISSION_RESULT_CODE = 42;

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(
                MainActivity.this,
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

    private void initCallbacks() {
//        mSubscribeResultCallback = new ResultCallback<Status>() {
//            @Override
//            public void onResult(@NonNull Status status) {
//                if (status.isSuccess()) {
//                    if (status.getStatusCode() == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
//                        Log.e("RecordingAPI", "Already subscribed to the Recording API");
//                    } else {
//                        Log.e("RecordingAPI", "Subscribed to the Recording API");
//                    }
//                }
//            }
//        };
//
//        mCancelSubscriptionResultCallback = new ResultCallback<Status>() {
//            @Override
//            public void onResult(@NonNull Status status) {
//                if (status.isSuccess()) {
//                    Log.e("RecordingAPI", "Canceled subscriptions!");
//                } else {
//                    // Subscription not removed
//                    Log.e("RecordingAPI", "Failed to cancel subscriptions");
//                }
//            }
//        };
//
//        mListSubscriptionsResultCallback = new ResultCallback<ListSubscriptionsResult>() {
//            @Override
//            public void onResult(@NonNull ListSubscriptionsResult listSubscriptionsResult) {
//                for (Subscription subscription : listSubscriptionsResult.getSubscriptions()) {
//                    DataType dataType = subscription.getDataType();
//                    Log.e("RecordingAPI", dataType.getName());
//                    for (Field field : dataType.getFields()) {
//                        Log.e("RecordingAPI", field.toString());
//                    }
//                }
//            }
//        };
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

    public void OpenHistoryActivity(View view) {
        Intent history = new Intent(MainActivity.this, ViewHistoryAPI.class);
        startActivity(history);
    }


    /***************************************ON CLICK EVENT FOR OPENING CALENDAR************************************/

    public void OpenCalenadrActivity(View view) {
        TextView textViewSteps = (TextView) findViewById(R.id.title_text_view);
        Intent calendar = new Intent(MainActivity.this, CalendarActivity.class);
        Bundle b = new Bundle();
        b.putString("steps", textViewSteps.getText().toString());
        calendar.putExtras(b); //parameter to next Intent
        startActivity(calendar);
    }


    /****************************************MENU*******************************************************/

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



    public class FenceReceiver extends BroadcastReceiver {

        private static final String TAG = "Fences API";

        @Override
        public void onReceive(Context context, Intent intent) {
            FenceState fenceState = FenceState.extract(intent);

            TextView textView = (TextView)findViewById(R.id.textViewHeadphones);
            TextView textView1 = (TextView)findViewById(R.id.textViewActivity);

            if (TextUtils.equals(fenceState.getFenceKey(), "headphoneFenceKey")) {
                switch(fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        Log.i(TAG, "Headphones are plugged in.");
                        textView.setText("Headphones are plugged in.");
                        break;
                    case FenceState.FALSE:
                        Log.i(TAG, "Headphones are NOT plugged in.");
                        textView.setText("Headphones are NOT plugged in.");
                        break;
                    case FenceState.UNKNOWN:
                        Log.i(TAG, "The headphone fence is in an unknown state.");
                        break;
                }
            }
            else if(TextUtils.equals(fenceState.getFenceKey(), "orFenceKey")) {
                switch(fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        textView1.setText("You are walking or heaphones are pluged.");
                        break;
                    case FenceState.FALSE:
                        textView1.setText("You are not walking or headphoens are not plugged!");
                        break;
                }
            }
            else if(TextUtils.equals(fenceState.getFenceKey(), "activityFenceStarted")) {
                switch(fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        textView1.setText("You started walikng.");
                        break;
                }
            }
            else if(TextUtils.equals(fenceState.getFenceKey(), "activityFenceStopped")) {
                switch(fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        textView1.setText("You stopped walikng.");
                        break;
                }
            }


        }
    }
}
