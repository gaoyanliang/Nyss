package com.nsyy.nsyy.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class StoragePermissionHelper {
    private static final int STORAGE_PERMISSION_CODE = 1001;
    private static final int MANAGE_STORAGE_CODE = 1002;
    private Activity activity;

    public StoragePermissionHelper(Activity activity) {
        this.activity = activity;
    }

    // 检查存储权限（适配版本）
    public boolean hasStoragePermission() {
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {  // Android 11+
            return Environment.isExternalStorageManager();  // MANAGE_EXTERNAL_STORAGE
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {  // Android 10
            // Scoped Storage，默认有 app 私有访问；若需全访问，用 MANAGE
            return true;  // 或检查旧权限，但无效
        } else {  // Android 9 及以下
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    // 请求权限（适配版本）
    public void requestStoragePermission() {
        if (hasStoragePermission()) {
            // 有权限，回调成功
            onPermissionGranted();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+：跳转全文件访问设置
            if (!Environment.isExternalStorageManager()) {
                showPermissionDialog("需要文件管理权限，请在设置中开启");
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivityForResult(intent, MANAGE_STORAGE_CODE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+：颗粒化权限
            String[] permissions = {
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
            };
            ActivityCompat.requestPermissions(activity, permissions, STORAGE_PERMISSION_CODE);
        } else {
            // Android 6-9：运行时权限
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(activity, permissions, STORAGE_PERMISSION_CODE);
        }
    }

    // 无权限提示并跳转设置
    private void showPermissionDialog(String message) {
        Toast.makeText(activity, message + "\n请手动开启存储权限", Toast.LENGTH_LONG).show();
        jumpToVendorSettings();  // 厂商适配跳转
    }

    // 厂商适配：检测系统并跳转自定义设置页
    private void jumpToVendorSettings() {
        String systemVersion = Build.MANUFACTURER.toLowerCase();  // 或用 getProp("ro.build.display.id")
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.parse("package:" + activity.getPackageName());
        intent.setData(uri);

        switch (systemVersion) {
            case "huawei":  // EMUI/HarmonyOS
                intent.setAction("com.huawei.permissionmanager.activity.PermissionManagerActivity");  // 权限管理页
                break;
            case "xiaomi":  // MIUI/HyperOS
                intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity");  // 权限编辑页
                break;
            case "oppo":  // ColorOS
                intent.setClassName("com.coloros.securityguard", "com.coloros.privacypermissions.ui.activity.PermissionsManagerActivity");  // 隐私权限页
                break;
            case "vivo":  // Funtouch OS
                intent.setClassName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.PermissionManagerActivity");  // 权限管理（或用 i Manager）
                break;
            default:  // 标准 Android（如 Samsung、Google）
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                break;
        }
        try {
            activity.startActivity(intent);
        } catch (Exception e) {
            // 备用：标准设置页
            Intent fallback = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            fallback.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(fallback);
        }
    }

    // 权限回调（在 Activity 的 onRequestPermissionsResult 和 onActivityResult 中调用）
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted();
            } else {
                showPermissionDialog("权限被拒绝，无法访问存储");
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MANAGE_STORAGE_CODE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                onPermissionGranted();
            } else {
                showPermissionDialog("未开启文件管理权限，请重试");
            }
        }
    }

    // 权限成功回调（替换为你的业务逻辑，如下载文件）
    private void onPermissionGranted() {
        Toast.makeText(activity, "存储权限已获取", Toast.LENGTH_SHORT).show();
        // TODO: 执行存储操作
    }
}
