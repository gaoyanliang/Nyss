package com.example.nsyy.service;

import android.app.Activity;

import com.example.nsyy.MainActivity;
import com.example.nsyy.server.api.request.Notification;
import com.example.nsyy.utils.NotificationUtil;

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

    public void notification(Notification notification){
//        NotificationUtil.getInstance(activity).createNotificationForHigh("南石医院", NotificationUtil.getRandomString(100));
        NotificationUtil.getInstance(activity).createNotificationForHigh(notification.title, notification.context);
    }
}
