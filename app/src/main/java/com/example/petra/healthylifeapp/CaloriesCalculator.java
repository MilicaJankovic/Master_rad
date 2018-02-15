package com.example.petra.healthylifeapp;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Created by Milica on 2/15/2018.
 */

public class CaloriesCalculator {

    // Fill with your data


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

    public CaloriesCalculator(double personWeight, double personHeight, double passedSteps) {
        this.weight = personWeight;
        this.height = personHeight;
        this.stepsCount = passedSteps;
    }

    public double CalculateCaloriesBurnedBySteps()
    {

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

}
