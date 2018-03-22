package com.example.petra.healthylifeapp;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CalendarView;
import android.widget.TextView;

import com.example.petra.healthylifeapp.DBMaintenance.FirebaseUtility;
import com.example.petra.healthylifeapp.Helpers.CaloriesCalculator;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

/**
 * Created by Milica on 2/15/2018.
 */

public class CalendarActivity extends AppCompatActivity {

    private  static final String TAG = "CalendarActivity";
    private CalendarView mCalendarView;

    private DatabaseReference mDatabase;
    private HashMap<String, Double> userCalories;

    String userWeight = "";
    String userHeight = "";
    Context con;
    CaloriesCalculator calculator;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get current number of steps
        Bundle b = getIntent().getExtras();
        String value = "";
        if(b != null) {
            value = b.getString("steps");
 //           userWeight = b.getString("weight");
 //           userHeight = b.getString("height");
        }
        final String finalValue = value;
        con = this;
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.keepSynced(true);
        if (mDatabase != null) {
            mDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    userCalories = FirebaseUtility.getUserCalories(dataSnapshot);
//                    userAge = FirebaseUtility.CalculateAge(dataSnapshot);
//
//                    stepsGoal = Integer.parseInt(FirebaseUtility.getUserProperty(dataSnapshot, "stepsGoal"));
//                    userGender = FirebaseUtility.getUserProperty(dataSnapshot, "gender");
//                    userSleep = Integer.parseInt(FirebaseUtility.getUserProperty(dataSnapshot, "sleep"));
//                    targetWeight = Integer.parseInt(FirebaseUtility.getUserProperty(dataSnapshot, "weight"));
//
                    userWeight = FirebaseUtility.getUserProperty(dataSnapshot, "weight");
                    userHeight = FirebaseUtility.getUserProperty(dataSnapshot, "height");

                    if(!userWeight.equals("") && !userHeight.equals(""))
                        calculator = new CaloriesCalculator(con, Double.valueOf(userWeight), Double.valueOf(userHeight));
                    else
                        calculator = new CaloriesCalculator(con, 1, 1);


                    if(finalValue!= "")
                    {
                        TextView textviewCalories = (TextView) findViewById(R.id.calories);
                        // CaloriesCalculator calculator = new CaloriesCalculator(this, 90, 184 /*, Double.valueOf(finalValue)*/);
                        calculator.SetNumberOfSteps(Long.valueOf(finalValue), con);
                        int calories = (int) (calculator.CalculateCaloriesBurnedBySteps() + calculator.CalculateCaloriesBurnedByDoingExercises());
                        //String value = String.valueOf();


                        textviewCalories.setText(String.valueOf(calories));

                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w("Canceled", "loadPost:onCancelled", databaseError.toException());
                    // ...
                }
            });
        }

        setContentView(R.layout.activity_calendar);
        mCalendarView = (CalendarView) findViewById(R.id.calendarView);


        mCalendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView CalendarView, int year, int month, int dayOfMonth) {

                if(userCalories != null && userCalories.size() > 0)
                {
                    month++;
                    String monthFormat = String.valueOf(month);
                    String dayFormat = String.valueOf(dayOfMonth);

                    if(monthFormat.length() == 1)
                    {
                        monthFormat = "0" + monthFormat;
                    }
                    if(dayFormat.length() == 1)
                    {
                        dayFormat = "0" + dayFormat;
                    }
                    String dateFormat = dayFormat + "-" + monthFormat + "-" + String.valueOf(year);

                    if(userCalories.containsKey(dateFormat))
                    {
                        TextView textviewCalories = (TextView) findViewById(R.id.calories);

                        textviewCalories.setText(String.valueOf(userCalories.get(dateFormat).intValue()));
                    }
                }
                }



        });
    }
}
