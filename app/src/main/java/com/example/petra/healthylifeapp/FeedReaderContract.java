package com.example.petra.healthylifeapp;

import android.provider.BaseColumns;

/**
 * Created by Milica on 3/11/2018.
 */

public class FeedReaderContract {

    private FeedReaderContract() {}

    /* Inner class that defines the table contents */
    public static class FeedEntry implements BaseColumns {

        public static final String TABLE_NAME = "UserActivityProperties";
        public static final String COLUMN_NAME_GENDER = "Gender";
        public static final String COLUMN_NAME_AGE = "Age";
        public static final String COLUMN_NAME_STEPSNUM = "StepsNum";
        public static final String COLUMN_NAME_STILL = "Still";
        public static final String COLUMN_NAME_CONTINUOUSSTILL = "ContinuousStill";
        public static final String COLUMN_NAME_RUNNING = "Running";
        public static final String COLUMN_NAME_DRIVING = "Driving";
        public static final String COLUMN_NAME_CYCLING = "Cycling";
        public static final String COLUMN_NAME_SLEEPING = "Sleeping";
        public static final String COLUMN_NAME_WEATHER = "Weaher";
        public static final String COLUMN_NAME_TARGETWEIGHT = "TargetWeight";
        public static final String COLUMN_NAME_CALORIES = "Calories";
        public static final String COLUMN_NAME_DAYOFTHEWEEK = "DayOfTheWeek";
        public static final String COLUMN_NAME_PARTOFTHEDAY = "PartOfTheDay";
        public static final String COLUMN_NAME_NOTIFICATIONTYPE = "NotificationType";
        public static final String COLUMN_NAME_USERINPUT = "UserInput";


    }
}
