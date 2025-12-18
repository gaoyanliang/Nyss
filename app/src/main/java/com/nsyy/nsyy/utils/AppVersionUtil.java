package com.nsyy.nsyy.utils;

import android.content.Context;

public class AppVersionUtil {
    private volatile static AppVersionUtil uniqueInstance;
    private Context mContext;

    //采用Double CheckLock(DCL)实现单例
    public static AppVersionUtil getInstance() {
        if (uniqueInstance == null) {
            synchronized (AppVersionUtil.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new AppVersionUtil();
                }
            }
        }
        return uniqueInstance;
    }

    public void init(Context context) {
        this.mContext = context;
    }

    // 获取当前运行的版本号
    public double getCurrentVersionCode() {
        try {
            String versionName = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
            return Double.parseDouble(versionName);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
