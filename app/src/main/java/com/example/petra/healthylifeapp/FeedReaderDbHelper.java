package com.example.petra.healthylifeapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Milica on 3/11/2018.
 */

public class FeedReaderDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "UserActivity.db";



    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + FeedReaderContract.FeedEntry.TABLE_NAME + " (" +
                    FeedReaderContract.FeedEntry._ID + " INTEGER PRIMARY KEY," +
                    FeedReaderContract.FeedEntry.COLUMN_NAME_GENDER + " TEXT," +
                    FeedReaderContract.FeedEntry.COLUMN_NAME_AGE + " INTEGER," +
                    FeedReaderContract.FeedEntry.COLUMN_NAME_STEPSNUM + " INTEGER," +
                    FeedReaderContract.FeedEntry.COLUMN_NAME_STILL + " INTEGER," +
                    FeedReaderContract.FeedEntry.COLUMN_NAME_CONTINUOUSSTILL + " INTEGER," +
                    FeedReaderContract.FeedEntry.COLUMN_NAME_RUNNING + " INTEGER," +
                    FeedReaderContract.FeedEntry.COLUMN_NAME_DRIVING + " INTEGER," +
                    FeedReaderContract.FeedEntry.COLUMN_NAME_CYCLING + " INTEGER," +
                    FeedReaderContract.FeedEntry.COLUMN_NAME_SLEEPING + " INTEGER," +
                    FeedReaderContract.FeedEntry.COLUMN_NAME_WEATHER + " TEXT," +
                    FeedReaderContract.FeedEntry.COLUMN_NAME_TARGETWEIGHT + " INTEGER," +
                    FeedReaderContract.FeedEntry.COLUMN_NAME_CALORIES + " INTEGER," +
                    FeedReaderContract.FeedEntry.COLUMN_NAME_DAYOFTHEWEEK + " INTEGER," +
                    FeedReaderContract.FeedEntry.COLUMN_NAME_PARTOFTHEDAY + " TEXT," +
                    FeedReaderContract.FeedEntry.COLUMN_NAME_USERINPUT + " INTEGER)";



    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + FeedReaderContract.FeedEntry.TABLE_NAME;

    public FeedReaderDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        super.onDowngrade(db, oldVersion, newVersion);
    }

    public void InsertToDatabase(UserActivityProperties properties)
    {
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_GENDER, properties.getGender());
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_AGE, properties.getAge());
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_STEPSNUM, properties.getStepsNum());
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_STILL, properties.getStill());
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CONTINUOUSSTILL, properties.getContinuousStill());
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_RUNNING, properties.getRunning());
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_DRIVING, properties.getDriving());
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CYCLING, properties.getCycling());
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_SLEEPING, properties.getSleeping());
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_WEATHER, properties.getWeather());
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_TARGETWEIGHT, properties.getTargetWeight());
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CALORIES, properties.getCalories());
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_DAYOFTHEWEEK, properties.getDayOfTheWeek());
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_PARTOFTHEDAY, properties.getPartOfTheDay());
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_USERINPUT, properties.getUserInput());

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(FeedReaderContract.FeedEntry.TABLE_NAME, null, values);
    }


    public List<UserActivityProperties> GetObjectsFromCursor(Cursor cursor)
    {
        List<UserActivityProperties> properties = new ArrayList<UserActivityProperties>();
        cursor.moveToFirst();
        do {
            UserActivityProperties property = new UserActivityProperties();
            property = property.SetProperties(cursor.getString(cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME_GENDER)),
                    cursor.getInt(cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME_AGE)),
                    cursor.getInt(cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME_STEPSNUM)),
                    cursor.getInt(cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME_STILL)),
                    cursor.getInt(cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME_CONTINUOUSSTILL)),
                    cursor.getInt(cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME_RUNNING)),
                    cursor.getInt(cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME_DRIVING)),
                    cursor.getInt(cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME_CYCLING)),
                    cursor.getInt(cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME_SLEEPING)),
                    cursor.getString(cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME_WEATHER)),
                    cursor.getInt(cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME_TARGETWEIGHT)),
                    cursor.getInt(cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME_CALORIES)),
                    cursor.getInt(cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME_DAYOFTHEWEEK)),
                    cursor.getString(cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME_PARTOFTHEDAY)),
                    cursor.getInt(cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME_USERINPUT)));
            properties.add(property);
        }while(cursor.moveToNext());

        return properties;
    }

    public Cursor ReadDataFromDatabase()
    {
        SQLiteDatabase db = this.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                FeedReaderContract.FeedEntry._ID,
                FeedReaderContract.FeedEntry.COLUMN_NAME_GENDER,
                FeedReaderContract.FeedEntry.COLUMN_NAME_AGE,
                FeedReaderContract.FeedEntry.COLUMN_NAME_STEPSNUM,
                FeedReaderContract.FeedEntry.COLUMN_NAME_STILL,
                FeedReaderContract.FeedEntry.COLUMN_NAME_CONTINUOUSSTILL,
                FeedReaderContract.FeedEntry.COLUMN_NAME_RUNNING,
                FeedReaderContract.FeedEntry.COLUMN_NAME_DRIVING,
                FeedReaderContract.FeedEntry.COLUMN_NAME_CYCLING,
                FeedReaderContract.FeedEntry.COLUMN_NAME_SLEEPING,
                FeedReaderContract.FeedEntry.COLUMN_NAME_WEATHER,
                FeedReaderContract.FeedEntry.COLUMN_NAME_TARGETWEIGHT,
                FeedReaderContract.FeedEntry.COLUMN_NAME_CALORIES,
                FeedReaderContract.FeedEntry.COLUMN_NAME_DAYOFTHEWEEK,
                FeedReaderContract.FeedEntry.COLUMN_NAME_PARTOFTHEDAY,
                FeedReaderContract.FeedEntry.COLUMN_NAME_USERINPUT
        };

        Cursor cursor = db.query(FeedReaderContract.FeedEntry.TABLE_NAME,projection, null, null, null, null, null);
        return cursor;
    }

    public void DeleteDataFromDatabase()
    {
        SQLiteDatabase db = this.getReadableDatabase();

        db.delete(FeedReaderContract.FeedEntry.TABLE_NAME, null, null);
    }


}
