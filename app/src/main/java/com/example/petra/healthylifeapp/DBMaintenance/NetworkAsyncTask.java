package com.example.petra.healthylifeapp.DBMaintenance;

import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

/**
 * Created by Milica on 3/10/2018.
 */

public class NetworkAsyncTask extends AsyncTask<Void, Void, Void> {

    FeedReaderDbHelper mDbHelper;

    public NetworkAsyncTask(FeedReaderDbHelper mdbHelper) {
        this.mDbHelper = mdbHelper;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            //UserActivityProperties property = new UserActivityProperties();
            List<UserActivityProperties> props = ReadDataFromSQLLite();
            if (HTTPHelper.setProperties(props)) {
                //delete from sqllite after putting data to phpmyadmin
                mDbHelper.DeleteDataFromDatabase();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private List<UserActivityProperties> ReadDataFromSQLLite() {
        List<UserActivityProperties> activities = mDbHelper.GetObjectsFromCursor(mDbHelper.ReadDataFromDatabase());

        for (int i = 0; i < activities.size(); i++) {
            Log.i("Activitie " + i + ": ", String.valueOf(activities.get(i)));
        }

        return activities;
    }
}
