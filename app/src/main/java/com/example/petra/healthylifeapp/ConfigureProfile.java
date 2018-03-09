package com.example.petra.healthylifeapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

public class ConfigureProfile extends AppCompatActivity implements View.OnClickListener {

    //firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private ArrayList<String> userLocations;
    private HashMap<String, Double> userCalories;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_profile);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        Button btnSave = (Button) findViewById(R.id.btnSave);
        btnSave.setOnClickListener(this);

        if (getUser() == null) {
            Intent intent = new Intent(this, FirebaseLogin.class);
            startActivity(intent);
        } else {
//            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            mDatabase = FirebaseDatabase.getInstance().getReference();
            mDatabase.keepSynced(true);
            if (mDatabase != null) {
                mDatabase.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        parseUserDetails(dataSnapshot);
                        userLocations = FirebaseUtility.getUserLocations(dataSnapshot);
                        userCalories = FirebaseUtility.getUserCalories(dataSnapshot);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w("Canceled", "loadPost:onCancelled", databaseError.toException());
                        // ...
                    }
                });
            }
        }
    }

    private void parseUserDetails(DataSnapshot dataSnapshot) {
        TextView txtUsername = (TextView) findViewById(R.id.txtUsername);
        TextView txtHeight = (TextView) findViewById(R.id.txtHeight);
        TextView txtWeight = (TextView) findViewById(R.id.txtWeight);
        TextView txtBirthDate = (TextView) findViewById(R.id.txtBirthDate);

        for (DataSnapshot ds : dataSnapshot.getChildren()) {
            String UserID = getUser().getUid();
//            User user = new User();
            HashMap<String, Object> user;

            if (ds.child(UserID).getValue() != null) {
                user = (HashMap<String, Object>) ds.child(UserID).getValue();

                txtUsername.setText(user.get("username").toString());
                txtHeight.setText(user.get("height").toString());
                txtWeight.setText(user.get("weight").toString());
                txtBirthDate.setText(user.get("birthDate").toString());

                RadioButton button = new RadioButton(this);

                switch (user.get("achivement").toString()) {
                    case "Keep Weight":
                        button = (RadioButton) findViewById(R.id.radioKeepWeight);
                        break;
                    case "Lose Weight":
                        button = (RadioButton) findViewById(R.id.radioLoseWeight);
                        break;
                    case "Gain Weight":
                        button = (RadioButton) findViewById(R.id.radioGainWeight);
                        break;
                }

                button.setChecked(true);

                RadioButton button1 = new RadioButton(this);
                switch (user.get("gender").toString()) {
                    case "Male":
                        button1 = (RadioButton) findViewById(R.id.radioMale);
                        break;
                    case "Female":
                        button1 = (RadioButton) findViewById(R.id.radioFemale);
                        break;
                }

                button1.setChecked(true);

//                RadioButton button2 = new RadioButton(this);
//                Long stepsGoal = Long.getLong(user.get("stepsGoal").toString());
//                switch (Integer.getInteger(stepsGoal.toString())) {
//                    case 5000:
//                        button2 = (RadioButton) findViewById(R.id.radio5000);
//                        break;
//                    case 10000:
//                        button2 = (RadioButton) findViewById(R.id.radio10000);
//                        break;
//                    case 15000:
//                        button2 = (RadioButton) findViewById(R.id.radio15000);
//                        break;
//                    default:
//                        button2 = (RadioButton) findViewById(R.id.radio10000);
//                        break;
//                }
//                    button2.setChecked(true);

            } else {
                //10000 by default
                RadioButton button3 = (RadioButton) findViewById(R.id.radio10000);
                button3.setChecked(true);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //return super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.action_buttons, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //return super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.googleSetup:
                // User chose the "google setup in FirebaseLogin" item, show the app settings UI...
                Intent firebaseLogin = new Intent(this, FirebaseLogin.class);
                startActivity(firebaseLogin);
                return true;

            case R.id.configureProfile:
                // User chose the "configure profile, this page" action, mark the current item
                // as a favorite...
                Intent configure = new Intent(this, ConfigureProfile.class);
                startActivity(configure);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.btnSave) {
            saveUser();

            Toast toast = Toast.makeText(this, "You successfully configured your user profile!", Toast.LENGTH_SHORT);
            toast.show();

            Intent mainIntent = new Intent(this, MainActivity.class);
            startActivity(mainIntent);
        }
    }


    private FirebaseUser getUser() {
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        return currentUser;
    }


    private void saveUser() {
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            mDatabase = FirebaseDatabase.getInstance().getReference();
            if (mDatabase != null) {
                writeNewUser(currentUser);
            }
        }
    }

    private void writeNewUser(FirebaseUser firebaseUser) {
        TextView txtUsername = (TextView) findViewById(R.id.txtUsername);
        TextView txtHeight = (TextView) findViewById(R.id.txtHeight);
        TextView txtWeight = (TextView) findViewById(R.id.txtWeight);
        TextView txtBirthDate = (TextView) findViewById(R.id.txtBirthDate);

        RadioGroup groupGender = (RadioGroup) findViewById(R.id.radiogroupGender);
        RadioButton btnGender = (RadioButton) findViewById(groupGender.getCheckedRadioButtonId());

        RadioGroup groupAchivement = (RadioGroup) findViewById(R.id.radiogroupAchivement);
        RadioButton btnAchivement = (RadioButton) findViewById(groupAchivement.getCheckedRadioButtonId());

        RadioGroup groupStepsGoal = (RadioGroup) findViewById(R.id.radiogroupStepsGoal);
        RadioButton btnSteps = (RadioButton) findViewById(groupStepsGoal.getCheckedRadioButtonId());

//        ArrayList<String> locations = userLocations;
//        locations.add("13.45|11.46");

        User user = new User(txtUsername.getText().toString(), firebaseUser.getEmail(), btnGender.getText().toString(), Double.parseDouble(txtHeight.getText().toString()), Double.parseDouble(txtWeight.getText().toString()), btnAchivement.getText().toString(), userLocations, Integer.parseInt(btnSteps.getText().toString()), (HashMap<String, Double>) userCalories, txtBirthDate.getText().toString(), 0L);
        mDatabase.child("users").child(firebaseUser.getUid()).setValue(user);
        mDatabase.push();

    }
}
