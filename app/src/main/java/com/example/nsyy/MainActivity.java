package com.example.nsyy;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.nsyy.alarm.LongRunningService;
import com.example.nsyy.service.NsServerService;
import com.example.nsyy.service.NsyyServerBroadcastReceiver;
import com.example.nsyy.utils.BlueToothUtil;
import com.example.nsyy.utils.LocationUtil;
import com.example.nsyy.utils.NotificationUtil;
import com.example.nsyy.utils.PermissionUtil;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "Nsyy";
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 启动定时任务 每十分钟打印一次时间
        Intent intent = new Intent(this, LongRunningService.class);
        startService(intent);

        // 检查权限: 这里需要开启位置权限 & 位置服务 TODO 其他权限
        PermissionUtil.checkLocationPermission(this);

        initView();

        // 启动 web server
        registerReceiver(nsyyServerBroadcastReceiver, new IntentFilter("NsyyServerBroadcastReceiver"));
        startService(new Intent(this, NsServerService.class));

        // 消息通知
        PermissionUtil.checkNotification(this);
        NotificationUtil.getInstance().setContext(this);
        NotificationUtil.getInstance().initNotificationChannel();

        // 检查是否开启位置服务
        LocationUtil.getInstance().setContext(this);
        LocationUtil.getInstance().initGPS();


        // 检查是否开启蓝牙权限 & 初始化
        PermissionUtil.checkBlueToothPermission(this);
        BlueToothUtil.getInstance().init(this);
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
        // 这里返回后台运行，而不是直接杀死
        moveTaskToBack(false);
//        // 否则退出应用程序
//        super.onBackPressed();
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
}