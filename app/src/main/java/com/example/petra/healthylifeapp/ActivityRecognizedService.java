package com.example.petra.healthylifeapp;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

/**
 * Created by Petra on 2/7/2018.
 */

public class ActivityRecognizedService extends IntentService {

    //public boolean StillFlag = true;
    public static int TimeStill = 0;
    public static int TimeWalking = 0;

    public ActivityRecognizedService() {
        super("ActivityRecognizedService");
    }

    public ActivityRecognizedService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            handleDetectedActivities( result.getProbableActivities() );
        }
    }
    

    private void handleDetectedActivities(List<DetectedActivity> probableActivities) {
        for( DetectedActivity activity : probableActivities ) {
            switch( activity.getType() ) {
                case DetectedActivity.IN_VEHICLE: {
                    Log.e( "ActivityRecognition", "In Vehicle: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.ON_BICYCLE: {
                    Log.e( "ActivityRecognition", "On Bicycle: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.ON_FOOT: {
                    Log.e( "ActivityRecognition", "On Foot: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.RUNNING: {
                    Log.e( "ActivityRecognition", "Running: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.STILL: {
                    Log.e( "ActivityRecognition", "Still: " + activity.getConfidence() );

                    if( activity.getConfidence() >= 75 ) {
                        TimeStill++;
                        Log.e( "TimeStill", "Time: " + TimeStill );
                    }

                    if(TimeStill >= 12)
                    { NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
                        builder.setContentText( "You are sitting for too long!" );
                        builder.setSmallIcon( R.mipmap.ic_launcher );
                        builder.setContentTitle( getString( R.string.app_name ) );
                        NotificationManagerCompat.from(this).notify(0, builder.build());

                        TimeStill = 0;
                    }
                    break;
                }
                case DetectedActivity.TILTING: {
                    Log.e( "ActivityRecognition", "Tilting: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.WALKING: {
                    Log.e( "ActivityRecognition", "Walking: " + activity.getConfidence() );
                    if( activity.getConfidence() >= 75 ) {
                        TimeWalking++;
                        TimeStill = 0;
                    }

                    if(TimeWalking >= 12*60)
                    {
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
                        builder.setContentText( "You are walking for so long! Well done!" );
                        builder.setSmallIcon( R.mipmap.ic_launcher );
                        builder.setContentTitle( getString( R.string.app_name ) );
                        NotificationManagerCompat.from(this).notify(0, builder.build());

                        TimeWalking = 0;
                    }
                    break;
                }
                case DetectedActivity.UNKNOWN: {
                    Log.e( "ActivityRecognition", "Unknown: " + activity.getConfidence() );
                    break;
                }
            }
        }
    }
}
