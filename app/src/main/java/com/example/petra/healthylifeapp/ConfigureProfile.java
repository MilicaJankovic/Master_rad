package com.example.petra.healthylifeapp;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ConfigureProfile extends AppCompatActivity implements View.OnClickListener {

    //firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_profile);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.btnSave) {

        }
    }

    private void saveUser()
    {
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser != null)
        {
            mDatabase = FirebaseDatabase.getInstance().getReference();
            if(mDatabase != null)
            {
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

        RadioGroup groupAchivement = (RadioGroup) findViewById(R.id.radiogroupGender);
        RadioButton gtnAchivement = (RadioButton) findViewById(groupAchivement.getCheckedRadioButtonId());

        User user = new User(txtUsername.getText().toString(), firebaseUser.getEmail(), btnGender.getText().toString(), Double.parseDouble(txtHeight.getText().toString()), Double.parseDouble(txtWeight.getText().toString()));
        mDatabase.child("users").child(firebaseUser.getUid()).setValue(user);
        mDatabase.push();
    }
}
