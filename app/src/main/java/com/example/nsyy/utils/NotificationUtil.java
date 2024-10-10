package com.example.nsyy.utils;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.example.nsyy.R;
import com.example.nsyy.config.MySharedPreferences;
import com.example.nsyy.notification.NotificationClickReceiver;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Random;

public class NotificationUtil {

    public static final String TAG = "NotificationUtil";
    private static final String CHECK_OP_NO_THROW = "checkOpNoThrow";
    private static final String OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION";
    // 重要消息
    private static final String mHighChannelId = "high_channel_id"; // 渠道ID可以随便定义，只要保证全局唯一性就可以
    private static final String mHignChannelName = "南石医院"; // 渠道名称是给用户看的，需要能够表达清楚这个渠道的用途
    public static final int mHighNotificationId = 9002;

    private NotificationManager notificationManager;
    private volatile static NotificationUtil uniqueInstance;
    private Context context;
    private TextToSpeech textToSpeech;

    public void setContext(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.SIMPLIFIED_CHINESE);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(context, "本设备不支持简体中文语音播报", Toast.LENGTH_SHORT).show();
                }

                boolean find = false;
                Float rate = MySharedPreferences.getSharedPreferences().getFloat("rate", 1.3f);
                String name = MySharedPreferences.getSharedPreferences().getString("name", "xiaozhang");
                for (Voice voice : textToSpeech.getVoices()) {
                    if (voice.getName().contains(name) && (voice.getLocale().equals(Locale.CHINESE) || voice.getLocale().equals(Locale.SIMPLIFIED_CHINESE))) {
                        textToSpeech.setVoice(voice);
                        textToSpeech.setSpeechRate(rate);
                        find = true;
                        Toast.makeText(context, "Speech Initialization successed", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
                if (find == false) {
                    Toast.makeText(context, "没有找到名字是 " + name + " 的语音播报人", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Initialization failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public String toString() {
        return "NotificationUtil{" +
                "notificationManager=" + notificationManager +
                ", mContext=" + context +
                '}';
    }

    //采用Double CheckLock(DCL)实现单例
    public static NotificationUtil getInstance() {
        if (uniqueInstance == null) {
            synchronized (NotificationUtil.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new NotificationUtil();
                }
            }
        }
        return uniqueInstance;
    }

    private NotificationUtil() {
    }

    /**
     * 语音播报
     * @param msg
     */
    public void speechInfo(String msg) {
        textToSpeech.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "text_to_speech");
    }


    /**
     * 消息通知
     *
     * @param title
     */
    public void createNotificationForHigh(String title, String message) {
//        // 设置通知的点按操作
//        Intent intent = new Intent(context, MainActivity.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // todo 组装想要跳转的页面信息 PendingIntent.FLAG_IMMUTABLE 其他的 PendingIntent 可能导致 extra 丢失
        // todo page_type 1=消息页面  page_no 具体页面
        Intent intent = new Intent(context, NotificationClickReceiver.class);
        intent.putExtra("target_page","http://192.168.124.12:6060?aaa=12");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        @SuppressLint("NotificationTrampoline") Notification notification = new NotificationCompat.Builder(context, mHighChannelId)
                // 通知标题
                .setContentTitle(title)
                // 通知内容
                .setContentText(message)
                // 通知框小图标
                .setSmallIcon(R.drawable.ic_notifications_24)
                // 大图标
                //.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_hospital_24))
                // 7.0 设置优先级
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // 配置跳转
                .setContentIntent(pendingIntent)
                // 展开通知
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setCategory(NotificationCompat.CATEGORY_MESSAGE) // 通知类别，"勿扰模式"时系统会决定要不要显示你的通知
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE) // 屏幕可见性，锁屏时，显示icon和标题，内容隐藏
                .setAutoCancel(true)
                .build();
        // 发送
        notificationManager.notify(mHighNotificationId, notification);
    }

    /**
     * 必须先创建通知渠道，然后才能在 Android 8.0 及更高版本上发布任何通知，因此应在应用启动时立即执行这段代码。
     * 反复调用这段代码是安全的，因为创建现有通知渠道不会执行任何操作。
     *
     * 初始化消息渠道,共创建3个渠道：普通消息渠道，重要消息渠道，自定义消息渠道
     */
    public void initNotificationChannel() {
        // 要确保的是当前手机的系统版本必须是Android 8.0系统或者更高，
        // 因为低版本的手机系统并没有通知渠道这个功能，不做系统版本检查的话会在低版本手机上造成崩溃。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 创建一个通知渠道至少需要渠道ID、渠道名称以及重要等级这三个参数
            // 重要等级的不同则会决定通知的不同行为，重要等级还可以设置为IMPORTANCE_LOW、IMPORTANCE_MIN，分别对应了更低的通知重要程度。

            // 重要消息
            createNotificationChannel( true, mHighChannelId, mHignChannelName, NotificationManager.IMPORTANCE_HIGH);
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannel(boolean showDadge, String channelId, String channelName, int importance) {
        // 创建 channel
        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        // 是否在桌面显示角标
        channel.setShowBadge(showDadge);
        // 获取 notificationManager
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        // 注册 channel
        notificationManager.createNotificationChannel(channel);
    }


    //调用该方法获取是否开启通知栏权限
    public static boolean isNotifyEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return isEnableV26(context);
        } else {
            return isEnabledV19(context);
        }
    }

    /**
     * 8.0以下判断
     *
     * @param context api19  4.4及以上判断
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private static boolean isEnabledV19(Context context) {

        AppOpsManager mAppOps =
                (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);

        ApplicationInfo appInfo = context.getApplicationInfo();
        String pkg = context.getApplicationContext().getPackageName();
        int uid = appInfo.uid;
        Class appOpsClass = null;

        try {
            appOpsClass = Class.forName(AppOpsManager.class.getName());

            Method checkOpNoThrowMethod =
                    appOpsClass.getMethod(CHECK_OP_NO_THROW,
                            Integer.TYPE, Integer.TYPE, String.class);

            Field opPostNotificationValue = appOpsClass.getDeclaredField(OP_POST_NOTIFICATION);
            int value = (Integer) opPostNotificationValue.get(Integer.class);

            return ((Integer) checkOpNoThrowMethod.invoke(mAppOps, value, uid, pkg) ==
                    AppOpsManager.MODE_ALLOWED);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * 8.0及以上通知权限判断
     *
     * @param context
     * @return
     */
    private static boolean isEnableV26(Context context) {
        ApplicationInfo appInfo = context.getApplicationInfo();
        String pkg = context.getApplicationContext().getPackageName();
        int uid = appInfo.uid;
        try {
            NotificationManager notificationManager = (NotificationManager)
                    context.getSystemService(NOTIFICATION_SERVICE);
            Method sServiceField = notificationManager.getClass().getDeclaredMethod("getService");
            sServiceField.setAccessible(true);
            Object sService = sServiceField.invoke(notificationManager);

            Method method = sService.getClass().getDeclaredMethod("areNotificationsEnabledForPackage"
                    , String.class, Integer.TYPE);
            method.setAccessible(true);
            return (boolean) method.invoke(sService, pkg, uid);
        } catch (Exception e) {
            return true;
        }
    }


    /**
     * 生成随机字符串
     *
     * @param length
     * @return
     */
    public static String getRandomString(int length){
        String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random=new Random();
        StringBuffer sb=new StringBuffer();
        for(int i=0;i<length;i++){
            int number=random.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }
}

