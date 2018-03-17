package com.example.petra.healthylifeapp.Helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.petra.healthylifeapp.DBMaintenance.FirebaseUtility;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;

import static android.content.Context.MODE_PRIVATE;
import static com.example.petra.healthylifeapp.MainActivity.ExerciseTag;

/**
 * Created by Milica on 2/15/2018.
 */

public class CaloriesCalculator {

    // Fill with your data

    Context context;
    static double weight;

    static double height;

    static double stepsCount;


    final static double walkingFactor = 0.57;

    static double CaloriesBurnedPerMile;

    static double strip;

    static double stepCountMile; // step/mile

    static double conversationFactor;

    static double CaloriesBurned;

    static NumberFormat formatter = new DecimalFormat("#0.00");

    static double distance;



    public static final String squats = "Squats";
    public static final String jumpingJack = "JumpingJack";
    public static final String mountainClimbers = "MountainClimbers";
    public static final String burpees = "Burpees";
    public static final String pushups = "Pushups";
    public static final String situps = "Situps";
    public static final String numberSteps = "NumberOfSteps";
    public static final int numberOfExercises = 6;

    public CaloriesCalculator(Context cont,double personWeight, double personHeight/*, double passedSteps*/) {
        this.context = cont;
        this.weight = personWeight;
        this.height = personHeight;
      //  this.stepsCount = passedSteps;
    }

    public CaloriesCalculator(Context cont) {
        this.context = cont;
        this.weight = 0;
        this.height = 0;
        this.stepsCount = 0;

    }

    public double CalculateCaloriesBurnedBySteps()
    {
        stepsCount = GetNumberOfSteps();

        CaloriesBurnedPerMile = walkingFactor * (weight * 2.2);

        strip = height * 0.415;

        stepCountMile = 160934.4 / strip;

        conversationFactor = CaloriesBurnedPerMile / stepCountMile;

        CaloriesBurned = stepsCount * conversationFactor;

        System.out.println("Calories burned: "
                + formatter.format(CaloriesBurned) + " cal");

        distance = (stepsCount * strip) / 100000;

        System.out.println("Distance: " + formatter.format(distance)
                + " km");
        return CaloriesBurned;

    }

    public double CalculateCaloriesBurnedByDoingExercises()
    {
        double squatsCalories = 0;
        double jumpingCalories = 0;
        double mountainCalories = 0;
        double barpeesCalories = 0;
        double pushupsCalories = 0;
        double situpsCalories = 0;

        if(GetSharedPreferencesForExercise(squats, context))
            squatsCalories = weight * 0.96 * 1;

        if(GetSharedPreferencesForExercise(jumpingJack, context))
            jumpingCalories = weight * 0.75 * 1;

        if(GetSharedPreferencesForExercise(mountainClimbers, context))
            mountainCalories = weight * 0.94 * 1;

        if(GetSharedPreferencesForExercise(burpees, context))
            barpeesCalories = weight * 0.75 * 1;

        if(GetSharedPreferencesForExercise(pushups, context))
            pushupsCalories = weight * 0.70 * 1;

        if(GetSharedPreferencesForExercise(situps, context))
            situpsCalories = weight * 0.45 * 1;

        return squatsCalories + jumpingCalories + mountainCalories + barpeesCalories + pushupsCalories + situpsCalories;

    }


    public void SetNumberOfSteps(long number, Context con)
    {
        SharedPreferences.Editor editor = con.getSharedPreferences(ExerciseTag, Context.MODE_PRIVATE).edit();
        editor.putLong(CaloriesCalculator.numberSteps, number);
        editor.apply();
    }

    public long GetNumberOfSteps()
    {
        SharedPreferences pref = context.getSharedPreferences(ExerciseTag, MODE_PRIVATE);
        long value = pref.getLong(CaloriesCalculator.numberSteps, 0);
        return value;
    }

    public void SetSharedPreferencesForExcersise(String name, Boolean value, Context con)
    {
        SharedPreferences.Editor editor = con.getSharedPreferences(ExerciseTag, MODE_PRIVATE).edit();
        editor.putBoolean(name, value);
        editor.apply();
    }
    public boolean GetSharedPreferencesForExercise(String name, Context con)
    {
        SharedPreferences pref = con.getSharedPreferences(ExerciseTag, MODE_PRIVATE);
        boolean value = pref.getBoolean(name, false);
        return value;
    }

    public void ResetSharedPreferences()
    {
        SetSharedPreferencesForExcersise(squats, false, context);
        SetSharedPreferencesForExcersise(jumpingJack, false, context);
        SetSharedPreferencesForExcersise(mountainClimbers, false, context);
        SetSharedPreferencesForExcersise(burpees, false, context);
        SetSharedPreferencesForExcersise(pushups, false, context);
        SetSharedPreferencesForExcersise(situps, false, context);
    }


    public int checkExcersises()
    {
        int exercises = 0;

        if(GetSharedPreferencesForExercise(squats, context))
            exercises++;

        if(GetSharedPreferencesForExercise(jumpingJack, context))
            exercises++;

        if(GetSharedPreferencesForExercise(mountainClimbers, context))
            exercises++;

        if(GetSharedPreferencesForExercise(burpees, context))
            exercises++;

        if(GetSharedPreferencesForExercise(pushups, context))
            exercises++;

        if(GetSharedPreferencesForExercise(situps, context))
            exercises++;

        return exercises;
    }

}
