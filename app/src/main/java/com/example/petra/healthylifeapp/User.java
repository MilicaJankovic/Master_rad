package com.example.petra.healthylifeapp;

import java.util.function.DoubleUnaryOperator;

/**
 * Created by Petra on 2/11/2018.
 */

public class User {

    public String username;
    public String email;
    public String gender;
    public Double heigh;
    public Double weight;
    public String achivement;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(com.example.petra.healthylifeapp.User.class)
    }

    public User(String username, String email, String gender, Double height, Double weight, String achivement) {
        this.username = username;
        this.email = email;
        this.gender = gender;
        this.heigh = height;
        this. weight = weight;
        this.achivement = achivement;

    }

}