package com.example.petra.healthylifeapp;

import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
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

    public static void SaveTime(String childName)
    {
        FirebaseUser currentUser = FirebaseUtility.getUser();
        if(currentUser != null) {
            mDatabase = FirebaseDatabase.getInstance().getReference();
            if (mDatabase != null) {
                Timestamp time  = new Timestamp(System.currentTimeMillis());
                mDatabase.child("users").child(currentUser.getUid()).child(childName).setValue(time.toString());
                mDatabase.push();
            }
        }
    }

    public static String getUserProperty(DataSnapshot dataSnapshot, String propertyName)
    {
        String property = "";

        for(DataSnapshot ds: dataSnapshot.getChildren() ) {
            String UserID = FirebaseUtility.getUser().getUid();

            HashMap<String, Object> user = null;
            if (ds.child(UserID).getValue() != null) {
                user = (HashMap<String, Object>)ds.child(UserID).getValue();
            }

            if(user.get(propertyName) != null) {
                property = user.get(propertyName).toString();
            }
        }

        return property;
    }


    public static ArrayList<String> getUserLocations(DataSnapshot dataSnapshot)
    {
        ArrayList<String> userLocs = null;
        for(DataSnapshot ds: dataSnapshot.getChildren() ) {
            String UserID = FirebaseUtility.getUser().getUid();

            HashMap<String, Object> user = null;
            if (ds.child(UserID).getValue() != null) {
                user = (HashMap<String, Object>)ds.child(UserID).getValue();
            }

            if(user.get("locations") != null) {
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

    public static void SaveUserLocation(String newLocation, ArrayList<String> userLocations)
    {
        ArrayList<String> locations;
        if(userLocations != null) {
            locations = userLocations;
        }
        else{
            locations = new ArrayList<String>();
        }

        locations.add(newLocation);

        FirebaseUser currentUser = getUser();

        if(currentUser != null) {
            mDatabase = FirebaseDatabase.getInstance().getReference();
            if (mDatabase != null) {
                mDatabase.child("users").child(currentUser.getUid()).child("locations").setValue(locations);
                mDatabase.push();
            }
        }
    }

    public static void ResetUserLocations()
    {
        ArrayList<String> locations = new ArrayList<>();


        FirebaseUser currentUser = getUser();

        if(currentUser != null) {
            mDatabase = FirebaseDatabase.getInstance().getReference();
            if (mDatabase != null) {
                mDatabase.child("users").child(currentUser.getUid()).child("locations").setValue(locations);
                mDatabase.push();
            }
        }
    }
}
