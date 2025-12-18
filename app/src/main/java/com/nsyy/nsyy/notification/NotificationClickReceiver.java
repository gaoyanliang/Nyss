package com.nsyy.nsyy.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationClickReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String targetPage = intent.getStringExtra("target_page");
//        Log.i("TAG", "============================ " +
//                "userClick:我被点击啦！！！ targetPage = " + targetPage);

        // 发送广播到 MainActivity
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("LOAD_TARGET_PAGE");
        broadcastIntent.putExtra("target_page", targetPage);
        context.sendBroadcast(broadcastIntent);

    }

}
