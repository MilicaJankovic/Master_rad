package com.example.petra.healthylifeapp;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by Milica on 3/7/2018.
 */

public class UserActivityProperties {

    private String Gender;
    private int Age;
    private int StepsNum;
    private int Still;
    private int ContinuousStill;
    private int Running;
    private int Driving;
    private int Cycling;
    private int Sleeping;
    private String Weather;
    private int TargetWeight;
    private int Calories;
    private int DayOfTheWeek;
    private String PartOfTheDay;
    private int UserInput;


    public UserActivityProperties()
    {
        setGender("");
        setAge(0);
        setStepsNum(0);
        setStill(0);
        setContinuousStill(0);
        setRunning(0);
        setDriving(0);
        setCycling(0);
        setSleeping(0);
        setWeather("");
        setTargetWeight(0);
        setCalories(0);
        setDayOfTheWeek(0);
        setPartOfTheDay("");
        setUserInput(0);

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


    public UserActivityProperties SetProperties(String gender, int age, int steps, int still, int contStill, int running, int driving, int cycling,  int sleeping, String weather, int target, int cal, int day, String part, int input)
    {
        UserActivityProperties properties = new UserActivityProperties();


        properties.setGender(gender);
        properties.setAge(age);
        properties.setStepsNum(steps);
        properties.setStill(still);
        properties.setContinuousStill(contStill);
        properties.setRunning(running);
        properties.setDriving(driving);
        properties.setCycling(cycling);
        properties.setSleeping(sleeping);
        properties.setWeather(weather);
        properties.setTargetWeight(target);
        properties.setDayOfTheWeek(day);
        properties.setPartOfTheDay(part);
        properties.setUserInput(input);


        return properties;
    }

    public UserActivityProperties(String gender, int age, int steps, int still, int contStill, int running, int driving, int cycling,  int sleeping, String weather, int target, int cal, int day, String part, int input)
    {
        setGender(gender);
        setAge(age);
        setStepsNum(steps);
        setStill(still);
        setContinuousStill(contStill);
        setRunning(running);
        setDriving(driving);
        setCycling(cycling);
        setSleeping(sleeping);
        setWeather(weather);
        setTargetWeight(target);
        setDayOfTheWeek(day);
        setPartOfTheDay(part);
        setUserInput(input);
    }

    public int getAge() {
        return Age;
    }

    public void setAge(int age) {
        Age = age;
    }

    public int getContinuousStill() {
        return ContinuousStill;
    }

    public void setContinuousStill(int continuousStill) {
        ContinuousStill = continuousStill;
    }

    public int getCycling() {
        return Cycling;
    }

    public void setCycling(int cycling) {
        Cycling = cycling;
    }

    public int getTargetWeight() {
        return TargetWeight;
    }

    public void setTargetWeight(int targetWeight) {
        TargetWeight = targetWeight;
    }

    public int getCalories() {
        return Calories;
    }

    public void setCalories(int calories) {
        Calories = calories;
    }

    public int getDayOfTheWeek() {
        return DayOfTheWeek;
    }

    public void setDayOfTheWeek(int dayOfTheWeek) {
        DayOfTheWeek = dayOfTheWeek;
    }


    public int getUserInput() {
        return UserInput;
    }

    public void setUserInput(int userInput) {
        UserInput = userInput;
    }

    public String getWeather() {
        return Weather;
    }

    public void setWeather(String weather) {
        Weather = weather;
    }

    public String getGender() {
        return Gender;
    }

    public void setGender(String gender) {
        Gender = gender;
    }

    public String getPartOfTheDay() {
        return PartOfTheDay;
    }

    public void setPartOfTheDay(String partOfTheDay) {
        PartOfTheDay = partOfTheDay;
    }
}


////toDO
//napraviti f-ju za poziv metoda za skladistenje podataka
//update kolona u tabeli
//napraviti f-ju za racunanje faktora
//resetovati checkboxeve u 12 sati i shared preferences