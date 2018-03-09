package com.example.petra.healthylifeapp;

import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;

import com.google.android.gms.vision.barcode.Barcode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Petra on 2/24/2018.
 */

public class FirebaseUtility {

    private static FirebaseAuth mAuth;
    private static DatabaseReference mDatabase;

    public static FirebaseUser getUser() {
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        return currentUser;
    }

    public static void SaveTime(String childName) {
        FirebaseUser currentUser = FirebaseUtility.getUser();
        if (currentUser != null) {
            mDatabase = FirebaseDatabase.getInstance().getReference();
            if (mDatabase != null) {
                Timestamp time = new Timestamp(System.currentTimeMillis());
                mDatabase.child("users").child(currentUser.getUid()).child(childName).setValue(time.toString());
                mDatabase.push();
            }
        }
    }

    public static String getUserProperty(DataSnapshot dataSnapshot, String propertyName) {
        String property = "";

        for (DataSnapshot ds : dataSnapshot.getChildren()) {
            String UserID = FirebaseUtility.getUser().getUid();

            HashMap<String, Object> user = null;
            if (ds.child(UserID).getValue() != null) {
                user = (HashMap<String, Object>) ds.child(UserID).getValue();
            }

            if (user.get(propertyName) != null) {
                property = user.get(propertyName).toString();
            }
        }

        return property;
    }


    public static int CalculateAge(DataSnapshot dataSnapshot) {
        int age = 0;

        String birthDate = getUserProperty(dataSnapshot, "birthDate");

        DateFormat format = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
        Date date = null;
        try {
            date = format.parse(birthDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (date != null) {
            Date today = new Date();
            age = today.getYear() - date.getYear();
        }

        return age;
    }

    public static HashMap<String, Double> getUserCalories(DataSnapshot dataSnapshot) {
        HashMap<String, Double> calories = null;
        for (DataSnapshot ds : dataSnapshot.getChildren()) {
            String UserID = FirebaseUtility.getUser().getUid();

            HashMap<String, Object> user = null;
            if (ds.child(UserID).getValue() != null) {
                user = (HashMap<String, Object>) ds.child(UserID).getValue();
            }

            if (user.get("calories") != null) {
                //calories = new HashMap<>();
                calories = (HashMap<String, Double>) user.get("calories");
            }
        }

        return calories;
    }


    public static ArrayList<String> getUserLocations(DataSnapshot dataSnapshot) {
        ArrayList<String> userLocs = null;
        for (DataSnapshot ds : dataSnapshot.getChildren()) {
            String UserID = FirebaseUtility.getUser().getUid();

            HashMap<String, Object> user = null;
            if (ds.child(UserID).getValue() != null) {
                user = (HashMap<String, Object>) ds.child(UserID).getValue();
            }

            if (user.get("locations") != null) {
                userLocs = new ArrayList<>();
                //String lala = user.get("locations").toString();
//                Map<String, String> locations = new HashMap<>();
//                for (String key : locations.keySet()) {
//                    String value = locations.get(key);
//                    System.out.println("Key = " + key + ", Value = " + value);
//                    userLocs.add(value);
//                }

                userLocs = (ArrayList<String>) user.get("locations");
            }
        }

        return userLocs;
    }

    public static void SaveUserLocation(String newLocation, ArrayList<String> userLocations) {
        ArrayList<String> locations;
        if (userLocations != null) {
            locations = userLocations;
        } else {
            locations = new ArrayList<String>();
        }

        locations.add(newLocation);

        FirebaseUser currentUser = getUser();

        if (currentUser != null) {
            mDatabase = FirebaseDatabase.getInstance().getReference();
            if (mDatabase != null) {
                mDatabase.child("users").child(currentUser.getUid()).child("locations").setValue(locations);
                mDatabase.push();
            }
        }
    }

    public static void saveUserCaolries(String date, Double newCalorieCalc, HashMap<String, Double> userCalories) {
        HashMap<String, Double> calories;
        if (userCalories != null) {
            calories = userCalories;
        } else {
            calories = new HashMap<>();
        }

        //String date = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        calories.put(date, newCalorieCalc);

        FirebaseUser currentUser = getUser();

        if (currentUser != null) {
            mDatabase = FirebaseDatabase.getInstance().getReference();
            if (mDatabase != null) {
                mDatabase.child("users").child(currentUser.getUid()).child("calories").setValue(calories);
                mDatabase.push();
            }
        }
    }

    public static void saveUserProperty(String property, String propertyName) {
        FirebaseUser currentUser = getUser();

        if (currentUser != null) {
            mDatabase = FirebaseDatabase.getInstance().getReference();
            if (mDatabase != null) {
                mDatabase.child("users").child(currentUser.getUid()).child(propertyName).setValue(property);
                mDatabase.push();
            }
        }
    }


    public static void ResetUserLocations() {
        ArrayList<String> locations = new ArrayList<>();


        FirebaseUser currentUser = getUser();

        if (currentUser != null) {
            mDatabase = FirebaseDatabase.getInstance().getReference();
            if (mDatabase != null) {
                mDatabase.child("users").child(currentUser.getUid()).child("locations").setValue(locations);
                mDatabase.push();
            }
        }
    }

    public static String getPartOfTheDay()
    {
        String partoftheday = "";

        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY); //Current hour

        if(currentHour > 6 && currentHour < 9)
        {
            partoftheday = "Morning";
        }
        else if(currentHour > 9 && currentHour < 12)
        {
            partoftheday = "Noon";
        }
        else if(currentHour > 12 && currentHour < 18)
        {
            partoftheday = "Afternoon";
        }
        else if(currentHour > 18 && currentHour < 21)
        {
            partoftheday = "Evening";
        }
        else{
            partoftheday = "Night";
        }

        return partoftheday;
    }
}
