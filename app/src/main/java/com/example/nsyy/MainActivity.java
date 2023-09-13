package com.example.nsyy;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.example.nsyy.notification.MessageActivity;
import com.example.nsyy.server.NsyyServerBroadcastReceiver;
import com.example.nsyy.service.LocationServices;
import com.example.nsyy.service.NotificationServices;
import com.example.nsyy.service.NsServerService;
import com.example.nsyy.utils.LocationUtil;
import com.example.nsyy.utils.NotificationUtil;
import com.example.nsyy.utils.PermissionUtil;

import java.util.Random;

public class MainActivity extends BaseActivity {

    private static final String LOAD_RUL = "http://oa.nsyy.com.cn:6060";
    private WebView webView;
    private SwipeRefreshLayout swipeRefreshLayout;

    private int version = Build.VERSION.SDK_INT;

    private final NsyyServerBroadcastReceiver nsyyServerBroadcastReceiver =
            new NsyyServerBroadcastReceiver(new NsyyServerBroadcastReceiver.ServerStateListener() {
                @Override
                public void onStart(String hostAddress) {
                    Log.d(TAG, "Nsyy 服务器已经启动，地址为：" + hostAddress);
                }

                @Override
                public void onStop() {
                    Log.d(TAG, "Nsyy 服务器已经停止");
                }

                @Override
                public void onError(String error) {
                    super.onError(error);
                    Log.e(TAG, error);
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                }
            });

    private NotificationManager notificationManager;
    // 重要消息
    private String mHighChannelId = "high_channel_id"; // 渠道ID可以随便定义，只要保证全局唯一性就可以
    private String mHignChannelName = "南石医院"; // 渠道名称是给用户看的，需要能够表达清楚这个渠道的用途
    public static final int mHighNotificationId = 9002;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 检查权限: 这里需要开启位置权限 & 位置服务 TODO 其他权限
        PermissionUtil.checkLocationPermission(this);
        LocationServices.getInstance().setActivity(this);
        LocationUtil.getInstance(this).initGPS();

        initView();

        // 启动 web server
        registerReceiver(nsyyServerBroadcastReceiver, new IntentFilter("NsyyServerBroadcastReceiver"));
        startService(new Intent(this, NsServerService.class));

        // 消息通知
        checkNotification(this);
        initNotificationChannel();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationServices.getInstance().setActivity(this);
    }

    private void initView() {
        swipeRefreshLayout = findViewById(R.id.refreshLayout);
        // 配置 SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // 检查 WebView 是否为空
                if (webView == null) {
                    swipeRefreshLayout.setRefreshing(false);
                    return;
                }
                // 在 UI 线程上执行 WebView 刷新
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        webView.reload();
                    }
                });
            }
        });

        webView = findViewById(R.id.webView);
        // Enable Javascript
        WebSettings webSettings = webView.getSettings();
        webSettings.setDomStorageEnabled(true);
        // 设置 WebView 允许执行 JavaScript 脚本
        webSettings.setJavaScriptEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // 确保跳转到另一个网页时仍然在当前 WebView 中显示,而不是调用浏览器打开
        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
        // 加载 南石OA
        webView.loadUrl(LOAD_RUL);
    }

    // 接管返回按键的响应
    @Override
    public void onBackPressed() {
        // 如果 WebView 可以返回，则返回上一页
        if (webView.canGoBack()) {
            webView.goBack();
            return;
        }
        // 否则退出应用程序
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        webView.destroy();
        unregisterReceiver(nsyyServerBroadcastReceiver);
        stopService(new Intent(this, NsServerService.class));//停止服务
    }


    /**
     * 检测是否开启通知
     *
     * @param context
     */
    private void checkNotification(final Context context) {
        if (!NotificationUtil.isNotifyEnabled(context)) {
            new AlertDialog.Builder(context).setTitle("温馨提示")
                    .setMessage("你还未开启系统通知，将影响消息的接收，要去开启吗？")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setNotification();
                        }
                    }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).show();
        }
    }

    /**
     * 如果没有开启通知，跳转至设置界面
     */
    private void setNotification() {
        Intent localIntent = new Intent();
        //直接跳转到应用通知设置的代码：
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//8.0及以上
            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            localIntent.setData(Uri.fromParts("package", getPackageName(), null));
        } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//5.0以上到8.0以下
            localIntent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            localIntent.putExtra("app_package", getPackageName());
            localIntent.putExtra("app_uid", getApplicationInfo().uid);
        } else if (android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {//4.4
            localIntent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            localIntent.addCategory(Intent.CATEGORY_DEFAULT);
            localIntent.setData(Uri.parse("package:" + getPackageName()));
        } else {
            //4.4以下没有从app跳转到应用通知设置页面的Action，可考虑跳转到应用详情页面,
            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= 9) {
                localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                localIntent.setData(Uri.fromParts("package", getPackageName(), null));
            } else if (Build.VERSION.SDK_INT <= 8) {
                localIntent.setAction(Intent.ACTION_VIEW);
                localIntent.setClassName("com.android.settings", "com.android.setting.InstalledAppDetails");
                localIntent.putExtra("com.android.settings.ApplicationPkgName", getPackageName());
            }
        }
        startActivity(localIntent);
    }

    /**
     * 消息通知
     *
     * @param title
     * @param context
     */
    public void createNotificationForHigh(String title, String context) {
        // 设置通知的点按操作
        Intent intent = new Intent(this, MessageActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(MainActivity.this, mHighChannelId)
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