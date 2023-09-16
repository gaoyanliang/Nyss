package com.example.nsyy.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.example.nsyy.MainActivity;
import com.example.nsyy.R;
import com.example.nsyy.permission.PermissionInterceptor;
import com.example.nsyy.permission.PermissionNameConvert;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.hjq.toast.Toaster;

import java.util.List;

public class PermissionUtil {

    /**
     * 检查蓝牙权限是否开启
     *
     * 注意： 旧版本的需要定位权限才能进行扫描蓝牙
     * @param context
     */
    public static void checkBlueToothPermission(Context context) {

//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
//            toast(context.getString(R.string.demo_android_12_bluetooth_permission_hint));
//        }

        if (!XXPermissions.isGranted(context, new String[]{
                Permission.BLUETOOTH_SCAN,
                Permission.BLUETOOTH_CONNECT,
                Permission.BLUETOOTH_ADVERTISE})) {

            XXPermissions.with(context)
                    .permission(Permission.BLUETOOTH_SCAN)
                    .permission(Permission.BLUETOOTH_CONNECT)
                    .permission(Permission.BLUETOOTH_ADVERTISE)
                    .interceptor(new PermissionInterceptor())
                    .request(new OnPermissionCallback() {

                        @Override
                        public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                            if (!allGranted) {
                                return;
                            }
                            toast(String.format(context.getString(R.string.demo_obtain_permission_success_hint),
                                    PermissionNameConvert.getPermissionString(context, permissions)));
                        }
                    });
        }


    }

    public static void checkLocationPermission(Context mContext) {

        /**
         * Permission.ACCESS_BACKGROUND_LOCATION  在后台获取位置（Android 10.0 新增的权限）
         * 需要注意的是：
         * 1. 一旦你申请了该权限，在授权的时候，需要选择《始终允许》，而不能选择《仅在使用中允许》
         * 2. 如果你的 App 只在前台状态下使用定位功能，没有在后台使用的场景，请不要申请该权限
         */
        // 判断是否已经获取位置权限，没有获取先获取位置权限
        if (!XXPermissions.isGranted(mContext, new String[]{
                Permission.ACCESS_COARSE_LOCATION,
                Permission.ACCESS_FINE_LOCATION,
        Permission.ACCESS_BACKGROUND_LOCATION})) {
            XXPermissions.with(mContext)
                    .permission(Permission.ACCESS_COARSE_LOCATION)
                    .permission(Permission.ACCESS_FINE_LOCATION)
                    .permission(Permission.ACCESS_BACKGROUND_LOCATION)
                    .interceptor(new PermissionInterceptor())
                    .request(new OnPermissionCallback() {

                        @Override
                        public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                            if (!allGranted) {
                                return;
                            }
                            toast(String.format(mContext.getString(R.string.demo_obtain_permission_success_hint),
                                    PermissionNameConvert.getPermissionString(mContext, permissions)));
                        }
                    });
        }

    }


    /**
     * 检测是否开启通知
     *
     * @param context
     */
    public static void checkNotification(final Context context) {
        if (!NotificationUtil.isNotifyEnabled(context)) {
            new AlertDialog.Builder(context).setTitle("温馨提示")
                    .setMessage("你还未开启系统通知，将影响消息的接收，要去开启吗？")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setNotification(context);
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
     * @param context
     */
    private static void setNotification(final Context context) {
        String packageName = context.getPackageName();
        Intent localIntent = new Intent();
        //直接跳转到应用通知设置的代码：
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//8.0及以上
            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            localIntent.setData(Uri.fromParts("package", packageName, null));
        } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//5.0以上到8.0以下
            localIntent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            localIntent.putExtra("app_package", packageName);
            localIntent.putExtra("app_uid", context.getApplicationInfo().uid);
        } else if (android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {//4.4
            localIntent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            localIntent.addCategory(Intent.CATEGORY_DEFAULT);
            localIntent.setData(Uri.parse("package:" + packageName));
        } else {
            //4.4以下没有从app跳转到应用通知设置页面的Action，可考虑跳转到应用详情页面,
            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= 9) {
                localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                localIntent.setData(Uri.fromParts("package", packageName, null));
            } else if (Build.VERSION.SDK_INT <= 8) {
                localIntent.setAction(Intent.ACTION_VIEW);
                localIntent.setClassName("com.android.settings", "com.android.setting.InstalledAppDetails");
                localIntent.putExtra("com.android.settings.ApplicationPkgName", packageName);
            }
        }
        context.startActivity(localIntent);
    }





















    public static void toast(CharSequence text) {
        Toaster.show(text);
    }
}
