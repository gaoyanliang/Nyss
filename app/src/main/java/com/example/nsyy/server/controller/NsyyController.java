package com.example.nsyy.server.controller;

import static com.example.nsyy.server.api.ReturnData.ERROR.FAILED_TO_GET_LOCATION;

import com.example.nsyy.config.MySharedPreferences;
import com.example.nsyy.exception.BluetoothException;
import com.example.nsyy.server.api.AppInfo;
import com.example.nsyy.server.api.Notification;
import com.example.nsyy.server.api.SpeechInfo;
import com.example.nsyy.server.api.UserInfo;
import com.example.nsyy.server.api.ReturnData;
import com.example.nsyy.utils.AppVersionUtil;
import com.example.nsyy.utils.BlueToothUtil;
import com.example.nsyy.utils.LocationUtil;
import com.example.nsyy.utils.NotificationUtil;
import com.yanzhenjie.andserver.annotation.CrossOrigin;
import com.yanzhenjie.andserver.annotation.DeleteMapping;
import com.yanzhenjie.andserver.annotation.GetMapping;
import com.yanzhenjie.andserver.annotation.PostMapping;
import com.yanzhenjie.andserver.annotation.RequestBody;
import com.yanzhenjie.andserver.annotation.RequestMethod;
import com.yanzhenjie.andserver.annotation.RequestParam;
import com.yanzhenjie.andserver.annotation.RestController;
import android.content.SharedPreferences;
import android.location.Location;

import java.io.IOException;

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

    /**
     * TODO 待确定具体地址格式
     * @return
     */
    @CrossOrigin(methods = {RequestMethod.GET})
    @GetMapping("/location")
    public ReturnData location() {
        ReturnData returnData = new ReturnData();
        try {
            Location local = LocationUtil.getInstance().getLocation(true);
            String address = LocationUtil.getInstance().getAddress(local);
            returnData.setSuccess(true);
            returnData.setCode(200);
            returnData.setData(address);
            returnData.setLatitude(local.getLatitude());
            returnData.setLongitude(local.getLongitude());
            return returnData;
        } catch (Exception e) {
            returnData.setCode(FAILED_TO_GET_LOCATION);
            returnData.setSuccess(false);

            StringBuilder sb = new StringBuilder();
            sb.append("LocationUtil: " + LocationUtil.getInstance().toString());

            returnData.setErrorMsg("Failed to get location: Please enable location service first.\n" + sb.toString());
            return returnData;
        }
    }

    /**
     * TODO 待确定具体消息格式
     */
    @CrossOrigin(methods = {RequestMethod.POST})
    @PostMapping(path = "/notification")
    public ReturnData notification(@RequestBody Notification notification) {
        ReturnData returnData = new ReturnData();
        try {
            NotificationUtil.getInstance().createNotificationForHigh(notification.title, notification.context);
            returnData.setSuccess(true);
            returnData.setCode(200);
            return returnData;
        } catch (Exception e) {
            returnData.setCode(FAILED_TO_GET_LOCATION);
            returnData.setSuccess(false);

            StringBuilder sb = new StringBuilder();
            sb.append("NotificationUtil: " + NotificationUtil.getInstance().toString());

            returnData.setErrorMsg("Failed notification: Please try again later." + sb.toString());
            return returnData;
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
            returnData.setCode(FAILED_TO_GET_LOCATION);
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
            System.out.println("get user info username: " + username + " password: " + password + " version: " + version);

            if (username == "" || password == "") {
                return new UserInfo(false,"", "", "");
            }
            return new UserInfo(true, username, password, version);
        } catch (Exception e) {
            return new UserInfo(false, "", "", "");
        }
    }

    @CrossOrigin(methods = {RequestMethod.POST})
    @PostMapping(path = "/user")
    public ReturnData save_user_info(@RequestBody UserInfo userInfo) {
        ReturnData returnData = new ReturnData();
        try {
            String username = "";
            String password = "";
            String version = "";

            if (userInfo != null) {
                username = userInfo.getUsername();
                password = userInfo.getPassword();
                version = userInfo.getVersion();
            }

            // Storing a setting
            SharedPreferences.Editor editor = MySharedPreferences.getSharedPreferences().edit();
            editor.putString("username", username);
            editor.putString("password", password);
            editor.putString("version", version);
            editor.apply();

            System.out.println("save user info username: " + username + " password: " + password + " version: " + version);

            returnData.setSuccess(true);
            returnData.setCode(200);
            return returnData;
        } catch (Exception e) {
            returnData.setCode(FAILED_TO_GET_LOCATION);
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
            returnData.setCode(FAILED_TO_GET_LOCATION);
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
            returnData.setCode(FAILED_TO_GET_LOCATION);
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
            returnData.setCode(FAILED_TO_GET_LOCATION);
            returnData.setSuccess(false);

            returnData.setErrorMsg("save failed");
            return returnData;
        }
    }



}
