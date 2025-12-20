package com.nsyy.nsyy.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.huawei.hms.location.FusedLocationProviderClient;
import com.huawei.hms.location.LocationRequest;
import com.huawei.hms.location.LocationResult;
import com.huawei.hms.location.LocationServices;
// 注意：这里不要导入 com.huawei.hms.location.LocationCallback

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationUtil {
    private static final String TAG = "LocationUtil";
    private static volatile LocationUtil instance;

    private Context context;
    private FusedLocationProviderClient fusedClient;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // 缓存结果
    public static class CachedResult {
        public final String address;
        public final double lat, lng;
        public final float accuracy;
        public final long timestamp;

        public CachedResult(String address, double lat, double lng, float accuracy) {
            this.address = address;
            this.lat = lat;
            this.lng = lng;
            this.accuracy = accuracy;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isValid() {
            return System.currentTimeMillis() - timestamp < 3 * 60 * 1000; // 3分钟
        }
    }

    private volatile CachedResult cachedResult;

    private LocationUtil() {

    }

    public void setContext(Context context) {
        this.context = context.getApplicationContext();
        this.fusedClient = LocationServices.getFusedLocationProviderClient(this.context);

        // app上架不合规 不能静默状态下持续采集精确位置信息       startBackgroundUpdates();
    }

    public static LocationUtil getInstance() {
        if (instance == null) {
            synchronized (LocationUtil.class) {
                if (instance == null) {
                    instance = new LocationUtil();
                }
            }
        }
        return instance;
    }

    // ==================== 公开 API ====================
    public void getBestLocation(MyLocationCallback callback) {
        if (cachedResult != null && cachedResult.isValid()) {
            callback.onSuccess(cachedResult.address, cachedResult.lat, cachedResult.lng, cachedResult.accuracy);
            return;
        }
        requestHighAccuracyLocation(callback);
    }

    private void requestHighAccuracyLocation(MyLocationCallback callback) {
        if (!hasLocationPermission()) {
            callback.onFailed("请开启定位权限");
            return;
        }

        LocationRequest request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setFastestInterval(1000)
                .setInterval(3000)
                .setMaxWaitTime(15000);

        // 使用华为原生回调（避免冲突）
        com.huawei.hms.location.LocationCallback hmsCallback = new com.huawei.hms.location.LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null || result.getLastLocation() == null) return;
                Location loc = result.getLastLocation();

                if (loc.getAccuracy() <= 80) {
                    fusedClient.removeLocationUpdates(this);
                    reverseGeocodeAndCache(loc, callback);
                }
            }
        };

        fusedClient.requestLocationUpdates(request, hmsCallback, Looper.getMainLooper())
                .addOnFailureListener(e -> {
                    fusedClient.removeLocationUpdates(hmsCallback);
                    callback.onFailed("定位失败：" + e.getMessage());
                });

        mainHandler.postDelayed(() -> {
            fusedClient.removeLocationUpdates(hmsCallback);
            if (cachedResult == null || !cachedResult.isValid()) {
                callback.onFailed("定位超时");
            }
        }, 15000);
    }

    private void startBackgroundUpdates() {
        LocationRequest bgRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setInterval(30_000);

        fusedClient.requestLocationUpdates(bgRequest, new com.huawei.hms.location.LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result != null && result.getLastLocation() != null) {
                    Location loc = result.getLastLocation();
                    if (loc.getAccuracy() <= 100) {
                        reverseGeocodeAndCache(loc, null);
                    }
                }
            }
        }, Looper.getMainLooper());
    }

    private void reverseGeocodeAndCache(Location location, MyLocationCallback callback) {
        executor.execute(() -> {
            String address = formatChineseAddress(location);
            cachedResult = new CachedResult(address, location.getLatitude(),
                    location.getLongitude(), location.getAccuracy());

            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onSuccess(address, location.getLatitude(),
                            location.getLongitude(), location.getAccuracy());
                }
            });
        });
    }

    private String formatChineseAddress(Location location) {
        Geocoder geocoder = new Geocoder(context, Locale.CHINA);
        try {
            List<Address> list = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (list != null && !list.isEmpty()) {
                Address a = list.get(0);
                StringBuilder sb = new StringBuilder();

                String poi = a.getFeatureName();
                String street = a.getThoroughfare();
                String district = a.getSubLocality();
                String city = a.getLocality();

                if (poi != null && !poi.contains("号") && !poi.equals(street)) sb.append(poi);
                if (street != null && !street.equals(poi)) sb.append(street);
                if (district != null) sb.append(district);
                if (city != null && city.contains("市")) sb.append(city.replace("市", ""));

                return sb.length() > 0 ? sb.toString() : "未知位置";
            }
        } catch (Exception e) {
            Log.e(TAG, "地址解析失败", e);
        }
        return String.format("%.6f, %.6f", location.getLatitude(), location.getLongitude());
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean hasValidCache() {
        return cachedResult != null && cachedResult.isValid();
    }

    public CachedResult getCachedResult() {
        return cachedResult;
    }

    // 改名！避免和华为的 LocationCallback 冲突
    public interface MyLocationCallback {
        void onSuccess(String address, double lat, double lng, float accuracy);
        void onFailed(String error);
    }
}