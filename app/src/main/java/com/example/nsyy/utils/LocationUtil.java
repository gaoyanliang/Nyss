package com.example.nsyy.utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.example.nsyy.permission.NsyyLocationListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 获取手机当前位置
 */
public class LocationUtil {
    private volatile static LocationUtil uniqueInstance;
    private LocationManager locationManager;
    private Context context;
    private AddressCallback addressCallback;
    private NsyyLocationListener locationListener;

    // 定位超时时间(秒)
    private static final int LOCATION_TIMEOUT = 30;
    // 最小定位精度(米)
    private static final int MIN_ACCURACY = 50;
    // 最小位置更新间隔(毫秒)
    private static final long MIN_TIME = 1000;
    // 最小位置变化距离(米)
    private static final float MIN_DISTANCE = 5;

    @Override
    public String toString() {
        return "LocationUtil{" +
                "locationManager=" + locationManager +
                ", mContext=" + context +
                ", addressCallback=" + addressCallback +
                ", locationListener=" + locationListener +
                '}';
    }

    private LocationUtil() {

    }

    //采用Double CheckLock(DCL)实现单例
    public static LocationUtil getInstance() {
        if (uniqueInstance == null) {
            synchronized (LocationUtil.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new LocationUtil();
                }
            }
        }
        return uniqueInstance;
    }

    public void setContext(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.locationListener = new NsyyLocationListener();

        this.addressCallback = new AddressCallback() {
            @Override
            public void onGetAddress(Address address) {
                String countryName = address.getCountryName(); //国家
                String adminArea = address.getAdminArea();     //省
                String locality = address.getLocality();       //市
                String subLocality = address.getSubLocality(); //区
                String featureName = address.getFeatureName(); //街道
                Log.e("定位地址: ", countryName + adminArea + locality + subLocality + featureName);
            }

            @Override
            public void onGetLocation(double lat, double lng) {
                Log.e("定位经纬度: ", lat + "\n" + lng);
            }
        };
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * 获取最新位置（强制刷新）
     */
    public Location getLocation(boolean turnOnGPS) {
        // 检查位置权限
        if (!checkLocationPermission()) {
            Log.e("LocationUtil", "Location permission not granted");
            return null;
        }

        // 尝试开启GPS
        if (turnOnGPS && !isGpsEnabled()) {
            initGPS();
        }

        // 使用原子引用保存最新位置
        AtomicReference<Location> bestLocation = new AtomicReference<>(null);
        CountDownLatch latch = new CountDownLatch(1);

        // 创建临时位置监听器
        NsyyLocationListener tempListener = new NsyyLocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location != null && (bestLocation.get() == null ||
                        location.getAccuracy() < bestLocation.get().getAccuracy())) {
                    bestLocation.set(location);

                    // 达到精度要求则停止等待
                    if (location.getAccuracy() <= MIN_ACCURACY) {
                        latch.countDown();
                    }
                }
            }
        };

        try {
            // 注册位置更新
            if (isGpsEnabled()) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME,
                        MIN_DISTANCE,
                        tempListener,
                        Looper.getMainLooper()
                );
            }

            if (isNetworkEnabled()) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME * 3, // 网络定位更新间隔稍长
                        MIN_DISTANCE * 2,
                        tempListener,
                        Looper.getMainLooper()
                );
            }

            // 同时尝试获取最后已知位置作为初始值
            Location lastGpsLoc = getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location lastNetLoc = getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (lastGpsLoc != null && (bestLocation.get() == null ||
                    lastGpsLoc.getAccuracy() < bestLocation.get().getAccuracy())) {
                bestLocation.set(lastGpsLoc);
            }

            if (lastNetLoc != null && (bestLocation.get() == null ||
                    lastNetLoc.getAccuracy() < bestLocation.get().getAccuracy())) {
                bestLocation.set(lastNetLoc);
            }

            // 等待获取足够精确的位置或超时
            latch.await(LOCATION_TIMEOUT, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            Log.e("LocationUtil", "Location update interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            // 确保移除监听器
            locationManager.removeUpdates(tempListener);
        }

        return bestLocation.get();
    }

    /**
     * 获取最后已知位置（不保证是最新的）
     */
    private Location getLastKnownLocation(String provider) {
        if (!checkLocationPermission()) {
            return null;
        }

        try {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null && location.getAccuracy() <= MIN_ACCURACY * 2) {
                return location;
            }
        } catch (SecurityException e) {
            Log.e("LocationUtil", "No location permission", e);
        }
        return null;
    }

    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isGpsEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private boolean isNetworkEnabled() {
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * 初始化GPS设置
     */
    public void initGPS() {
        if (!isGpsEnabled() && !isNetworkEnabled()) {
            openGPSDialog();
        }
    }

    /**
     * 打开GPS设置对话框
     */
    private void openGPSDialog() {
        new AlertDialog.Builder(context)
                .setTitle("提示")
                .setMessage("打开定位功能，可以提高定位精确度。\n请点击\"设置\"-\"定位服务\"-打开定位功能。")
                .setPositiveButton("设置", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    context.startActivity(intent);
                })
                .setNeutralButton("取消", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * 将位置转换为地址
     */
    public String getAddress(Location location) {
        if (location == null) {
            return "unknown address";
        }

        Geocoder gc = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> locationList = gc.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1
            );

            if (locationList != null && !locationList.isEmpty()) {
                Address address = locationList.get(0);
                StringBuilder retAddress = new StringBuilder();

                for (int i = 0; address.getAddressLine(i) != null; i++) {
                    retAddress.append(address.getAddressLine(i));
                    Log.d("AddressLine", address.getAddressLine(i));
                }

                if (addressCallback != null) {
                    addressCallback.onGetAddress(address);
                    addressCallback.onGetLocation(
                            location.getLatitude(),
                            location.getLongitude()
                    );
                }

                return retAddress.toString();
            }
        } catch (IOException e) {
            Log.e("LocationUtil", "Geocoder error", e);
        }
        return null;
    }

    public interface AddressCallback {
        void onGetAddress(Address address);
        void onGetLocation(double lat, double lng);
    }
}

//    /**
//     * 将 location 转换为具体地址 TODO 这里需要根据前端需求确定返回类型
//     * @param location
//     * @return
//     */
//    public String getAddress(Location location) {
//        if (location == null) {
//            return "unknown address";
//        }
//
//        //Geocoder通过经纬度获取具体信息
//        Geocoder gc = new Geocoder(context, Locale.getDefault());
//        try {
//            List<Address> locationList = gc.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
//
//            String ret_address = "";
//            if (locationList != null && locationList.size() > 0) {
//                Address address = locationList.get(0);
////                String countryName = address.getCountryName();//国家
////                String countryCode = address.getCountryCode();
////                String adminArea = address.getAdminArea();//省
////                String locality = address.getLocality();//市
////                String subLocality = address.getSubLocality();//区
////                String featureName = address.getFeatureName();//街道
//
//                for (int i = 0; address.getAddressLine(i) != null; i++) {
//                    ret_address = ret_address + address.getAddressLine(i);
//                    String addressLine = address.getAddressLine(i);
//                    System.out.println("addressLine=====" + addressLine);
//                }
//                if(addressCallback != null){
//                    addressCallback.onGetAddress(address);
//                }
//                return ret_address;
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }



