package com.example.petra.healthylifeapp;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by Petra on 2/17/2018.
 */

public class HealthyLifeApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
