package com.example.petra.healthylifeapp;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.HeadphoneFence;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import static com.example.petra.healthylifeapp.MainActivity.FENCE_RECEIVER_ACTION;

/**
 * Created by Petra on 2/20/2018.
 */

public class BackgroundService extends IntentService implements GoogleApiClient.ConnectionCallbacks{

    public GoogleApiClient mApiClient;

    // Declare variables for pending intent and fence receiver.
    private PendingIntent myPendingIntent;

    public String TAG = "BackgroundService: ";

    public BackgroundService(){super("BackgroundService");}

    @Override
    public void onCreate() {
        super.onCreate();

        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(Awareness.API)
                .addConnectionCallbacks(this)
                //.enableAutoManage(this, 0, this)
                .build();
        mApiClient.connect();

        //pending intent for headphones FENCES api
        Intent intent1 = new Intent(FENCE_RECEIVER_ACTION);
        myPendingIntent = PendingIntent.getBroadcast(this, 1, intent1, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        FenceReceiver receiver = new BackgroundService.FenceReceiver();
        registerReceiver(receiver, new IntentFilter(FENCE_RECEIVER_ACTION));

        AwarenessFence headphoneFence = HeadphoneFence.during(HeadphoneState.PLUGGED_IN);
        createFence("headphoneFenceKey", headphoneFence);

        AwarenessFence stillFence = DetectedActivityFence.starting(DetectedActivityFence.STILL);
        createFence("stillFenceKey", stillFence);

        AwarenessFence walkingFence = DetectedActivityFence.starting(DetectedActivityFence.WALKING);
        createFence("walkingFenceKey", walkingFence);

//        AwarenessFence invehicleFence = DetectedActivityFence.during(DetectedActivityFence.IN_VEHICLE);
//        createFence("invehicleFenceKey", invehicleFence);
//
//        AwarenessFence onbicycleFence = DetectedActivityFence.during(DetectedActivityFence.ON_BICYCLE);
//        createFence("onbicycleFenceKey", onbicycleFence);
//
//        AwarenessFence runningFence = DetectedActivityFence.during(DetectedActivityFence.RUNNING);
//        createFence("runningFenceKey", runningFence);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("HistoryAPI", "onConnectionSuspended");
    }


    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        //other code
        return START_STICKY;
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

    public class FenceReceiver extends BroadcastReceiver {

        private static final String TAG = "Fences API";

        @Override
        public void onReceive(Context context, Intent intent) {
            FenceState fenceState = FenceState.extract(intent);

//            TextView textView = (TextView)findViewById(R.id.textViewHeadphones);
//            TextView textView1 = (TextView)findViewById(R.id.textViewActivity);

            if (TextUtils.equals(fenceState.getFenceKey(), "headphoneFenceKey")) {
                switch(fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        Log.i(TAG, "Headphones are plugged in.");
                        //textView.setText("Headphones are plugged in.");
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
                        builder.setContentText( "Headphones are plugged in." );
                        builder.setSmallIcon( R.mipmap.ic_launcher );
                        builder.setContentTitle( getString( R.string.app_name ) );
                        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        builder.setSound(alarmSound);
                        long[] vibrate = { 0, 100 };
                        builder.setVibrate(vibrate);
                        NotificationManagerCompat.from(getApplicationContext()).notify(0, builder.build());
                        break;
                }
            }

            if (TextUtils.equals(fenceState.getFenceKey(), "stillFenceKey")) {
                switch(fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        Log.i(TAG, "You are still.");
                        FirebaseUtility.SaveTime("timeStill");
                        //textView.setText("Headphones are plugged in.");
                        break;
                }
            }

            if (TextUtils.equals(fenceState.getFenceKey(), "walkingFenceKey")) {
                switch(fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        Log.i(TAG, "You are walking!");
                        FirebaseUtility.SaveTime("timeWalking");
                        //textView.setText("Headphones are plugged in.");
                        break;
                }
            }
        }
    }

}
