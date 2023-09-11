package com.example.nsyy.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.nsyy.R;
import com.example.nsyy.permission.PermissionInterceptor;
import com.example.nsyy.permission.PermissionNameConvert;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.hjq.toast.Toaster;

import java.util.List;

public class PermissionUtil {
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

    public static void toast(CharSequence text) {
        Toaster.show(text);
    }
}
