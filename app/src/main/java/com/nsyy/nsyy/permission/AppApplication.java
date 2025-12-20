package com.nsyy.nsyy.permission;

import android.app.Application;
import android.content.Context;

import com.hjq.toast.Toaster;
import com.hjq.toast.style.WhiteToastStyle;

/**
 *    desc   : 应用入口 只能有一个 Application
 *    Android 系统机制：在 AndroidManifest.xml 的 <application> 标签中，
 *    只能通过 android:name 属性指定一个自定义 Application 类。
 *      系统在 App 启动时会只创建这个指定的 Application 实例，调用它的 onCreate()。
 */
public final class AppApplication extends Application {
    private static AppApplication instance;  // 添加单例

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // 初始化吐司工具类
        Toaster.init(this, new WhiteToastStyle());
    }

    // 提供全局 Context（Application Context）
    public static AppApplication getInstance() {
        return instance;
    }

    // 更常用的是直接返回 Context
    public static Context getContext() {
        return instance.getApplicationContext();
    }
}