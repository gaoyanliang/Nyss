package com.example.nsyy.oa;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.example.nsyy.R;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

public class NsyyActivity extends AppCompatActivity {

    public final static String LOAD_URL = "http://oa.nsyy.com.cn:6060";
    private WebView webView;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nsyy);

        swipeRefreshLayout = findViewById(R.id.nsyy_oa_layout);
        creatWebView();

        // 检查 Internet 权限
        if (!hasInternetPermission()) {
            Toast.makeText(this, "请授予应用程序 Internet 权限", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void creatWebView() {
        // 创建 WebView 实例并通过 id 绑定我们刚在布局中创建的 WebView 标签
        // 这里的 R.id.webview 就是 activity_main.xml 中的 WebView 标签的 id
        webView = findViewById(R.id.nsyy_oa);

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
        webView.loadUrl(LOAD_URL);
    }

    private boolean hasInternetPermission() {
        return checkSelfPermission(android.Manifest.permission.INTERNET) == getPackageManager().PERMISSION_GRANTED;
    }

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
}