package com.nsyy.nsyy.service;

import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.nsyy.nsyy.permission.AppApplication;

// app上架审核比较严格，不能在app启动时就申请 位置权限，要在使用时在申请
// 使用 LocalBroadcastManager 实现权限申请触发
// 1. 在 MainActivity 中注册
// 2. 在andserver接口中触发
public class LocalBroadcastHelper {
    private static final LocalBroadcastManager instance = LocalBroadcastManager.getInstance(AppApplication.getContext());

    public static LocalBroadcastManager getInstance() {
        return instance;
    }

    // 定义动作常量
    public static final String ACTION_REQUEST_LOCATION_PERMISSION = "action_request_location_permission";
    public static final String ACTION_REQUEST_NOTIFICATION_PERMISSION = "action_request_notification_permission";

    // 发送广播的方法
    public static void sendRequestLocationPermission() {
        Intent intent = new Intent(ACTION_REQUEST_LOCATION_PERMISSION);
        instance.sendBroadcast(intent);
    }

    public static void sendRequestNotificationPermission() {
        Intent intent = new Intent(ACTION_REQUEST_NOTIFICATION_PERMISSION);
        instance.sendBroadcast(intent);
    }

}
