package com.example.nsyy;

import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.example.nsyy.server.NsyyServerBroadcastReceiver;
import com.example.nsyy.service.LocationServices;
import com.example.nsyy.service.NsServerService;
import com.example.nsyy.utils.LocationUtil;
import com.example.nsyy.utils.PermissionUtil;

public class MainActivity extends BaseActivity {

    private static final String LOAD_RUL = "http://oa.nsyy.com.cn:6060";
    private Toolbar toolbar;
    private WebView webView;
    private ProgressDialog progressDialog;

    private SwipeRefreshLayout swipeRefreshLayout;

    private int version = Build.VERSION.SDK_INT;

    private final NsyyServerBroadcastReceiver nsyyServerBroadcastReceiver =
            new NsyyServerBroadcastReceiver(new NsyyServerBroadcastReceiver.ServerStateListener() {
                @Override
                public void onStart(String hostAddress) {
                    Log.d(TAG, "Nsyy 服务器已经启动，地址为：" + hostAddress);
                    toolbar.setSubtitle("http://" + hostAddress + ":8080");
                }

                @Override
                public void onStop() {
                    Log.d(TAG, "Nsyy 服务器已经停止");
                    toolbar.setSubtitle("服务已停止");
                    progressDialog.dismiss();
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

        // 检查权限: 这里需要开启位置权限 & 位置服务 TODO 其他权限
        PermissionUtil.checkLocationPermission(this);

        LocationServices.getInstance().setActivity(this);

        initView();

        registerReceiver(nsyyServerBroadcastReceiver, new IntentFilter("NsyyServerBroadcastReceiver"));
        startService(new Intent(this, NsServerService.class));//启动服务

        LocationUtil.getInstance(this).initGPS();
    }

    private void initView() {
        // 导航栏
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("正在停止服务...");
        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_chevron_left);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView != null) {
                    if (webView.canGoBack()) {
                        webView.goBack();
                    }
                }
            }
        });

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

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view,
                                                              WebResourceRequest request) {

                Log.i("Nsyy", "=========> rev request" + request.getUrl().toString());

                return shouldInterceptRequest(view, request.getUrl().toString());
            }

            public void onPageFinished(WebView view, String url) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
        // 加载指定网页
        webView.loadUrl(LOAD_RUL);
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