package com.example.nsyy.permission;

import android.location.Location;
import android.location.LocationListener;
import android.util.Log;

public class NsyyLocationListener implements LocationListener {
    @Override
    public void onLocationChanged(Location loc) {
        System.out.println("==onLocationChanged==");
        Log.i("GPS: ", "位置信息更新");
        Log.i("GPS: ", "经度："+loc.getLongitude());
        Log.i("GPS: ","纬度："+loc.getLatitude());
    }

    @Override
    public void onProviderDisabled(String provider) {
        //当provider被用户关闭时调用
        Log.i("GPS: ","GPS provider 被关闭！");
    }

    @Override
    public void onProviderEnabled(String provider) {
        //当provider被用户开启后调用
        Log.i("GPS: ","GPS provider 被开启！");
    }
}
