package com.nsyy.nsyy.service;

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.heytap.msp.push.callback.ICallBackResultService;
import com.nsyy.nsyy.MainActivity;
import com.nsyy.nsyy.config.MySharedPreferences;

public class NsyyOppoMessageService implements ICallBackResultService {
    public static final String TAG = "NsyyOppoMessageService";

    private void refreshedTokenToServer(String token) {
        Log.i("NsyyHonorMessageService", "保存oppo推送token " + token);
        SharedPreferences.Editor editor = MySharedPreferences.getSharedPreferences().edit();
        editor.putString("token", token);
        editor.apply();
    }


    @Override
    public void onRegister(int responseCode, String registerID, String packageName, String miniPackageName) {
        StringBuilder stringBuilder = new StringBuilder("onRegister() :");
        stringBuilder.append("responseCode:").append(responseCode).append(" registerID:").append(registerID).
                append(" packageName:").append(packageName).append(" miniPackageName:").append(miniPackageName);
        Log.i(TAG, "OPPO onRegister " + stringBuilder.toString());

        // 判断token是否为空
        if (!TextUtils.isEmpty(registerID)) {
            MainActivity.oppoFlag = false;
            refreshedTokenToServer(registerID);
        }
    }


    @Override
    public void onUnRegister(int responseCode, String packageName, String miniProgramPkg) {
        StringBuilder stringBuilder = new StringBuilder("onRegister() :");
        stringBuilder.append("responseCode:").append(responseCode).
                append(" packageName:").append(packageName).append(" miniPackageName:").append(miniProgramPkg);
        Log.i(TAG, "OPPO onUnRegister " + stringBuilder.toString());
    }

    @Override
    public void onGetPushStatus(final int code, int status) {
        if (code == 0 && status == 0) {
            Log.i(TAG, "OPPO Push状态正常 " + "code=" + code + ",status=" + status);
        } else {
            Log.i(TAG, "OPPO Push状态错误 " + "code=" + code + ",status=" + status);
        }
    }

    @Override
    public void onGetNotificationStatus(final int code, final int status) {
        if (code == 0 && status == 0) {
            Log.i(TAG, "OPPO 通知状态正常 " + "code=" + code + ",status=" + status);
        } else {
            Log.i(TAG, "OPPO 通知状态错误 " + "code=" + code + ",status=" + status);
        }
    }

    @Override
    public void onSetPushTime(final int code, final String s) {
        Log.i(TAG, "OPPO SetPushTime " + "code=" + code + ",result:" + s);
    }


    @Override
    public void onError(int i, String s, String s1, String s2) {

    }
}
