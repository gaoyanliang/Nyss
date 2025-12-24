package com.nsyy.nsyy.service;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.nsyy.nsyy.config.MySharedPreferences;
import com.huawei.hms.push.HmsMessageService;
public class NsyyHmsMessageService extends HmsMessageService {
    public static final String TAG = "NsyyHmsMessageService";
    @Override
    public void onNewToken(String token, Bundle bundle) {
        // 获取token
        Log.i(TAG, "获取到华为推送token." + token);

        // 判断token是否为空
        if (!TextUtils.isEmpty(token)) {
            refreshedTokenToServer(token);
        }
    }


    private void refreshedTokenToServer(String token) {
        Log.i(TAG, "保存华为推送token " + token);
        SharedPreferences.Editor editor = MySharedPreferences.getSharedPreferences().edit();
        editor.putString("token", token);
        editor.apply();
    }
}
