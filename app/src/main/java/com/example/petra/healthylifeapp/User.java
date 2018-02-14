package com.example.petra.healthylifeapp;

import java.util.function.DoubleUnaryOperator;

/**
 * Created by Petra on 2/11/2018.
 */

public class User {

    public String username;
    public String email;
    public String gender;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Double getHeigh() {
        return heigh;
    }

    public void setHeigh(Double heigh) {
        this.heigh = heigh;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public String getAchivement() {
        return achivement;
    }

    public void setAchivement(String achivement) {
        this.achivement = achivement;
    }

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