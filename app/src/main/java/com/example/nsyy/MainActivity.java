package com.example.nsyy;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import com.example.nsyy.common.GPSUtils;
import com.example.nsyy.permission.NotificationMonitorService;
import com.example.nsyy.permission.PermissionInterceptor;
import com.example.nsyy.permission.PermissionNameConvert;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.hjq.toast.Toaster;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 申请定位权限
        findViewById(R.id.btn_main_request_location_permission).setOnClickListener(this);

        // 获取位置信息
        findViewById(R.id.btn_main_request_location).setOnClickListener(this);

        // 申请通知权限
        findViewById(R.id.btn_main_request_notification_service_permission).setOnClickListener(this);

        // 申请新版通知权限
        findViewById(R.id.btn_main_request_post_notification).setOnClickListener(this);

        // 申请通知栏监听权限
        findViewById(R.id.btn_main_request_bind_notification_listener_permission).setOnClickListener(this);

        // 跳转到应用详情页面
        findViewById(R.id.btn_main_start_permission_activity).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.btn_main_request_location_permission) {

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

        } else if (viewId == R.id.btn_main_request_location) {

            // 判断是否已经获取位置权限，没有获取先获取位置权限
            if (XXPermissions.isGranted(this, new String[] {
                    Permission.ACCESS_COARSE_LOCATION,
                    Permission.ACCESS_FINE_LOCATION,
                    Permission.ACCESS_BACKGROUND_LOCATION
            })) {

                String location = GPSUtils.getInstance().getProvince(this);
                toast(location);

            } else {
                toast("未获取位置权限,请先获取权限!");
            }

        } else if (viewId == R.id.btn_main_request_notification_service_permission) {

            XXPermissions.with(this)
                    .permission(Permission.NOTIFICATION_SERVICE)
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

        } else if (viewId == R.id.btn_main_request_post_notification) {

            long delayMillis = 0;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                delayMillis = 2000;

            }

            view.postDelayed(new Runnable() {

                @Override
                public void run() {
                    XXPermissions.with(MainActivity.this)
                            .permission(Permission.POST_NOTIFICATIONS)
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
            }, delayMillis);

        } else if (viewId == R.id.btn_main_request_bind_notification_listener_permission) {

            XXPermissions.with(this)
                    .permission(Permission.BIND_NOTIFICATION_LISTENER_SERVICE)
                    .interceptor(new PermissionInterceptor())
                    .request(new OnPermissionCallback() {

                        @Override
                        public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                            if (!allGranted) {
                                return;
                            }
                            toast(String.format(getString(R.string.demo_obtain_permission_success_hint),
                                    PermissionNameConvert.getPermissionString(MainActivity.this, permissions)));
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                                toggleNotificationListenerService();
                            }
                        }
                    });


        } else if (viewId == R.id.btn_main_start_permission_activity) {
            XXPermissions.startPermissionActivity(this);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void toggleNotificationListenerService() {
        PackageManager packageManager = getPackageManager();
        packageManager.setComponentEnabledSetting(
                new ComponentName(this, NotificationMonitorService.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

        packageManager.setComponentEnabledSetting(
                new ComponentName(this, NotificationMonitorService.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    public void toast(CharSequence text) {
        Toaster.show(text);
    }
}