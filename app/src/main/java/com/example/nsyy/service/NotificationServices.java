package com.example.nsyy.service;

import android.app.Activity;

import com.example.nsyy.MainActivity;

public class NotificationServices {

    private static NotificationServices instance;

    private MainActivity activity;
    private NotificationServices (){}

    public static NotificationServices getInstance() {
        if (instance == null) {
            instance = new NotificationServices();
        }
        return instance;
    }

    public void setActivity(Activity activity) {
        this.activity = (MainActivity) activity;
    }

    public void notification(){
        activity.createNotificationForHigh("南石医院", MainActivity.getRandomString(100));
    }
}
