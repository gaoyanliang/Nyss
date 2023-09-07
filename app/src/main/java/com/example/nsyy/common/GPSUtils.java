package com.example.nsyy.common;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;
import android.Manifest;

import java.io.IOException;
import java.util.List;

public class GPSUtils {

    private static GPSUtils instance;
    private LocationManager locationManager;

    public static GPSUtils getInstance() {
        if (instance == null) {
            instance = new GPSUtils();
        }
        return instance;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public String getProvince(Activity activity) {
        Log.i("GPS: ", "getProvince");
        locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);        // 默认Android GPS定位实例

        Location location = null;
        // 是否已经授权
        if (activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            //判断GPS是否开启，没有开启，则开启
//            if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
//                //跳转到手机打开GPS页面
//                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                //设置完成后返回原来的界面
//                AppActivity.instance.startActivityForResult(intent,OPEN_GPS_CODE);
//            }
//
//            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);      // GPS芯片定位 需要开启GPS
//            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);      // 利用网络定位 需要开启GPS
            location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);      // 其他应用使用定位更新了定位信息 需要开启GPS
        }

        String p = "";
        if(location != null) {
            Log.i("GPS: ", "获取位置信息成功");
            Log.i("GPS: ","经度：" + location.getLatitude());
            Log.i("GPS: ","纬度：" + location.getLongitude());

            // 获取地址信息
            p = getAddress(activity, location.getLatitude(),location.getLongitude());
            Log.i("GPS: ","location：" + p);
        } else {
            Log.i("GPS: ", "获取位置信息失败，请检查是够开启GPS,是否授权");
        }


        return p;
    }

    /*
     * 根据经度纬度 获取国家，省份
     * */
    public String getAddress(Activity activity, double latitude, double longitude) {
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
            return addressLines;
        }
        return "unknown address";
    }
}

