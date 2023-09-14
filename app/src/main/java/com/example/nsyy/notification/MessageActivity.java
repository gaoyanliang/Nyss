package com.example.nsyy.notification;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.example.nsyy.R;
import com.example.nsyy.server.NsyyServerBroadcastReceiver;
import com.example.nsyy.service.NotificationServices;
import com.example.nsyy.service.NsServerService;

import java.util.Random;

/**
 * 测试过程中使用该类，目前功能已经迁移到 MainActivity
 */
public class MessageActivity extends AppCompatActivity {

    private NotificationManager notificationManager;

    // 普通消息
    private String mNormalChannelId = "normal_channel_id"; // 渠道ID可以随便定义，只要保证全局唯一性就可以
    private String mNormalChannelName = "normal_channel_name"; // 渠道名称是给用户看的，需要能够表达清楚这个渠道的用途

    // 重要消息
    private String mHighChannelId = "high_channel_id";
    private String mHignChannelName = "high_channel_name";

    public static final int mNormalNotificationId = 9001;
    public static final int mHighNotificationId = 9002;

    private final NsyyServerBroadcastReceiver nsyyServerBroadcastReceiver =
            new NsyyServerBroadcastReceiver(new NsyyServerBroadcastReceiver.ServerStateListener() {
                @Override
                public void onStart(String hostAddress) {
                    Log.d("MessageActivity", "Nsyy 服务器已经启动，地址为：" + hostAddress);
                }

                @Override
                public void onStop() {
                    Log.d("MessageActivity", "Nsyy 服务器已经停止");
                }

                @Override
                public void onError(String error) {
                    super.onError(error);
                    Log.e("MessageActivity", error);
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        //checkNotification(this);

        initNotificationChannel();

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        registerReceiver(nsyyServerBroadcastReceiver, new IntentFilter("NsyyServerBroadcastReceiver"));
        startService(new Intent(this, NsServerService.class));//启动服务

        NotificationServices.getInstance().setActivity(this);



        View btn_send_chat = findViewById(R.id.btn_send_chat);
        btn_send_chat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createNotificationForNormal("普通消息", getRandomString(100));
            }
        });


        View btn_send_subscribe = findViewById(R.id.btn_send_subscribe);
        btn_send_subscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                createNotificationForHigh("重要消息", getRandomString(100));
            }
        });


    }


    /**
     * 普通通知
     *
     * @param title
     * @param context
     */
    public void createNotificationForNormal(String title, String context) {
        // 设置通知的点按操作
        Intent intent = new Intent(this, MessageActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // 创建通知
        Notification notification = new NotificationCompat.Builder(MessageActivity.this, mNormalChannelId)
                // 通知框小图标
                .setSmallIcon(R.drawable.ic_notifications_24)
                // 大图标
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_hospital_24))
                // 通知标题
                .setContentTitle(title)
                // 通知内容
                .setContentText(context)
                .setShowWhen(true)
                // 7.0 设置优先级
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // 配置跳转
                .setContentIntent(pendingIntent)
                // 展开通知
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(context))
//                //息屏通知
//                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                // 是否自动消失（点击）or mManager.cancel(mNormalNotificationId)、cancelAll、setTimeoutAfter()
                .setAutoCancel(true)
                .build();

        // 发送通知
        notificationManager.notify(mNormalNotificationId, notification);
    }

    /**
     * 重要通知
     *
     * @param title
     * @param context
     */
    public void createNotificationForHigh(String title, String context) {
        // 设置通知的点按操作
        Intent intent = new Intent(this, MessageActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(MessageActivity.this, mHighChannelId)
                // 通知框小图标
                .setSmallIcon(R.drawable.ic_notifications_24)
                // 大图标
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_hospital_24))
                // 通知标题
                .setContentTitle(title)
                // 通知内容
                .setContentText(context)
                .setShowWhen(true)
                // 7.0 设置优先级
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // 配置跳转
                .setContentIntent(pendingIntent)
                // 展开通知
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(context))
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
    private void initNotificationChannel() {
        // 要确保的是当前手机的系统版本必须是Android 8.0系统或者更高，
        // 因为低版本的手机系统并没有通知渠道这个功能，不做系统版本检查的话会在低版本手机上造成崩溃。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 创建一个通知渠道至少需要渠道ID、渠道名称以及重要等级这三个参数
            // 重要等级的不同则会决定通知的不同行为，重要等级还可以设置为IMPORTANCE_LOW、IMPORTANCE_MIN，分别对应了更低的通知重要程度。

            // 普通消息
            createNotificationChannel(true, mNormalChannelId, mNormalChannelName, NotificationManager.IMPORTANCE_LOW);

            // 重要消息
            createNotificationChannel(true, mHighChannelId, mHignChannelName, NotificationManager.IMPORTANCE_HIGH);
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannel(boolean showDadge, String channelId, String channelName, int importance) {
        // 创建 channel
        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        // 是否在桌面显示角标
        channel.setShowBadge(showDadge);
        // 获取 notificationManager
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // 注册 channel
        notificationManager.createNotificationChannel(channel);
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