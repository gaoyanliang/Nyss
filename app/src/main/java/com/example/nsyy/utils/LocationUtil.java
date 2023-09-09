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

import androidx.core.app.ActivityCompat;

import com.example.nsyy.permission.NsyyLocationListener;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

public class LocationUtil {
    private volatile static LocationUtil uniqueInstance;
    private LocationManager locationManager;
    private Context mContext;
    private static ArrayList<AddressCallback> addressCallbacks;
    private AddressCallback addressCallback;
    public void setAddressCallback(AddressCallback addressCallback) {
        this.addressCallback = addressCallback;
    }
    private static Location location;

    private NsyyLocationListener locationListener = new NsyyLocationListener();
    
    private LocationUtil(Context context) {
        mContext = context;
        initLocation();
    }

    //采用Double CheckLock(DCL)实现单例
    public static LocationUtil getInstance(Context context) {
        if (uniqueInstance == null) {
            synchronized (LocationUtil.class) {
                if (uniqueInstance == null) {
                    addressCallbacks = new ArrayList<>();
                    uniqueInstance = new LocationUtil(context);
                }
            }
        }
        return uniqueInstance;
    }

    /**
     * 管理回调事件
     * @param addressCallback
     */
    private void addAddressCallback(AddressCallback addressCallback){
        addressCallbacks.add(addressCallback);
    }

    public void removeAddressCallback(AddressCallback addressCallback){
        if(addressCallbacks.contains(addressCallback)){
            addressCallbacks.remove(addressCallback);
        }
    }

    public void cleareAddressCallback(){
        removeLocationUpdatesListener();
        addressCallbacks.clear();
    }

    private void initLocation() {
        //1.获取位置管理器
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        //添加用户权限申请判断
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            throw new RuntimeException("未获取位置权限，请先获取位置权限");
        }

        String address = getLocation();
        Log.i("Nsyy", "初始化位置信息，当前位置：" + address);
    }

    public String getLocation() {
        // 获取所有可用的位置提供器
        List<String> providerList = locationManager.getProviders(true);
        String locationProvider;
        if (providerList.contains(LocationManager.GPS_PROVIDER)) {
            //GPS 定位的精准度比较高，但是非常耗电。
            System.out.println("=====GPS_PROVIDER=====");
            locationProvider = LocationManager.GPS_PROVIDER;
        } else if (providerList.contains(LocationManager.NETWORK_PROVIDER)) {//Google服务被墙不可用
            //网络定位的精准度稍差，但耗电量比较少。
            System.out.println("=====NETWORK_PROVIDER=====");
            locationProvider = LocationManager.NETWORK_PROVIDER;
        } else {
            System.out.println("=====NO_PROVIDER=====");
            // 当没有可用的位置提供器时，弹出Toast提示用户
            throw new RuntimeException("没有开启位置服务,请先开启位置服务");
        }

        // 获取上次的位置，一般第一次运行，此值为null
        location = locationManager.getLastKnownLocation(locationProvider);
        //当GPS信号弱没获取到位置的时候可从网络获取
        if (location == null) {
            System.out.println("==Google服务被墙的解决办法==");
            location = getLngAndLatWithNetwork();//Google服务被墙的解决办法
            return getAddress(location);
        }

        // 监视地理位置变化，第二个和第三个参数分别为更新的最短时间minTime和最短距离minDistace
        //LocationManager 每隔 5 秒钟会检测一下位置的变化情况，当移动距离超过 10 米的时候，
        // 就会调用 LocationListener 的 onLocationChanged() 方法，并把新的位置信息作为参数传入。
        locationManager.requestLocationUpdates(locationProvider, 5000, 10, locationListener, Looper.getMainLooper());

        return getAddress(location);
    }

    //从网络获取经纬度
    private Location getLngAndLatWithNetwork() {
        //添加用户权限申请判断
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        //LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, locationListener, Looper.getMainLooper());
        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        return location;
    }

    private String getAddress(Location location) {
        //Geocoder通过经纬度获取具体信息
        Geocoder gc = new Geocoder(mContext, Locale.getDefault());
        try {
            List<Address> locationList = gc.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

            if (locationList != null && locationList.size() > 0) {
                Address address = locationList.get(0);
                String countryName = address.getCountryName();//国家
                String countryCode = address.getCountryCode();
                String adminArea = address.getAdminArea();//省
                String locality = address.getLocality();//市
                String subLocality = address.getSubLocality();//区
                String featureName = address.getFeatureName();//街道

                for (int i = 0; address.getAddressLine(i) != null; i++) {
                    String addressLine = address.getAddressLine(i);
                    System.out.println("addressLine=====" + addressLine);
                }
                if(addressCallback != null){
                    addressCallback.onGetAddress(address);
                }
                return address.getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void removeLocationUpdatesListener() {
        if (locationManager != null) {
            uniqueInstance = null;
            locationManager.removeUpdates(locationListener);
        }
    }

    public interface AddressCallback{
        void onGetAddress(Address address);
        void onGetLocation(double lat,double lng);
    }
}

