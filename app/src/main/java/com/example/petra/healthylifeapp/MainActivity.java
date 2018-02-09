package com.example.petra.healthylifeapp;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
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
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
public class MainActivity extends AppCompatActivity implements OnDataPointListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public GoogleApiClient mApiClient;

    private static final int REQUEST_OAUTH = 1;
    private static final String AUTH_PENDING = "auth_state_pending";
    private boolean authInProgress = false;

    private ResultCallback<Status> mSubscribeResultCallback;
    private ResultCallback<Status> mCancelSubscriptionResultCallback;
    private ResultCallback<ListSubscriptionsResult> mListSubscriptionsResultCallback;

    public static final String TAG = "StepCounter";
    private static final int REQUEST_OAUTH_REQUEST_CODE = 0x1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }

        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.SENSORS_API)
                .addApi(Fitness.RECORDING_API)
                .addApi(ActivityRecognition.API)
                .addApi(Fitness.HISTORY_API)
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
        Button button= (Button)findViewById(R.id.button_viewHistory);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                OpenHistoryActivity(view);
            }
        });
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
                .setDataTypes( DataType.TYPE_STEP_COUNT_CUMULATIVE )
                .setDataSourceTypes( DataSource.TYPE_RAW )
                .build();

        ResultCallback<DataSourcesResult> dataSourcesResultCallback = new ResultCallback<DataSourcesResult>() {
            @Override
            public void onResult(DataSourcesResult dataSourcesResult) {
                for( DataSource dataSource : dataSourcesResult.getDataSources() ) {
                    if( DataType.TYPE_STEP_COUNT_CUMULATIVE.equals( dataSource.getDataType() ) ) {
                        registerFitnessDataListener(dataSource, DataType.TYPE_STEP_COUNT_CUMULATIVE);
                    }
                }
            }
        };

        Fitness.SensorsApi.findDataSources(mApiClient, dataSourceRequest)
                .setResultCallback(dataSourcesResultCallback);

        Fitness.RecordingApi.subscribe(mApiClient, DataType.TYPE_STEP_COUNT_DELTA)
                .setResultCallback(mSubscribeResultCallback);

        if(mApiClient.isConnected()){
            Log.i("mApiClient", "Google_Api_Client: It was connected on (onConnected) function, working as it should.");
        }
        else{
            Log.i("mApiClient", "Google_Api_Client: It was NOT connected on (onConnected) function, It is definetly bugged.");
        }


    }
    private void registerFitnessDataListener(DataSource dataSource, DataType dataType) {

        SensorRequest request = new SensorRequest.Builder()
                .setDataSource( dataSource )
                .setDataType( dataType )
                .setSamplingRate( 3, TimeUnit.SECONDS )
                .build();

        Fitness.SensorsApi.add( mApiClient, request, this )
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.e( "GoogleFit", "SensorApi successfully added" );
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
        if( !authInProgress ) {
            try {
                authInProgress = true;
                connectionResult.startResolutionForResult( MainActivity.this, REQUEST_OAUTH );
            } catch(IntentSender.SendIntentException e ) {

            }
        } else {
            Log.e( "GoogleFit", "authInProgress" );
        }
    }

    @Override
    public void onDataPoint(DataPoint dataPoint) {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if( requestCode == REQUEST_OAUTH ) {
            authInProgress = false;
            if( resultCode == RESULT_OK ) {
                if( !mApiClient.isConnecting() && !mApiClient.isConnected() ) {
                    mApiClient.connect();
                }
            } else if( resultCode == RESULT_CANCELED ) {
                Log.e( "GoogleFit", "RESULT_CANCELED" );
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


    /** Records step data by requesting a subscription to background step data. */
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

   public void ShowNumberOfSteps(String steps)
   {
       TextView textViewSteps = (TextView)findViewById(R.id.title_text_view);
       if(textViewSteps.getText() != steps && steps != "")
            textViewSteps.setText(steps);

   }
/****************************************END STEP METHODS**************************************************/


/***************************************ON CLICK EVENT FOR HISOTRY API************************************/

public void OpenHistoryActivity(View view)
{
    Intent history = new Intent(MainActivity.this, ViewHistoryAPI.class);
    startActivity(history);
}


}
