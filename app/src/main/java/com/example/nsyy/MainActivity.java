package com.example.nsyy;

import android.Manifest;
import android.app.ActivityManager;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;
import android.widget.Toast;
import androidx.core.app.ActivityOptionsCompat;

import android.app.AlertDialog;
import android.content.DialogInterface;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.util.Base64;

import com.example.nsyy.alarm.LongRunningService;
import com.example.nsyy.config.MySharedPreferences;
import com.example.nsyy.message.FileHelper;
import com.example.nsyy.service.NsServerService;
import com.example.nsyy.service.NsyyServerBroadcastReceiver;
import com.example.nsyy.utils.AppVersionUtil;
import com.example.nsyy.utils.BlueToothUtil;
import com.example.nsyy.utils.LocationUtil;
import com.example.nsyy.utils.NotificationUtil;
import com.example.nsyy.utils.PermissionUtil;

import com.example.nsyy.vivo_scan.VivoQRCodeScanActivity;
import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;
import com.king.camera.scan.CameraScan;

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

//    private static String LOAD_RUL = "http://192.168.124.14:6060";
    private static String LOAD_RUL = "http://oa.nsyy.com.cn:6060";

    private WebView webView;
//    private SwipeRefreshLayout swipeRefreshLayout;

    public static String last_camera_img_name = null;
    private final static int CAMERA_FILE_RESULT_CODE = 10001;

    private String manufacturer = Build.MANUFACTURER;
    private String model = Build.MODEL;

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
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AppVersionUtil.getInstance().init(this);
        MySharedPreferences.init(this);
        // 检查权限: 这里需要开启位置权限 & 位置服务
        PermissionUtil.checkLocationPermission(this);

        // 消息通知
        PermissionUtil.checkNotification(this);
        NotificationUtil.getInstance().setContext(this);
        NotificationUtil.getInstance().initNotificationChannel();

        FileHelper.getInstance().setContext(this);

        // 检查是否开启位置服务
        LocationUtil.getInstance().setContext(this);
        LocationUtil.getInstance().initGPS();

        // 检查是否开启蓝牙权限 & 初始化
        PermissionUtil.checkBlueToothPermission(this);
        BlueToothUtil.getInstance().init(this);

        // 初始化 WebView
        webView = findViewById(R.id.webView);
        loadWebsite(2);
//        String loadUrl = MySharedPreferences.getSharedPreferences().getString("load_url", "");
//        if (!loadUrl.isEmpty() && !loadUrl.equals("")) {
//            LOAD_RUL = loadUrl;
//            initView();
//        } else {
//            // Ask the user to choose a website
//            showWebsiteChooserDialog();
//        }

        // 启动定时任务 每十分钟打印一次时间
        Intent intent = new Intent(this, LongRunningService.class);
        startService(intent);

        // 启动 web server
        registerReceiver(nsyyServerBroadcastReceiver, new IntentFilter("NsyyServerBroadcastReceiver"));
        startService(new Intent(this, NsServerService.class));

        // 注册广播接收器
        registerReceiver(noticeReceiver, new IntentFilter("LOAD_TARGET_PAGE"));
    }


    private void showWebsiteChooserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择要访问的网站：")
                .setItems(R.array.website_choices, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Load the selected website
                        loadWebsite(which);
                    }
                });

        // Create and show the alert dialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void loadWebsite(int choice) {
//        // Array of websites
//        String[] websites = getResources().getStringArray(R.array.websites);
//
//        if (choice >= 0 && choice < websites.length) {
//            String selectedWebsite = websites[choice];
//            LOAD_RUL = selectedWebsite;
//            initView();
//        } else {
//            LOAD_RUL = "http://oa.nsyy.com.cn:6060";
//            initView();
//        }
        SharedPreferences.Editor editor = MySharedPreferences.getSharedPreferences().edit();
        editor.putString("load_url", LOAD_RUL);
        editor.apply();

        initView();
    }

    private void initView() {
//        swipeRefreshLayout = findViewById(R.id.refreshLayout);
//        // 配置 SwipeRefreshLayout
//        swipeRefreshLayout.setOnRefreshListener(null);
//        // 隐藏加载图标
//        swipeRefreshLayout.setRefreshing(false);
//        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
//            @Override
//            public void onRefresh() {
//                // 检查 WebView 是否为空
//                if (webView == null) {
//                    swipeRefreshLayout.setRefreshing(false);
//                    return;
//                }
//                // 在 UI 线程上执行 WebView 刷新
//                new Handler().post(new Runnable() {
//                    @Override
//                    public void run() {
//                        webView.reload();
//                    }
//                });
//            }
//        });

        if (webView == null) {
            webView = findViewById(R.id.webView);
        }

        // Enable Javascript
        WebSettings webSettings = webView.getSettings();
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        // 设置允许JS弹窗
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        // 设置 WebView 允许执行 JavaScript 脚本
        webSettings.setJavaScriptEnabled(true);

        // Add the JavaScriptInterface to the WebView
        webView.addJavascriptInterface(this, "AndroidInterface");

        webSettings.setAllowContentAccess(true); // 是否可访问Content Provider的资源，默认值 true
        webSettings.setAllowFileAccess(true);    // 是否可访问本地文件，默认值 true

        // 确保跳转到另一个网页时仍然在当前 WebView 中显示,而不是调用浏览器打开
        webView.setWebViewClient(new WebViewClient() {
//            public void onPageFinished(WebView view, String url) {
//                swipeRefreshLayout.setRefreshing(false);
//            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // 在这里根据需要判断是否要拦截请求  返回 true 表示拦截请求，返回 false 表示不拦截请求

                String fileName = "";
                // 邮件附件下载
                if (url.contains("gyl/workstation/mail/download_attachment")) {

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
                    // app 安装包下载
                    String base64String = url.substring(url.indexOf("save_path=") + "save_path=".length());
                    fileName = base64decode(base64String);
                } else {
                    return false;
                }

                // 请求存储权限
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_FILE_PERMISSION_CODE);
                } else {
                    // 下载文件
                    startDownload(url, fileName);
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
        });

        // 清除之前的缓存
        webView.clearCache(true);
        // 加载 南石OA
        webView.loadUrl(LOAD_RUL);
    }

    protected String base64decode(String encodedString) {
        // 解码 Base64 编码的字符串
        byte[] decodedBytes = new byte[0];
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            decodedBytes = java.util.Base64.getDecoder().decode(encodedString);
        }

        // 将字节数组转换为字符串
        String decodedString = new String(decodedBytes);
        return decodedString;
    }

    private void startDownload(String url, String fileName) {

        // 获取下载目录
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        // 形成完整的文件地址
        File file = new File(downloadsDir, fileName);
        // 判断文件是否存在
        if (file.exists()) {
            // 在此处处理下载完成后 跳转到文件管理器查看下载的文件
            openDownloadFile(fileName);
            return;
        }

        // 获取文件扩展名
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription("Downloading file...");
        request.setTitle(fileName);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        long downloadId = dm.enqueue(request);

        // 下载完成广播接收器
        String finalFileName = fileName;
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    openDownloadFile(finalFileName);
                }
            }
        };

        // 注册广播接收器
        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private void openDownloadFile(String fileName) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);

        Uri uri = FileProvider.getUriForFile(webView.getContext(), "com.example.nsyy.fileprovider", file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (fileName.toLowerCase().endsWith("png") || fileName.toLowerCase().endsWith("jpg") || fileName.toLowerCase().endsWith("jpeg")
                || fileName.toLowerCase().endsWith("gif") || fileName.toLowerCase().endsWith("webp")) {
            intent.setDataAndType(uri, "image/*");
        } else if (fileName.toLowerCase().endsWith("pdf")) {
            intent.setDataAndType(uri, "application/pdf");
        } else if (fileName.toLowerCase().endsWith(".docx") || fileName.toLowerCase().endsWith(".doc")) {
            intent.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        } else if (fileName.toLowerCase().endsWith(".xlsx") || fileName.toLowerCase().endsWith("xls")) {
            intent.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else if (fileName.toLowerCase().endsWith(".apk")) {
            // 打开安装包
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            //intent.setDataAndType(uri, "application/vnd.android.package-archive");
            // 设置 Intent 的 flags 为 FLAG_ACTIVITY_NEW_TASK，表示新建一个任务进行安装
            //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri downloadUri = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath());
            intent.setDataAndType(downloadUri, "*/*");// 设置要显示的文件类型为所有类型
        }
        webView.getContext().startActivity(intent);
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

        webView.onResume();
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
        webView.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        webView.destroy();
        FileHelper.RUN_IN_BACKGROUND = true;
        unregisterReceiver(nsyyServerBroadcastReceiver);
        stopService(new Intent(this, NsServerService.class));//停止服务

        // 注销广播接收器
        unregisterReceiver(noticeReceiver);
    }


    @JavascriptInterface
    public void takePhoto(){
        // 检查是否已经获取相机权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // 请求相机权限 和 存储权限
            String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(this, permissions, CAMERA_PERMISSION_REQUEST_CODE);
        }

        String filename = "CAMERA_IMG_" + DateFormat.format("yyyyMMdd_hhmmss", Calendar.getInstance(Locale.CHINA)) + ".jpg";
        // 更新文件名字
        last_camera_img_name = filename;

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
        Uri imageUri = FileProvider.getUriForFile(webView.getContext(), "com.example.nsyy.fileprovider", file);

        Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            captureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); //添加这一句表示对目标应用临时授权该Uri所代表的文件
        }
        captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(captureIntent, CAMERA_FILE_RESULT_CODE);
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

    private void vivoScan(Class<?> cls) {
        ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeCustomAnimation(this, R.anim.in, R.anim.out);
        Intent intent = new Intent(this, cls);
        ActivityCompat.startActivityForResult(this, intent, REQUEST_CODE_VIVO_SCAN, optionsCompat.toBundle());
    }

    /**
     * Call the customized view.
     */
    public void newViewBtnClick() {
        // CAMERA_REQ_CODE为用户自定义，用于接收权限校验结果的请求码
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.requestPermissions(
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE},
                    DEFAULT_VIEW);
        }
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
        if (resultCode != RESULT_OK ) {
            return;
        }

        // 处理拍照上传
        if (requestCode == CAMERA_FILE_RESULT_CODE && data == null) {
            // 拍照结果 TODO 通过 js 返回
            String base64Str = compressAndEncodeImage();
            System.out.println(base64Str);
            base64Str = base64Str.replace("\n", "");
            String jsonString = "{\"data\": \"%s\"}";
            String jsonWithUrlSafeBase64 = String.format(jsonString, base64Str);

            try {
                String js = "javascript:receiveCameraResult('" + jsonWithUrlSafeBase64 + "')";
                System.out.println("开始执行 JS 方法：" + js);
                webView.evaluateJavascript(js, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String s) {
                        //将button显示的文字改成JS返回的字符串
                        System.out.println("成功接收到扫码返回值：" + s);
                    }
                });
            } catch (Exception e) {
                System.out.println("未成功调用 JS 方法 handleCameraResult");
                e.printStackTrace();
                // Handle the exception
            }
        }

        // 处理扫码结果
        if (requestCode == REQUEST_CODE_SCAN) {
            Object object = data.getParcelableExtra(ScanUtil.RESULT);
            if (object instanceof HmsScan) {
                HmsScan obj = (HmsScan) object;
                if (obj != null) {
                    String retValue = obj.originalValue;
                    Toast.makeText(this, retValue, Toast.LENGTH_SHORT).show();

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

                        //webView.loadUrl("javascript:handleScanResult('" + retValue + "')");
                    } catch (Exception e) {
                        System.out.println("未成功调用 JS 方法 handleScanResult");
                        e.printStackTrace();
                        // Handle the exception
                    }
                }
            }
        }

        if (requestCode == REQUEST_CODE_VIVO_SCAN) {
            String result = CameraScan.parseScanResult(data);
            if (result != null) {
                Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
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

                    //webView.loadUrl("javascript:handleScanResult('" + retValue + "')");
                } catch (Exception e) {
                    System.out.println("未成功调用 JS 方法 handleScanResult");
                    e.printStackTrace();
                    // Handle the exception
                }
            }
        }
    }


    /**
     * 压缩图片
     * @return
     */
    public static String compressAndEncodeImage() {
        // 压缩图片
        Bitmap compressedBitmap = null;
        try {
            compressedBitmap = PhotoUtils.getBitmapFromFile(last_camera_img_name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 将压缩后的 Bitmap 对象转换为 Base64 字符串
        String base64Image = bitmapToBase64(compressedBitmap);
        compressedBitmap.recycle();
        return base64Image;
    }

    private static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

}