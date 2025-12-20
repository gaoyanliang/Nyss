package com.nsyy.nsyy.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.nsyy.Nsyy.R;
import com.nsyy.nsyy.MainActivity;
import com.nsyy.nsyy.permission.PermissionInterceptor;
import com.nsyy.nsyy.permission.PermissionNameConvert;
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
        if (!XXPermissions.isGranted(context, new String[]{
                Permission.BLUETOOTH_SCAN,
                Permission.BLUETOOTH_CONNECT})) {

            XXPermissions.with(context)
                    .permission(Permission.BLUETOOTH_SCAN)
                    .permission(Permission.BLUETOOTH_CONNECT)
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
        // 判断是否已经获取位置权限，没有获取先获取位置权限
        if (!XXPermissions.isGranted(mContext, new String[]{
                Permission.ACCESS_COARSE_LOCATION,
                Permission.ACCESS_FINE_LOCATION})) {

            new android.app.AlertDialog.Builder(mContext)
                    .setTitle("需要位置权限")
                    .setMessage("为了使用签到、定位考勤功能，本应用需要访问您的位置信息。\n\n我们仅在您使用相关功能时获取位置，不会后台持续定位。")
                    .setPositiveButton("去开启", (dialog, which) -> {
                        XXPermissions.with(mContext)
                                .permission(Permission.ACCESS_COARSE_LOCATION)
                                .permission(Permission.ACCESS_FINE_LOCATION)
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
                    })
                    .setNegativeButton("暂不开启", (dialog, which) -> {
                        Toast.makeText(mContext, "未开启位置权限，定位打卡功能将无法使用", Toast.LENGTH_LONG).show();
                    })
                    .setCancelable(false)  // 防止用户直接关闭对话框
                    .show();


        }

    }


    /**
     * 检测是否开启通知
     *
     * @param context
     */
    public static void checkNotification(final Context context) {
        if (!NotificationUtil.isNotifyEnabled(context)) {
            new AlertDialog.Builder(context).setTitle("需要通知权限")
                    .setMessage("为了及时接收 OA 重要消息、审批提醒、公告通知等推送，本应用需要发送通知权限。\n\n您可以随时在系统设置中关闭。")
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
