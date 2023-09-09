package com.example.nsyy;


import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.example.nsyy.permission.PermissionInterceptor;
import com.example.nsyy.permission.PermissionNameConvert;
import com.example.nsyy.server.NsyyServerBroadcastReceiver;
import com.example.nsyy.service.LocationServices;
import com.example.nsyy.service.NsServerService;
import com.example.nsyy.utils.LocationUtil;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.hjq.toast.Toaster;

import java.util.List;


public class MainActivity extends BaseActivity {

    private static final String LOAD_RUL = "http://oa.nsyy.com.cn:6060";
    private Toolbar toolbar;
    private WebView webView;
    private Menu menu;
    private ProgressDialog progressDialog;

    private SwipeRefreshLayout swipeRefreshLayout;

    private int version = Build.VERSION.SDK_INT;

    private final NsyyServerBroadcastReceiver nsyyServerBroadcastReceiver =
            new NsyyServerBroadcastReceiver(new NsyyServerBroadcastReceiver.ServerStateListener() {
                @Override
                public void onStart(String hostAddress) {
                    Log.d(TAG, "Nsyy 服务器已经启动，地址为：" + hostAddress);
                    toolbar.setSubtitle("http://" + hostAddress + ":8080");

                    //webView.loadUrl("http://" + hostAddress + ":8080");
                    if (menu != null) {
                        menu.findItem(R.id.stop).setVisible(true);
                        menu.findItem(R.id.start).setVisible(false);
                    }
                }

                @Override
                public void onStop() {
                    Log.d(TAG, "Nsyy 服务器已经停止");
                    toolbar.setSubtitle("服务已停止");
                    progressDialog.dismiss();
                    if (menu != null) {
                        menu.findItem(R.id.stop).setVisible(false);
                        menu.findItem(R.id.start).setVisible(true);
                    }
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
        initView();

        // 检查权限: 这里需要开启位置权限 & 位置服务 TODO 其他权限
        checkLocationPermission();

        registerReceiver(nsyyServerBroadcastReceiver, new IntentFilter("NsyyServerBroadcastReceiver"));
        startService(new Intent(this, NsServerService.class));//启动服务

        initLocation();

        LocationServices.getInstance().setActivity(this);
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
            public void onPageFinished(WebView view, String url) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
        // 加载指定网页
        webView.loadUrl(LOAD_RUL);
    }

    private void checkLocationPermission() {

        // 判断是否已经获取位置权限，没有获取先获取位置权限
        if (!XXPermissions.isGranted(this, new String[]{
                Permission.ACCESS_COARSE_LOCATION,
                Permission.ACCESS_FINE_LOCATION,
                Permission.ACCESS_BACKGROUND_LOCATION})) {
            XXPermissions.with(this)
                    .permission(Permission.ACCESS_COARSE_LOCATION)
                    .permission(Permission.ACCESS_FINE_LOCATION)
                    // 如果不需要在后台使用定位功能，请不要申请此权限
                    .permission(Permission.ACCESS_BACKGROUND_LOCATION)
                    .interceptor(new PermissionInterceptor())
                    .request(new OnPermissionCallback() {

                        @Override
                        public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                            if (!allGranted) {
                                return;
                            }
                            toast(String.format(getString(R.string.demo_obtain_permission_success_hint),
                                    PermissionNameConvert.getPermissionString(MainActivity.this, permissions)));
                        }
                    });
        }

        openLocationProvider();
    }

    private void openLocationProvider() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //判断GPS是否开启，没有开启，则开启并更新位置信息
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)  ||
            !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){

            // leads to the settings because there is no last known location
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
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

    public void toast(CharSequence text) {
        Toaster.show(text);
    }


    private void initLocation() {
        //注意6.0及以上版本需要在申请完权限后调用方法
        LocationUtil.getInstance(this).setAddressCallback(new LocationUtil.AddressCallback() {
            @Override
            public void onGetAddress(Address address) {
                String countryName = address.getCountryName();//国家
                String adminArea = address.getAdminArea();//省
                String locality = address.getLocality();//市
                String subLocality = address.getSubLocality();//区
                String featureName = address.getFeatureName();//街道
                Log.e("定位地址: ",countryName+adminArea+locality+subLocality+featureName);
            }

            @Override
            public void onGetLocation(double lat, double lng) {
                Log.e("定位经纬度: ",lat + "\n" + lng);
            }
        });

    }

}