package com.example.petra.healthylifeapp;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by Milica on 3/7/2018.
 */

public class UserActivityProperties {

    private int Gender;
    private Date BirthDate;
    private int StepsNum;
    private int Running;
    private int Still;
    private int Driving;
    private int Sleeping;
    private int Weather;
    private int LookingGoals;
    private int Factor;

    public UserActivityProperties()
    {
        setGender(0);
        setBirthDate(null);/********!!!!!!!!!!!!!********/
        setStepsNum(0);
        setRunning(0);
        setStill(0);
        setDriving(0);
        setSleeping(0);
        setWeather(0);
        setLookingGoals(0);
        setFactor(0);
    }
    public int getGender() {
        return Gender;
    }

    public void setGender(int gender) {
        Gender = gender;
    }

    public Date getBirthDate() {
        return BirthDate;
    }

    public void setBirthDate(Date birthDate) {
        BirthDate = birthDate;
    }

    public int getStepsNum() {
        return StepsNum;
    }

    public void setStepsNum(int stepsNum) {
        StepsNum = stepsNum;
    }

    public int getRunning() {
        return Running;
    }

    public void setRunning(int running) {
        Running = running;
    }

    public int getStill() {
        return Still;
    }

    public void setStill(int still) {
        Still = still;
    }

    public int getDriving() {
        return Driving;
    }

    public void setDriving(int driving) {
        Driving = driving;
    }

    public int getSleeping() {
        return Sleeping;
    }

    public void setSleeping(int sleeping) {
        Sleeping = sleeping;
    }

    public int getWeather() {
        return Weather;
    }

    public void setWeather(int weather) {
        Weather = weather;
    }

    public int getLookingGoals() {
        return LookingGoals;
    }

    public void setLookingGoals(int lookingGoals) {
        LookingGoals = lookingGoals;
    }

    public int getFactor() {
        return Factor;
    }

    public void setFactor(int factor) {
        Factor = factor;
    }

    public UserActivityProperties SetProperties(int gender, Date date, int steps, int still, int running, int driving, int sleeping, int weather, int looking)
    {
        UserActivityProperties properties = new UserActivityProperties();


        properties.setGender(gender);
        properties.setBirthDate(date);
        properties.setStepsNum(steps);
        properties.setRunning(running);
        properties.setStill(still);
        properties.setDriving(driving);
        properties.setSleeping(sleeping);
        properties.setWeather(weather);
        properties.setLookingGoals(looking);
        properties.setFactor(CalculateFactor(properties));

        return properties;
    }

    public int CalculateFactor(UserActivityProperties properties)
    {
        return 0;
    }

    public UserActivityProperties(int gender, Date date, int steps, int still, int running, int driving, int sleeping, int weather, int looking, int factor)
    {
        setGender(gender);
        setBirthDate(date);
        setStepsNum(steps);
        setRunning(running);
        setStill(still);
        setDriving(driving);
        setSleeping(sleeping);
        setWeather(weather);
        setLookingGoals(looking);
        setFactor(factor);
    }
}


////toDO
//napraviti f-ju za poziv metoda za skladistenje podataka
//update kolona u tabeli
//napraviti f-ju za racunanje faktora