package com.nsyy.nsyy.service;

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.hihonor.push.sdk.HonorMessageService;
import com.hihonor.push.sdk.HonorPushDataMsg;
import com.nsyy.nsyy.config.MySharedPreferences;

public class NsyyHonorMessageService extends HonorMessageService {
    public static final String TAG = "NsyyHonorMessageService";
    //Token发生变化时，会以onNewToken方法返回
    @Override
    public void onNewToken(String pushToken) {
        Log.i(TAG, "获取到荣耀推送token." + pushToken);

        // 判断token是否为空
        if (!TextUtils.isEmpty(pushToken)) {
            refreshedTokenToServer(pushToken);
        }
    }

    @Override
    public void onMessageReceived(HonorPushDataMsg msg) {
        // 处理收到的透传消息。
        Log.i(TAG, "接收到荣耀推送消息 " + msg.toString());
    }

    private void refreshedTokenToServer(String token) {
        Log.i("NsyyHonorMessageService", "保存华为推送token " + token);
        SharedPreferences.Editor editor = MySharedPreferences.getSharedPreferences().edit();
        editor.putString("token", token);
        editor.apply();
    }
}
