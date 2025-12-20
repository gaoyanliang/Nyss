package com.nsyy.nsyy.server.controller;

import com.nsyy.nsyy.service.LocalBroadcastHelper;
import com.nsyy.nsyy.config.MySharedPreferences;
import com.nsyy.nsyy.exception.BluetoothException;
import com.nsyy.nsyy.permission.AppApplication;
import com.nsyy.nsyy.server.api.AppInfo;
import com.nsyy.nsyy.server.api.Notification;
import com.nsyy.nsyy.server.api.SpeechInfo;
import com.nsyy.nsyy.server.api.UserInfo;
import com.nsyy.nsyy.server.api.ReturnData;
import com.nsyy.nsyy.utils.AppVersionUtil;
import com.nsyy.nsyy.utils.BlueToothUtil;
import com.nsyy.nsyy.utils.LocationUtil;
import com.nsyy.nsyy.utils.NotificationUtil;
import com.yanzhenjie.andserver.annotation.CrossOrigin;
import com.yanzhenjie.andserver.annotation.DeleteMapping;
import com.yanzhenjie.andserver.annotation.GetMapping;
import com.yanzhenjie.andserver.annotation.PostMapping;
import com.yanzhenjie.andserver.annotation.RequestBody;
import com.yanzhenjie.andserver.annotation.RequestMethod;
import com.yanzhenjie.andserver.annotation.RequestParam;
import com.yanzhenjie.andserver.annotation.RestController;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

@RestController
public class NsyyController {

    @CrossOrigin(methods = {RequestMethod.GET})
    @GetMapping("/test")
    public ReturnData ping() {
        ReturnData returnData = new ReturnData();
        returnData.setSuccess(true);
        returnData.setCode(200);
        returnData.setData("SERVER OK");
        return returnData;
    }

    @CrossOrigin(methods = {RequestMethod.GET})
    @GetMapping("/get_weight")
    public ReturnData connectBlueTooth(@RequestParam("address") String address) {
        try {
            // 搜索蓝牙设备
            String weight = BlueToothUtil.getInstance().read(address);
            ReturnData returnData = new ReturnData();
            returnData.setSuccess(true);
            returnData.setCode(200);
            returnData.setData(weight);
            return returnData;
        } catch (BluetoothException e) {
            ReturnData returnData = new ReturnData();
            returnData.setSuccess(false);
            returnData.setCode(e.code);
            returnData.setErrorMsg(e.msg);
            return returnData;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @CrossOrigin(methods = {RequestMethod.GET})
    @GetMapping("/location")
    public ReturnData location() {

        ReturnData data = new ReturnData();
        // 先检查权限（用 ApplicationContext 安全）
        Context appContext = AppApplication.getContext();  // 或传入 context
        if (ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 未授权，发送本地广播，通知 MainActivity 申请
            LocalBroadcastHelper.sendRequestLocationPermission();

            data.setSuccess(false);
            data.setCode(ReturnData.ERROR.FAILED_TO_GET_LOCATION);
            data.setErrorMsg("未开启定位权限, 请先打开定位权限");
            data.setData("未开启定位权限, 请先打开定位权限");
            return data;
        }

        LocationUtil util = LocationUtil.getInstance();

        // 1. 优先返回缓存（99% 情况 < 50ms）
        if (util.hasValidCache()) {
            LocationUtil.CachedResult c = util.getCachedResult();
            data.setSuccess(true);
            data.setCode(200);
            data.setData(c.address);
            data.setLatitude(c.lat);
            data.setLongitude(c.lng);
            return data;
        }

        // 2. 无缓存 → 触发定位，但最多等 100ms
        final AtomicReference<ReturnData> result = new AtomicReference<>(null);
        final long start = System.currentTimeMillis();

        util.getBestLocation(new LocationUtil.MyLocationCallback() {
            @Override
            public void onSuccess(String address, double lat, double lng, float accuracy) {
                ReturnData r = new ReturnData();
                r.setSuccess(true);
                r.setCode(200);
                r.setData(address);
                r.setLatitude(lat);
                r.setLongitude(lng);
                result.set(r);
            }

            @Override
            public void onFailed(String error) {
                ReturnData r = new ReturnData();
                r.setSuccess(false);
                r.setCode(ReturnData.ERROR.FAILED_TO_GET_LOCATION);
                r.setErrorMsg(error);
                result.set(r);
            }
        });

        while (result.get() == null && System.currentTimeMillis() - start < 100) {
            try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        ReturnData finalResult = result.get();
        if (finalResult != null) {
            return finalResult;
        } else {
            data.setSuccess(false);
            data.setCode(ReturnData.ERROR.FAILED_TO_GET_LOCATION);
            data.setErrorMsg("正在获取精准位置...");
            data.setData("定位中，约5-15秒后完成");
            return data;
        }
    }


    @CrossOrigin(methods = {RequestMethod.POST})
    @PostMapping(path = "/speech")
    public ReturnData speech(@RequestBody Notification notification) {
        ReturnData returnData = new ReturnData();
        try {
            NotificationUtil.getInstance().speechInfo(notification.context);
            returnData.setSuccess(true);
            returnData.setCode(200);
            return returnData;
        } catch (Exception e) {
            returnData.setCode(ReturnData.ERROR.FAILED_TO_GET_LOCATION);
            returnData.setSuccess(false);

            StringBuilder sb = new StringBuilder();
            sb.append("NotificationUtil: " + NotificationUtil.getInstance().toString());

            returnData.setErrorMsg("Failed notification: Please try again later." + sb.toString());
            return returnData;
        }
    }


    /**
     * 获取用户信息
     *
     * @return
     */
    @CrossOrigin(methods = {RequestMethod.GET})
    @GetMapping("/user")
    public UserInfo get_user_info() {
        try {
            // Retrieving user info
            String username = MySharedPreferences.getSharedPreferences().getString("username", "");
            String password = MySharedPreferences.getSharedPreferences().getString("password", "");
            String version = MySharedPreferences.getSharedPreferences().getString("version", "");
            String token = MySharedPreferences.getSharedPreferences().getString("token", "");
            String brand = MySharedPreferences.getSharedPreferences().getString("brand", "");
            int pers_id = MySharedPreferences.getSharedPreferences().getInt("pers_id", 0);
            System.out.println("get user info username: " + username + " password: " + password + " version: " + version);

            if (username == "" || password == "") {
                return new UserInfo(false,"", "", "", 0, token, Build.BRAND);
            }
            return new UserInfo(true, username, password, version, pers_id, token, brand);
        } catch (Exception e) {
            return new UserInfo(false, "", "", "", 0, "", Build.BRAND);
        }
    }

    @CrossOrigin(methods = {RequestMethod.POST})
    @PostMapping(path = "/user")
    public ReturnData save_user_info(@RequestBody UserInfo userInfo) {
        System.out.println("====》 保存用户名密码" + userInfo.toString());

        ReturnData returnData = new ReturnData();
        try {
            String username = "";
            String password = "";
            String version = "";
            Integer pers_id = 0;

            if (userInfo != null) {
                username = userInfo.getUsername();
                password = userInfo.getPassword();
                version = userInfo.getVersion();
                pers_id = userInfo.getPers_id();
            }

            // Storing a setting
            SharedPreferences.Editor editor = MySharedPreferences.getSharedPreferences().edit();
            editor.putString("username", username);
            editor.putString("password", password);
            editor.putString("version", version);
            editor.putInt("pers_id", pers_id);
            editor.putString("brand", Build.BRAND);
            editor.apply();

            System.out.println("save user info username: " + username + " password: " +
                    password + " version: " + version + " pers_id: " + pers_id);

            returnData.setSuccess(true);
            returnData.setCode(200);
            return returnData;
        } catch (Exception e) {
            returnData.setCode(ReturnData.ERROR.FAILED_TO_GET_LOCATION);
            returnData.setSuccess(false);

            returnData.setErrorMsg("The user name, password, version save failed");
            return returnData;
        }
    }

    @CrossOrigin(methods = {RequestMethod.DELETE})
    @DeleteMapping(path = "/user")
    public ReturnData delete_user_info() {
        ReturnData returnData = new ReturnData();
        try {

            // Storing a setting
            SharedPreferences.Editor editor = MySharedPreferences.getSharedPreferences().edit();
            editor.remove("username");
            editor.remove("password");
            editor.remove("version");
            editor.commit();

            returnData.setSuccess(true);
            returnData.setCode(200);
            return returnData;
        } catch (Exception e) {
            returnData.setCode(ReturnData.ERROR.FAILED_TO_GET_LOCATION);
            returnData.setSuccess(false);

            returnData.setErrorMsg("The user name, password, version delete failed");
            return returnData;
        }
    }

    @CrossOrigin(methods = {RequestMethod.DELETE})
    @DeleteMapping(path = "/load_url")
    public ReturnData delete_load_url() {
        ReturnData returnData = new ReturnData();
        try {

            // Storing a setting
            SharedPreferences.Editor editor = MySharedPreferences.getSharedPreferences().edit();
            editor.remove("load_url");
            editor.commit();

            returnData.setSuccess(true);
            returnData.setCode(200);
            return returnData;
        } catch (Exception e) {
            returnData.setCode(ReturnData.ERROR.FAILED_TO_GET_LOCATION);
            returnData.setSuccess(false);

            returnData.setErrorMsg("The load_url delete failed");
            return returnData;
        }
    }

    @CrossOrigin(methods = {RequestMethod.GET})
    @GetMapping("/app_version")
    public AppInfo get_app_version() {
        /**
         * 通过 type 区分 ios / android
         * 通过 detail 区分使用场景： phone / pda / 医废 / 综合预约
         */
        try {
            double version = AppVersionUtil.getInstance().getCurrentVersionCode();
            return new AppInfo(true, 200, "", version, "android", "phone");
        } catch (Exception e) {
            return new AppInfo(false, 500, e.getMessage(), -1.0, "android", "phone");
        }
    }

    @CrossOrigin(methods = {RequestMethod.GET})
    @GetMapping("/speech_info")
    public SpeechInfo get_speech_info() {
        try {
            // Retrieving user info
            Float rate = MySharedPreferences.getSharedPreferences().getFloat("rate", 0);
            String name = MySharedPreferences.getSharedPreferences().getString("name", "");
            String locale = MySharedPreferences.getSharedPreferences().getString("locale", "");
            System.out.println("get speech info rate: " + rate.toString() + " name: " + name + " locale: " + locale);
            return new SpeechInfo(rate, name, locale);
        } catch (Exception e) {
            return new SpeechInfo(0, "", "");
        }
    }

    @CrossOrigin(methods = {RequestMethod.POST})
    @PostMapping(path = "/speech_info")
    public ReturnData save_speech_info(@RequestBody SpeechInfo speechInfo) {
        ReturnData returnData = new ReturnData();
        try {
            float rate = 1.0f;
            String name = "xinghe";
            String locale = "zh";

            if (speechInfo != null) {
                rate = speechInfo.getRate();
                name = speechInfo.getName();
                locale = speechInfo.getLocale();
            }

            // Storing a setting
            SharedPreferences.Editor editor = MySharedPreferences.getSharedPreferences().edit();
            editor.putFloat("rate", rate);
            editor.putString("name", name);
            editor.putString("locale", locale);
            editor.apply();

            System.out.println("save speech info rate: " + rate + " name: " + name + " locale: " + locale);

            returnData.setSuccess(true);
            returnData.setCode(200);
            return returnData;
        } catch (Exception e) {
            returnData.setCode(ReturnData.ERROR.FAILED_TO_GET_LOCATION);
            returnData.setSuccess(false);

            returnData.setErrorMsg("save failed");
            return returnData;
        }
    }



}
