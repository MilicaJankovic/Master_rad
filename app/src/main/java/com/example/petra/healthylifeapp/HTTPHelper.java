package com.example.petra.healthylifeapp;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Milica on 3/7/2018.
 */

public class HTTPHelper {
    public static String SERVER_URL = "http://192.168.0.15:8080/";


    //insert one element to SQL database on server
    public static boolean setProperty(UserActivityProperties property)
    {
        JSONObject jsonObject = new JSONObject();
        boolean result = false;
        try {
            URL url = new URL(SERVER_URL +"property");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setReadTimeout(10000);
            conn.setConnectTimeout(30000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            String currentTime = getDateTime();

            jsonObject = new JSONObject();

            jsonObject.put("Gender", property.getGender());
            jsonObject.put("Age", property.getAge());
            jsonObject.put("StepsNum", property.getStepsNum());
            jsonObject.put("Still", property.getStill());
            jsonObject.put("ContinuousStill", property.getContinuousStill());
            jsonObject.put("Running", property.getRunning());
            jsonObject.put("Driving", property.getDriving());
            jsonObject.put("Cycling", property.getCycling());
            jsonObject.put("Sleeping", property.getSleeping());
            jsonObject.put("Weather", property.getWeather());
            jsonObject.put("TargetWeight", property.getTargetWeight());
            jsonObject.put("Calories", property.getCalories());
            jsonObject.put("DayOfTheWeek", property.getDayOfTheWeek());
            jsonObject.put("PartOfTheWeek", property.getPartOfTheDay());
            jsonObject.put("UserInput", property.getUserInput());

            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("property", jsonObject.toString());
            String query = builder.build().getEncodedQuery();
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(query);
            writer.flush();
            writer.close();
            os.close();
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                result = true;
            }
        }
        catch(Exception error)
        {
            error.printStackTrace();
            result = false;
        }

        return result;
    }

    //insert array of elements in sql database on server
    public static boolean setProperties(List<UserActivityProperties> properties)
    {
        JSONObject jsonObject;
        JSONArray jsonArray = new JSONArray();
        boolean result = false;
        try {
            URL url = new URL(SERVER_URL +"property");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            String currentTime = getDateTime();

            for(int i = 0; i<properties.size(); i++) {
                jsonObject = new JSONObject();
                jsonObject.put("Gender", properties.get(i).getGender());
                jsonObject.put("Age", properties.get(i).getAge());
                jsonObject.put("StepsNum", properties.get(i).getStepsNum());
                jsonObject.put("Still", properties.get(i).getStill());
                jsonObject.put("ContinuousStill", properties.get(i).getContinuousStill());
                jsonObject.put("Running", properties.get(i).getRunning());
                jsonObject.put("Driving", properties.get(i).getDriving());
                jsonObject.put("Cycling", properties.get(i).getCycling());
                jsonObject.put("Sleeping", properties.get(i).getSleeping());
                jsonObject.put("Weather", properties.get(i).getWeather());
                jsonObject.put("TargetWeight", properties.get(i).getTargetWeight());
                jsonObject.put("Calories", properties.get(i).getCalories());
                jsonObject.put("DayOfTheWeek", properties.get(i).getDayOfTheWeek());
                jsonObject.put("PartOfTheDay", properties.get(i).getPartOfTheDay());
                jsonObject.put("UserInput", properties.get(i).getUserInput());
                jsonArray.put(jsonObject);
            }
            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("property", jsonArray.toString());
            String query = builder.build().getEncodedQuery();
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(query);
            writer.flush();
            writer.close();
            os.close();
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                result = true;
            }
        }
        catch(Exception error)
        {
            error.printStackTrace();
            result = false;
        }

        return result;
    }

    private static String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    public static ArrayList<UserActivityProperties> getProperties() {
        ArrayList<UserActivityProperties> properties = new ArrayList<UserActivityProperties>();

        try{
            URL url = new URL(SERVER_URL + "getProperties");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Accept", "application/json");
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_OK){
                String str = inputStreamToString(conn.getInputStream());

                if(!str.equals("err")) {
                    JSONArray jsonArray = new JSONArray(str);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        UserActivityProperties property = new UserActivityProperties();
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        property.setGender(jsonObject.getString("Gender"));
                        property.setAge(jsonObject.getInt("Age"));
                        property.setStepsNum(jsonObject.getInt("StepsNum"));
                        property.setStill(jsonObject.getInt("Still"));
                        property.setContinuousStill(jsonObject.getInt("ContinuousStill"));
                        property.setRunning(jsonObject.getInt("Running"));
                        property.setDriving(jsonObject.getInt("Driving"));
                        property.setCycling(jsonObject.getInt("Cycling"));
                        property.setSleeping(jsonObject.getInt("Sleeping"));
                        property.setWeather(jsonObject.getString("Weather"));
                        property.setTargetWeight(jsonObject.getInt("TargetWeight"));
                        property.setCalories(jsonObject.getInt("Calories"));
                        property.setDayOfTheWeek(jsonObject.getInt("DayOfTheWeek"));
                        property.setPartOfTheDay(jsonObject.getString("PartOfTheDay"));
                        property.setUserInput(jsonObject.getInt("UserInput"));


                        properties.add(property);

                        // OMG RAAAAADIIIIIIIIIIIIII
                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();

        }
        return properties;

    }

    private static String inputStreamToString(InputStream is) {
        String line = "";
        StringBuilder total = new StringBuilder();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        try{
            while((line= rd.readLine())!=null){
                total.append(line);
            }
        }catch(Exception e)
        {
            e.printStackTrace();
        }
        return total.toString();
    }
}


////toDO
//napraviti f-ju za poziv metoda za skladistenje podataka
//update kolona u tabeli
//napraviti f-ju za racunanje faktora
//resetovati checkboxeve u 12 sati i shared preferences