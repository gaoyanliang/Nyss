package com.example.nsyy.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.nsyy.MainActivity;
import com.example.nsyy.R;
import com.example.nsyy.server.NsyyServerBroadcastReceiver;
import com.example.nsyy.utils.NetUtils;
import com.yanzhenjie.andserver.AndServer;
import com.yanzhenjie.andserver.Server;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

public class NsServerService extends Service {

    private Server server;

    @Override
    public void onCreate() {
        super.onCreate();

        server= AndServer.webServer(this)
                .port(ServerConfig.SERVER_PORT) //服务器端口
                .timeout(ServerConfig.SERVER_TIMEOUT, TimeUnit.SECONDS) //连接超时，响应超时时间
                .listener(new Server.ServerListener() {
                    @Override
                    public void onStarted() {
                        InetAddress localIPAddress = NetUtils.getLocalIPAddress();
                        Log.i(MainActivity.TAG, "NsServerService: 服务启动：获取到本地地址" + localIPAddress.getHostAddress());

                        if (localIPAddress!=null){
                            NsyyServerBroadcastReceiver.onServerStart(NsServerService.this,localIPAddress.getHostAddress());//服务器启动的时候发送广播
                        }else {
                            NsyyServerBroadcastReceiver.onServerError(NsServerService.this, "获取网络地址失败，请检查网络连接。");//服务器出现异常的时候发送一条广播
                        }
                    }

                    @Override
                    public void onStopped() {
                        Log.i(MainActivity.TAG, "NsServerService: 服务停止");
                        NsyyServerBroadcastReceiver.onServerStop(NsServerService.this);//服务器停止的时候发送一条广播
                    }

                    @Override
                    public void onException(Exception e) {
                        Log.i(MainActivity.TAG, "NsServerService: 服务异常");
                        e.printStackTrace();
                        NsyyServerBroadcastReceiver.onServerError(NsServerService.this, e.getMessage());//服务器出现异常的时候发送一条广播
                    }
                })
                .build();


        /**
         * 启动一个前台进程，提高自身优先级，尽量后台保活。 参考：https://blog.51cto.com/u_16099245/6603732
         */
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = new NotificationChannel("high_channel_id",
                    "南石医院", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        Intent intent=new Intent(this, MainActivity.class);
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "high_channel_id");
        builder.setContentTitle("南石医院");
        builder.setContentText("欢迎使用南石医院 APP，祝您度过愉快的一天 (*￣︶￣)");
        builder.setSmallIcon(R.drawable.ic_notifications_24);
        //builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.ic_hospital_24));
        builder.setContentIntent(pendingIntent);
        Notification notification = builder.build();

        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        server.startup();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        server.shutdown();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
