package com.example.nsyy.permission;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import com.example.nsyy.MainActivity;
import com.example.nsyy.R;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.hjq.toast.Toaster;

import java.io.IOException;
import java.util.List;

public class PermissionActivity extends AppCompatActivity implements View.OnClickListener {

    private LocationManager locationManager;
    private String bestProvider;

    private NsyyLocationListener nsyyLocationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);

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
                                    PermissionNameConvert.getPermissionString(PermissionActivity.this, permissions)));
                        }
                    });

        } else if (viewId == R.id.btn_main_request_location) {

            // 判断是否已经获取位置权限，没有获取先获取位置权限
            if (XXPermissions.isGranted(this, new String[]{
                    Permission.ACCESS_COARSE_LOCATION,
                    Permission.ACCESS_FINE_LOCATION,
                    Permission.ACCESS_BACKGROUND_LOCATION})) {

                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

                // get the best provider depending on the criteria
                Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_FINE);
                criteria.setCostAllowed(false);
                bestProvider = locationManager.getBestProvider(criteria, false);

                nsyyLocationListener = new NsyyLocationListener();


                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }

                Location location = locationManager.getLastKnownLocation(bestProvider);

                // 如果存在位置 更新
                if (location != null) {
                    nsyyLocationListener.onLocationChanged(location);

                    getAddress(this, location.getLatitude(), location.getLongitude());
                } else {
                    // 打开 GPS
                    openGPS();

                    // location updates: at least 1 meter and 200millsecs change
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        locationManager.requestLocationUpdates(bestProvider, 200, 1, nsyyLocationListener);
                    }
                }
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
                                    PermissionNameConvert.getPermissionString(PermissionActivity.this, permissions)));
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
                    XXPermissions.with(PermissionActivity.this)
                            .permission(Permission.POST_NOTIFICATIONS)
                            .interceptor(new PermissionInterceptor())
                            .request(new OnPermissionCallback() {

                                @Override
                                public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                                    if (!allGranted) {
                                        return;
                                    }
                                    toast(String.format(getString(R.string.demo_obtain_permission_success_hint),
                                            PermissionNameConvert.getPermissionString(PermissionActivity.this, permissions)));
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
                                    PermissionNameConvert.getPermissionString(PermissionActivity.this, permissions)));
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                                toggleNotificationListenerService();
                            }
                        }
                    });


        } else if (viewId == R.id.btn_main_start_permission_activity) {
            XXPermissions.startPermissionActivity(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != XXPermissions.REQUEST_CODE) {
            return;
        }
        toast(getString(R.string.demo_return_activity_result_hint));
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

    // ------------------- 位置信息获取相关 -------------------

    private void openGPS() {
        //判断GPS是否开启，没有开启，则开启并更新位置信息
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){

            // leads to the settings because there is no last known location
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);

            //Using 10 seconds timer till it gets location
            final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("正在更新位置，请稍后 ...");
            alertDialog.setMessage("00:10");
            alertDialog.show();

            new CountDownTimer(10000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    alertDialog.setMessage("00:" + (millisUntilFinished / 1000));
                }

                @Override
                public void onFinish() {
                    alertDialog.dismiss();
                }
            }.start();
        }
    }

    private void getAddress(Activity activity, double latitude, double longitude) {
        List<Address> addList = null;

        try {
            Geocoder ge = new Geocoder(activity);
            addList = ge.getFromLocation(latitude, longitude, 1);
        } catch (IOException e) {
            Log.i("GPS: ", "获取定位信息有误！");
            e.printStackTrace();
        }

        if (addList != null && addList.size() > 0) {

            Address address = addList.get(0);
            //获取国家名称
            String countryName = address.getCountryName();

            //返回地址的国家代码，CN
            String countryCode = address.getCountryCode();
            Log.d("TAG", "getAddress: "+countryCode);

            //对应的省或者市
            String adminArea = address.getAdminArea();
            //一个市对应的具体的区
            String subLocality = address.getSubLocality();
            //具体镇名加具体位置
            String featureName = address.getFeatureName();
            //返回一个具体的位置串，这个就不用进行拼接了。
            String addressLines =address.getAddressLine(0);
            String specificAddress = countryName + adminArea + subLocality + featureName;

            Log.d("TAG", "addressLines: "+addressLines);
            Log.d("TAG", "specificAddress: "+specificAddress);

            toast(addressLines);
        } else {
            toast("获取位置信息有误，请重试");
        }
    }
}