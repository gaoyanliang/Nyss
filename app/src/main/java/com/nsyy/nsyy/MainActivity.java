package com.nsyy.nsyy;

import com.hihonor.push.sdk.HonorPushClient;
import com.nsyy.Nsyy.R;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.core.app.ActivityOptionsCompat;
import android.content.ActivityNotFoundException;

import android.app.AlertDialog;
import android.content.DialogInterface;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Base64;

import com.nsyy.nsyy.config.MySharedPreferences;
import com.nsyy.nsyy.config.NsyyConfig;
import com.nsyy.nsyy.config.PrivacyPolicyDialogFragment;
import com.nsyy.nsyy.email.EmailDatabaseHelper;
import com.nsyy.nsyy.message.FileHelper;
import com.nsyy.nsyy.message.MessageDatabaseHelper;
import com.nsyy.nsyy.service.NsServerService;
import com.nsyy.nsyy.service.NsyyServerBroadcastReceiver;
import com.nsyy.nsyy.utils.AppVersionUtil;
import com.nsyy.nsyy.utils.LocationUtil;
import com.nsyy.nsyy.utils.NotificationUtil;
import com.nsyy.nsyy.utils.PermissionUtil;
import com.nsyy.nsyy.service.LocalBroadcastHelper;

import com.nsyy.nsyy.utils.SocketUtil;
import com.nsyy.nsyy.vivo_scan.VivoQRCodeScanActivity;
import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;
import com.huawei.hms.push.HmsMessaging;
import com.king.camera.scan.CameraScan;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    public static final int DEFAULT_VIEW = 0x22;
    private static final int REQUEST_CODE_SCAN = 0X01;
    private static final int REQUEST_CODE_VIVO_SCAN = 0X02;
    public static final int REQUEST_FILE_PERMISSION_CODE = 666;
    public static final int CAMERA_PERMISSION_REQUEST_CODE= 777;
    public static final String TAG = "Nsyy";

    private WebView webView;

    // 处理文件选择上传
    private ValueCallback<Uri[]> mFilePathCallback;
    private static final int REQUEST_CODE_FILE_CHOOSER = 1;

    private static MessageDatabaseHelper dbHelper;
    private static EmailDatabaseHelper emailHelper;

    // 用于处理定位权限 的接收器
    private BroadcastReceiver permissionReceiver;

    private static final int REQ_CAMERA = 10001;
    private static final int REQ_PICK_IMAGE = 10002;
    private Uri cameraImageUri;

    private boolean hasShownPrivacyDialog = false;

    private RelativeLayout loadingOverlay;

    public static MessageDatabaseHelper getDatabaseHelper() {
        return dbHelper;
    }
    public static EmailDatabaseHelper getEmailHelper() {
        return emailHelper;
    }

    private AlertDialog downloadDialog;

    private final BroadcastReceiver noticeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String targetPage = intent.getStringExtra("target_page");
            // 在 WebView 中加载目标页面
            webView.loadUrl(targetPage);
        }
    };

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
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sp = getSharedPreferences("app_config", MODE_PRIVATE);
        boolean hasAgreed = sp.getBoolean("has_agreed_privacy", false);

        if (!hasAgreed) {
            // 只设置主布局（带加载层），显示加载中
            setContentView(R.layout.activity_main);

            // 弹出隐私政策弹窗
            if (!hasShownPrivacyDialog) {
                hasShownPrivacyDialog = true;
                new PrivacyPolicyDialogFragment().show(getSupportFragmentManager(), "privacy_dialog");
            }
            return;
        }

        // 已同意，直接完整初始化
        fullSetupAfterAgree();
    }

    // 新增：仅初始化 WebView 基本设置，不加载页面、不申请权限
    private void initBasicWebView() {
        // === 先初始化 WebView 和加载层
        setContentView(R.layout.activity_main);  // 确保已设置布局

        // 找到加载覆盖层
        loadingOverlay = findViewById(R.id.loading_overlay);

        // 初始化 WebView
        webView = findViewById(R.id.webView);

        // 基本的 WebView 设置
        WebSettings webSettings = webView.getSettings();
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true); // 设置允许JS弹窗
        webSettings.setJavaScriptEnabled(true);  // 设置 WebView 允许执行 JavaScript 脚本
        webSettings.setAllowContentAccess(true); // 是否可访问Content Provider的资源，默认值 true
        webSettings.setAllowFileAccess(true);    // 是否可访问本地文件，默认值 true
        // 对于Android 5+设备
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        // Enable Javascript
        webView.addJavascriptInterface(this, "AndroidInterface");
        webView.setBackgroundColor(Color.WHITE);  // 防止 WebView 背景透明导致黑屏

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);  // 硬件加速
        }

        // 关键：每次初始化 WebView 时都强制显示加载层
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.VISIBLE);
        }
    }


    // 同意后执行的完整初始化
    public void fullSetupAfterAgree() {
        initBasicWebView();  // 先确保 WebView 已初始化

        // === 现在才开始敏感操作 ===

        // 数据库初始化
        dbHelper = MessageDatabaseHelper.getInstance(this);
        emailHelper = EmailDatabaseHelper.getInstance(this);
        MySharedPreferences.init(this);

        // 版本检测、文件工具
        AppVersionUtil.getInstance().init(this);
        FileHelper.getInstance().setContext(this);

        // 启动 web server
        registerReceiver(nsyyServerBroadcastReceiver, new IntentFilter("NsyyServerBroadcastReceiver"));
        startService(new Intent(this, NsServerService.class));

        // 注册广播接收器
        registerReceiver(noticeReceiver, new IntentFilter("LOAD_TARGET_PAGE"));
        registerPermissionReceiver();

        // 检查权限:  定位权限改用使用时申请
        LocationUtil.getInstance().setContext(this);

        // 消息通知
        PermissionUtil.checkNotification(this);
        NotificationUtil.getInstance().setContext(this);
        NotificationUtil.getInstance().initNotificationChannel();

        // socket 连接
        SocketUtil.getInstance().setContext(this);

        // === 最后加载页面 ===
        loadView();

        // 初始化消息推送
        initPush();
    }

    private void registerPermissionReceiver() {
        permissionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (LocalBroadcastHelper.ACTION_REQUEST_LOCATION_PERMISSION.equals(action)) {
                    // 切到主线程（虽然 LocalBroadcastManager 默认在主线程发送，但保险）
                    runOnUiThread(() -> {
                        PermissionUtil.checkLocationPermission(MainActivity.this);
                    });
                } else if (LocalBroadcastHelper.ACTION_REQUEST_NOTIFICATION_PERMISSION.equals(action)) {
                    runOnUiThread(() -> {
                        PermissionUtil.checkNotification(MainActivity.this);
                    });
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(LocalBroadcastHelper.ACTION_REQUEST_LOCATION_PERMISSION);
        filter.addAction(LocalBroadcastHelper.ACTION_REQUEST_NOTIFICATION_PERMISSION);
        LocalBroadcastManager.getInstance(this).registerReceiver(permissionReceiver, filter);
    }


    /**
     * 判断当前设备是否属于指定品牌（包含常见子品牌）
     */
    public static boolean isBrand(String targetBrand) {
        if (targetBrand == null) return false;

        String manufacturer = Build.MANUFACTURER;
        if (manufacturer == null) return false; // 极少数情况防护

        String upperManufacturer = manufacturer.toUpperCase(Locale.ROOT);

        Log.d("Device", "当前设备手机厂商是 " + Build.MANUFACTURER + " 手机型号是 " + Build.MODEL + " 手机品牌是 " + Build.BRAND);

        // 根据目标品牌返回匹配结果
        switch (targetBrand.toLowerCase(Locale.ROOT)) {
            case "xiaomi": // 小米及其子品牌
                return upperManufacturer.contains("XIAOMI") ||
                        upperManufacturer.contains("REDMI") ||
                        upperManufacturer.contains("POCO") ||
                        upperManufacturer.contains("BLACKSHARK"); // 黑鲨

            case "huawei": // 华为及荣耀（荣耀已独立，但部分老设备仍显示 HUAWEI）
                return upperManufacturer.contains("HUAWEI") ;

            case "honor": // 荣耀
                return upperManufacturer.contains("HONOR");

            case "vivo":
                return upperManufacturer.contains("VIVO") ||
                        upperManufacturer.contains("IQOO"); // iQOO 是 vivo 子品牌

            case "oppo":
                return upperManufacturer.contains("OPPO") ||
                        upperManufacturer.contains("REALME") ||
                        upperManufacturer.contains("ONEPLUS"); // 一加属于 OPPO 体系

            default:
                // 如果想支持其他品牌，可扩展
                return upperManufacturer.contains(targetBrand.toUpperCase(Locale.ROOT));
        }
    }

    private void initPush(){
        if (isBrand("xiaomi")) {
            Log.d("Device", "这是小米/红米/POCO/黑鲨设备");
        } else if (isBrand("huawei")) {
            Log.d("Device", "这是华为设备");
            // https://developer.huawei.com/consumer/cn/doc/HMSCore-Guides/android-client-dev-0000001050042041
            // 设置自动初始化
            HmsMessaging.getInstance(this).setAutoInitEnabled(true);
        } else if (isBrand("honor")) {
            Log.d("Device", "这是荣耀设备");
            //  https://developer.honor.com/cn/docs/11002/guides/sdk-base-api
            boolean isSupport = HonorPushClient.getInstance().checkSupportHonorPush(getApplicationContext());
            if (isSupport) {
                HonorPushClient.getInstance().init(getApplicationContext(), true);
            } else {
                Toast.makeText(MainActivity.this, "当前系统不支持消息推送", Toast.LENGTH_SHORT).show();
            }
        } else if (isBrand("vivo")) {
            Log.d("Device", "这是vivo或iQOO设备");
        } else if (isBrand("oppo")) {
            Log.d("Device", "这是OPPO、一加或Realme设备");
        } else {
            Toast.makeText(MainActivity.this, "当前设备不支持消息推送", Toast.LENGTH_SHORT).show();
        }
    }


    private void loadView() {
        SharedPreferences.Editor editor = MySharedPreferences.getSharedPreferences().edit();
        editor.putString("load_url", NsyyConfig.LOAD_RUL);
        editor.apply();

        // 确保跳转到另一个网页时仍然在当前 WebView 中显示,而不是调用浏览器打开
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.d("WebView", "开始加载: " + url);
                // 开始加载时确保显示加载层（保险起见）
                if (loadingOverlay != null) {
                    loadingOverlay.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d("WebView", "页面加载完成: " + url);

                // 延迟 300ms 隐藏，确保首帧已渲染
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (loadingOverlay != null) {
                        loadingOverlay.setVisibility(View.GONE);
                    }
                }, 1000);
            }
            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                Log.d("WebView", "正在加载资源: " + url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                Log.e("WebView", "加载错误: " + error.getDescription() + " Code: " + error.getErrorCode());
                if (loadingOverlay != null) {
                    loadingOverlay.setVisibility(View.GONE);
                }
                // 可选：显示错误提示 Toast
//                 Toast.makeText(MainActivity.this, "加载失败，请检查网络", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                Log.e("WebView", "HTTP错误: " + errorResponse.getStatusCode() + " " + errorResponse);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // 在这里根据需要判断是否要拦截请求  返回 true 表示拦截请求，返回 false 表示不拦截请求
                String fileName = "";
                String downloadUrl = url;  // 默认下载地址
                try {
                    // 邮件附件下载
                    if (url.contains("gyl/workstation/mail/download_attachment")) {

                        if (url.contains("192.168.3.12:6080")) {
                            url = url.replace("192.168.3.12:6080", "oa.nsyy.com.cn:6080");
                        }
                        // 找到最后一个 '/' 的索引
                        int lastIndex = url.lastIndexOf('/');
                        // 截取最后一个 '/' 后面的部分
                        String base64String = url.substring(lastIndex + 1);
                        base64String = base64String.replace("&", "/");
                        String param = base64decode(base64String);
                        param = param.replace(" ", "");
                        String[] params = param.split("#");

                        if (params.length != 3) {
                            return false;
                        }

                        url = params[1];
                        String date = params[2];
                        date = date.replace(" ", "");
                        date = date.replace(":", "");
                        date = date.replace("-", "");

                        String originalFileName = params[0];
                        int dotIndex = originalFileName.lastIndexOf(".");
                        fileName = originalFileName.substring(0, dotIndex);
                        String extension = originalFileName.substring(dotIndex);

                        fileName = fileName + "-" +date + extension;
                    } else if (url.contains("att_download?save_path=")) {
                        if (url.contains("192.168.3.12:6080")) {
                            url = url.replace("192.168.3.12:6080", "oa.nsyy.com.cn:6080");
                        }

                        // 提取 save_path= 后面的 Base64 字符串，严格截取到第一个 & 为止
                        int startIndex = url.indexOf("save_path=") + "save_path=".length();
                        int endIndex = url.indexOf("&", startIndex);  // 查找第一个 &

                        String base64String;
                        if (endIndex != -1) {
                            // 有 &，截取到 & 前
                            base64String = url.substring(startIndex, endIndex);
                        } else {
                            // 没有 &，取到 URL 末尾
                            base64String = url.substring(startIndex);
                        }
                        // 去除可能的空格
                        base64String = base64String.trim();
                        // 如果字符串为空，直接返回不处理
                        if (base64String.isEmpty()) {
                            return false;
                        }
                        Log.i("WebView", "下载地址: " + url);

                        fileName = base64decode(base64String);
                        if (fileName.contains("/")) {
                            String[] tmp = fileName.split("/");
                            fileName = tmp[tmp.length - 1];
                        }
                    } else {
                        return false;
                    }

                    // 开始下载
                    startDownload(url, fileName);

                } catch (Exception e) {
                    // 捕获所有异常，防止崩溃
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
        });

        // 重写 javascript 的 alert 和 confirm 函数,弹窗界面更美观。
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
                b.setTitle("Alert");
                b.setMessage(message);
                b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result.confirm();
                    }
                });
                b.setCancelable(false);
                b.create().show();
                return true;
            }

            //设置响应js 的Confirm()函数
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
                b.setTitle("Confirm");
                b.setMessage(message);
                b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result.confirm();
                    }
                });
                b.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result.cancel();
                    }
                });
                b.create().show();
                return true;
            }

            // 处理文件选择请求
            @Override
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                // 如果已经有回调未处理，取消它
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = filePathCallback;

                // 创建Intent
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*"); // 所有文件类型

                // 可选：设置多选
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

                // 可选：限制文件类型
                // String[] mimeTypes = {"image/*", "application/pdf"};
                // intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

                try {
                    startActivityForResult(Intent.createChooser(intent, "选择文件"), REQUEST_CODE_FILE_CHOOSER);
                } catch (ActivityNotFoundException e) {
                    mFilePathCallback = null;
                    Log.d("WebView", "无法打开文件选择器");
                    Toast.makeText(MainActivity.this, "无法打开文件选择器", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });

        // 清除之前的缓存
        webView.clearCache(true);

        // 加载 南石OA
        webView.loadUrl(NsyyConfig.LOAD_RUL);
    }

    protected String base64decode(String encodedString) throws Exception {
        if (encodedString == null || encodedString.isEmpty()) {
            throw new IllegalArgumentException("Empty base64 string");
        }
        byte[] decodedBytes = Base64.decode(encodedString, Base64.URL_SAFE | Base64.NO_WRAP);
        return new String(decodedBytes, "UTF-8");  // 根据实际编码调整 charset
    }

    private void startDownload(String url, String fileName) {
        // ✅ App 私有下载目录（不需要任何存储权限）
        File downloadsDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (downloadsDir == null) {
            Toast.makeText(this, "无法访问下载目录", Toast.LENGTH_SHORT).show();
            return;
        }
        File file = new File(downloadsDir, fileName);

        // 已存在直接打开
        if (file.exists()) {
            openDownloadFile(file);
            return;
        }

        // ===== 下载中弹框 =====
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("下载中");
        builder.setMessage("正在下载 " + fileName + "，请稍候…");

        ProgressBar progressBar = new ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        builder.setView(progressBar);
        builder.setCancelable(false);

        downloadDialog = builder.create();
        downloadDialog.show();

        // 清理 URL 参数
        if (url.contains("mid")) {
            String midStr = url.replaceAll(".*[?&]mid=([^&]+).*", "$1");
            try {
                int mid = Integer.parseInt(midStr);
                MainActivity.getDatabaseHelper().updateDownloadFlag(mid);
            } catch (Exception e) {

            }
            url = url.replaceAll("[?&]mid=[^&]*", "").replaceAll("\\?$", "");
        } else if (url.contains("message_id")) {
            String midStr = url.replaceAll(".*[?&]message_id=([^&]+).*", "$1");
//            try {
//                int mid = Integer.parseInt(midStr);
//                MainActivity.getDatabaseHelper().updateDownloadFlag(mid);
//            } catch (Exception e) {
//
//            }
            url = url.replaceAll("[?&]message_id=[^&]*", "").replaceAll("\\?$", "");
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(fileName);
        request.setDescription("文件下载中");
        request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
        );

        // ✅ 指定 App 私有目录
        request.setDestinationUri(Uri.fromFile(file));

        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        long downloadId = dm.enqueue(request);

        // 定义超时时间，例如 5 分钟（300000 ms），可根据需求调整
        long timeoutMillis = 1 * 60 * 1000L;
        // 使用 Handler 延迟检查
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            Cursor cursor = dm.query(query);
            if (cursor != null && cursor.moveToFirst()) {
                @SuppressLint("Range") int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                if (status != DownloadManager.STATUS_SUCCESSFUL && status != DownloadManager.STATUS_FAILED) {
                    // 超时未完成，取消下载
                    dm.remove(downloadId);
                    Toast.makeText(MainActivity.this, "下载超时，已取消", Toast.LENGTH_SHORT).show();
                    // 可在这里处理超时逻辑，如重试或通知用户
                }
                cursor.close();
            }
        }, timeoutMillis);

        // 下载完成广播接收器
        String finalFileName = fileName;
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    // 隐藏正在下载的弹框
                    if (downloadDialog != null && downloadDialog.isShowing()) {
                        downloadDialog.dismiss();
                    }
                    unregisterReceiver(this);
                    openDownloadFile(file);
                }
            }
        };

        // 注册广播接收器
        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private void openDownloadFile(File file) {

        Uri uri = FileProvider.getUriForFile(
                this,
                "com.nsyy.Nsyy.fileprovider",
                file
        );

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        String name = file.getName().toLowerCase(Locale.ROOT);

        if (name.endsWith(".png") || name.endsWith(".jpg") ||
                name.endsWith(".jpeg") || name.endsWith(".gif") ||
                name.endsWith(".webp")) {

            intent.setDataAndType(uri, "image/*");

        } else if (name.endsWith(".pdf")) {

            intent.setDataAndType(uri, "application/pdf");

        } else if (name.endsWith(".doc") || name.endsWith(".docx")) {

            intent.setDataAndType(uri,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        } else if (name.endsWith(".xls") || name.endsWith(".xlsx")) {

            intent.setDataAndType(uri,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        } else {
            intent.setDataAndType(uri, "*/*");
        }

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "未找到可打开该文件的应用", Toast.LENGTH_SHORT).show();
        }
    }

    // 接管返回按键的响应
    @Override
    public void onBackPressed() {
        // 如果 WebView 可以返回，则返回上一页
        if (webView.canGoBack()) {
            webView.goBack();
            return;
        }
        FileHelper.RUN_IN_BACKGROUND = true;
        // 这里返回后台运行，而不是直接杀死
        moveTaskToBack(false);
//        // 否则退出应用程序
//        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        System.out.println("===> webview resume");
        FileHelper.RUN_IN_BACKGROUND = false;

        if (isAppInBackground()) {
            bringWebViewActivityToFront();
        }

        // 加 null 判断，防止首次同意前 webView 还没初始化
        if (webView != null) {
            webView.onResume();
        }
    }

    private boolean isAppInBackground() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;
            return !topActivity.getPackageName().equals(getPackageName());
        }
        return false;
    }

    private void bringWebViewActivityToFront() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        System.out.println("===> webview 暂停");
        FileHelper.RUN_IN_BACKGROUND = true;
        if (webView != null) {
            webView.onPause(); // 加 null 判断
        }

    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.destroy();
        }
        super.onDestroy();

        if (downloadDialog != null && downloadDialog.isShowing()) {
            downloadDialog.dismiss();
        }

        // 清理回调
        if (mFilePathCallback != null) {
            mFilePathCallback.onReceiveValue(null);
            mFilePathCallback = null;
        }

        FileHelper.RUN_IN_BACKGROUND = true;
        unregisterReceiver(nsyyServerBroadcastReceiver);
        stopService(new Intent(this, NsServerService.class));//停止服务

        // 注销广播接收器
        unregisterReceiver(noticeReceiver);

        if (permissionReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(permissionReceiver);
        }
    }


    @JavascriptInterface
    public void takePhoto(){
        String[] options = {"拍照", "从文件选择"};

        new AlertDialog.Builder(MainActivity.this)
                .setTitle("选择图片来源")
                .setCancelable(true)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else {
                        openFilePicker();
                    }
                })
                .show();
    }

    private void openCamera() {
        // 只检查相机权限（不再涉及任何存储权限）
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this)
                    .setTitle("权限请求")
                    .setMessage("需要访问相机权限，以便拍照上传附件或扫码")
                    .setPositiveButton("允许", (dialog, which) -> ActivityCompat.requestPermissions(
                            this, new String[]{Manifest.permission.CAMERA},
                            CAMERA_PERMISSION_REQUEST_CODE)
                    )
                    .setNegativeButton("拒绝", null)
                    .show();
            return;
        }

        String filename = "CAMERA_IMG_"
                + DateFormat.format("yyyyMMdd_HHmmss", Calendar.getInstance(Locale.CHINA))
                + ".jpg";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        cameraImageUri = getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (cameraImageUri == null) {
            Toast.makeText(this, "无法创建图片文件", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        try {
            startActivityForResult(intent, REQ_CAMERA);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "未找到相机应用", Toast.LENGTH_SHORT).show();
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES,
                new String[]{"image/jpeg", "image/png", "image/webp"});


        try {
            startActivityForResult(intent, REQ_PICK_IMAGE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "未找到文件选择器", Toast.LENGTH_SHORT).show();
        }

    }


    @JavascriptInterface
    public void scanCode(){
        // 接入华为统一扫码功能：https://developer.huawei.com/consumer/cn/doc/development/HMSCore-Guides/android-dev-process-0000001050043953
        // 官方案例： https://github.com/huaweicodelabs/ScanKit/blob/master/DefaultView-java/app/src/main/java/com/example/scankitdemo/MainActivity.java
        vivoScan(VivoQRCodeScanActivity.class);
//        if (manufacturer.equalsIgnoreCase("vivo") || manufacturer.equalsIgnoreCase("oppo") ||
//                manufacturer.equalsIgnoreCase("honor")) {
//            // vivo oppo 使用 zxing lite 扫码
//            vivoScan(VivoQRCodeScanActivity.class);
//        } else {
//            newViewBtnClick();
//        }
    }

    // 处理从Socket收到的消息
    public void onSocketMessageReceived(String message) {
        runOnUiThread(() -> {
            try {
                // 假设 message 已经是 JSON 字符串
                JSONObject jsonObj = new JSONObject(message);
                // 更安全的 JSON 序列化（推荐）
                String escapedJson = jsonObj.toString()
                        .replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n");

                webView.evaluateJavascript("javascript:handleSocketMessage('" + escapedJson + "')", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        System.out.println("handleSocketMessage: " + value);
                    }
                });
            } catch (JSONException e) {
                Log.e("WebView", "JSON解析失败", e);
                // 回退到字符串处理
                String safeMsg = message.replace("'", "\\'").replace("\"", "\\\"");
                String js = String.format("javascript:handleSocketMessage('%s')", safeMsg);
                webView.loadUrl(js);
            }
        });
    }

    private void vivoScan(Class<?> cls) {
        ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeCustomAnimation(this, R.anim.in, R.anim.out);
        Intent intent = new Intent(this, cls);
        ActivityCompat.startActivityForResult(this, intent, REQUEST_CODE_VIVO_SCAN, optionsCompat.toBundle());
    }


    /**
     * Call back the permission application result. If the permission application is successful, the barcode scanning view will be displayed.
     * @param requestCode Permission application code.
     * @param permissions Permission array.
     * @param grantResults: Permission application result array.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 请求文件权限
        if (requestCode == REQUEST_FILE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户授予了存储权限，开始下载
                Toast.makeText(MainActivity.this, "存储权限已获取，请重新点击下载", Toast.LENGTH_SHORT).show();
            } else {
                // 用户拒绝了存储权限，显示提示信息
                Toast.makeText(MainActivity.this, "没有存储权限，无法下载文件", Toast.LENGTH_SHORT).show();
            }
        }

        // 扫码-相机权限
        if (permissions == null || grantResults == null || grantResults.length < 2 || grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (requestCode == DEFAULT_VIEW) {
            //start ScankitActivity for scanning barcode
            ScanUtil.startScan(MainActivity.this, REQUEST_CODE_SCAN, new HmsScanAnalyzerOptions.Creator().setHmsScanTypes(HmsScan.ALL_SCAN_TYPE).create());
        }

    }

    /**
     * Event for receiving the activity result.
     *
     * @param requestCode Request code.
     * @param resultCode Result code.
     * @param data        Result.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //receive result after your activity finished scanning
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_FILE_CHOOSER) {
            if (mFilePathCallback == null) return;

            Uri[] results = null;
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    String dataString = data.getDataString();
                    ClipData clipData = data.getClipData();

                    if (clipData != null) {
                        // 多选
                        results = new Uri[clipData.getItemCount()];
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            results[i] = clipData.getItemAt(i).getUri();
                        }
                    } else if (dataString != null) {
                        // 单选
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }

            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;
        }

        if (resultCode != RESULT_OK ) {
            return;
        }

        // 处理拍照上传
        if (requestCode == REQ_CAMERA || requestCode == REQ_PICK_IMAGE) {
            Uri imageUri = null;

            if (requestCode == REQ_CAMERA) {
                imageUri = cameraImageUri;
            } else if (requestCode == REQ_PICK_IMAGE && data != null) {
                imageUri = data.getData();

                // ✅ 持久化权限（非常重要）
                if (imageUri != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(
                                imageUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (SecurityException e) {
                        Log.w(TAG, "无法持久化 URI 权限（可能不是 SAF 返回）", e);
                        Toast.makeText(MainActivity.this, "无法持久化 URI 权限（可能不是 SAF 返回）", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            if (imageUri == null) {
                Log.e(TAG, "imageUri is null");
                Toast.makeText(MainActivity.this, "图片保存失败", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                String base64 = compressAndEncodeImageFromUri(imageUri);
                base64 = base64.replace("\n", "");

                String json = String.format("{\"data\":\"%s\"}", base64);
                String js = "javascript:receiveCameraResult('" + json + "')";

                webView.evaluateJavascript(js, value ->
                        Log.d(TAG, "JS 回调成功: " + value)
                );

            } catch (Exception e) {
                Log.e(TAG, "图片处理失败", e);
                Toast.makeText(MainActivity.this, "图片处理失败", Toast.LENGTH_SHORT).show();
            }

        }

        // 处理扫码结果
        if (requestCode == REQUEST_CODE_SCAN) {
            Object object = data.getParcelableExtra(ScanUtil.RESULT);
            if (object instanceof HmsScan) {
                HmsScan obj = (HmsScan) object;
                if (obj != null) {
                    String retValue = obj.originalValue;
                    try {
                        String js = "javascript:receiveScanResult('" + retValue + "')";
                        System.out.println("开始执行 JS 方法：" + js);

                        webView.evaluateJavascript(js, new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String s) {
                                //将button显示的文字改成JS返回的字符串
                                System.out.println("成功接收到扫码返回值：" + s);
                            }
                        });
                    } catch (Exception e) {
                        System.out.println("未成功调用 JS 方法 handleScanResult");
                        e.printStackTrace();
                    }
                }
            }
        }

        if (requestCode == REQUEST_CODE_VIVO_SCAN) {
            String result = CameraScan.parseScanResult(data);
            if (result != null) {
                try {
                    String js = "javascript:receiveScanResult('" + result + "')";
                    System.out.println("开始执行 JS 方法：" + js);

                    webView.evaluateJavascript(js, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {
                            //将button显示的文字改成JS返回的字符串
                            System.out.println("成功接收到扫码返回值：" + s);
                        }
                    });
                } catch (Exception e) {
                    System.out.println("未成功调用 JS 方法 handleScanResult");
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 压缩图片
     * @return
     */
    private String compressAndEncodeImageFromUri(Uri uri) throws IOException {
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
        bitmap.recycle();

        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
    }
}