package com.nsyy.nsyy.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.nsyy.nsyy.config.MySharedPreferences;
import com.vivo.push.model.UPSNotificationMessage;
import com.vivo.push.model.UnvarnishedMessage;
import com.vivo.push.sdk.OpenClientPushMessageReceiver;

public class NsyyVivoMessageReceiverImpl extends OpenClientPushMessageReceiver {
    public static final String TAG = "NsyyVivoMessageReceiverImpl";

    private void refreshedTokenToServer(String token) {
        Log.i(TAG, "保存oppo推送token " + token);
        SharedPreferences.Editor editor = MySharedPreferences.getSharedPreferences().edit();
        editor.putString("token", token);
        editor.apply();
    }
    @Override
    public void onReceiveRegId(Context context, String s) {
        Log.d(TAG, " onReceiveRegId= " + s);
        // 判断token是否为空
        if (!TextUtils.isEmpty(s)) {
            refreshedTokenToServer(s);
        }
    }

    @Override
    public void onTransmissionMessage(Context context, UnvarnishedMessage unvarnishedMessage) {
        Log.d(TAG, " onTransmissionMessage= " + unvarnishedMessage.getMessage());
    }

    @Override
    public void onForegroundMessageArrived(UPSNotificationMessage msg) {
        Log.d(TAG, " 收到前台不展示消息 onForegroundMessageArrived= " + msg);

    }


}
