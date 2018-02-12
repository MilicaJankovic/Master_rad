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

public class ConfigureProfile extends AppCompatActivity implements View.OnClickListener {

    //firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;


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

        if(getUser() == null)
        {
            Intent intent = new Intent(this, FirebaseLogin.class);
            startActivity(intent);
        }
        else
        {
            parseUserDetails(getUser());
        }
    }

    private void parseUserDetails(FirebaseUser user)
    {
        TextView txtUsername = (TextView) findViewById(R.id.txtUsername);

        txtUsername.setText(user.getDisplayName());
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

        RadioGroup groupGender = (RadioGroup) findViewById(R.id.radiogroupGender);
        RadioButton btnGender = (RadioButton) findViewById(groupGender.getCheckedRadioButtonId());

        RadioGroup groupAchivement = (RadioGroup) findViewById(R.id.radiogroupAchivement);
        RadioButton btnAchivement = (RadioButton) findViewById(groupAchivement.getCheckedRadioButtonId());

        User user = new User(txtUsername.getText().toString(), firebaseUser.getEmail(), btnGender.getText().toString(), Double.parseDouble(txtHeight.getText().toString()), Double.parseDouble(txtWeight.getText().toString()), btnAchivement.getText().toString());
        mDatabase.child("users").child(firebaseUser.getUid()).setValue(user);
        mDatabase.push();

    }

    ValueEventListener userListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Get Post object and use the values to update the UI
            User user = dataSnapshot.getValue(User.class);
            // ...
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            Log.w("Canceled", "loadPost:onCancelled", databaseError.toException());
            // ...
        }
    };

    private void FindUser(View view) {
        FirebaseUser firebaseUser = getUser();

        if (firebaseUser != null) {
            String child = "email";
            String email = firebaseUser.getEmail();
            Query query = mDatabase.orderByChild(child).equalTo(email);
            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                        // TODO: handle the post here
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Getting Post failed, log a message
                    Log.w("Canceled: ", "loadPost:onCancelled", databaseError.toException());
                    // ...
                }
            });
        }

    }
}
