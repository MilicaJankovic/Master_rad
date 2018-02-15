package com.example.petra.healthylifeapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CalendarView;
import android.widget.TextView;

/**
 * Created by Milica on 2/15/2018.
 */

public class CalendarActivity extends AppCompatActivity {

    private  static final String TAG = "CalendarActivity";
    private CalendarView mCalendarView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get current number of steps
        Bundle b = getIntent().getExtras();
        String value = "";
        if(b != null)
            value = b.getString("steps");


        setContentView(R.layout.activity_calendar);
        mCalendarView = (CalendarView) findViewById(R.id.calendarView);

        final String finalValue = value;
        if(finalValue != "")
        {
            TextView textviewDate = (TextView) findViewById(R.id.date);
            CaloriesCalculator calculator = new CaloriesCalculator(90, 184, Double.valueOf(finalValue));
            textviewDate.setText(String.valueOf(calculator.CalculateCaloriesBurnedBySteps()));
        }
        mCalendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView CalendarView, int year, int month, int dayOfMonth) {
                String date = year + "/" + month + "/"+ dayOfMonth ;
                Log.d(TAG, "onSelectedDayChange: yyyy/mm/dd:" + date);
//                TextView textviewDate = (TextView) findViewById(R.id.date);
//                textviewDate.setText(date);


            }
        });
    }
}
