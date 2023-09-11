package com.example.nsyy.service;

import android.app.Activity;

import com.example.nsyy.MainActivity;
import com.example.nsyy.utils.LocationUtil;

public class LocationServices {

    private static LocationServices instance;

    private MainActivity activity;
    private LocationServices (){}

    public static LocationServices getInstance() {
        if (instance == null) {
            instance = new LocationServices();
        }
        return instance;
    }

    public void setActivity(Activity activity) {
        this.activity = (MainActivity) activity;
    }

    public String location(){
        return LocationUtil.getInstance(activity).getLocation(true);
    }
}
