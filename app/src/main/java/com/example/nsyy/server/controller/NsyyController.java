package com.example.nsyy.server.controller;

import static com.example.nsyy.server.api.response.ReturnData.ERROR.FAILED_TO_GET_LOCATION;

import com.example.nsyy.exception.BluetoothException;
import com.example.nsyy.server.api.request.Notification;
import com.example.nsyy.server.api.response.ReturnData;
import com.example.nsyy.utils.BlueToothUtil;
import com.example.nsyy.utils.LocationUtil;
import com.example.nsyy.utils.NotificationUtil;
import com.yanzhenjie.andserver.annotation.GetMapping;
import com.yanzhenjie.andserver.annotation.PostMapping;
import com.yanzhenjie.andserver.annotation.RequestBody;
import com.yanzhenjie.andserver.annotation.RequestParam;
import com.yanzhenjie.andserver.annotation.RestController;

import java.io.IOException;

@RestController
public class NsyyController {

    @GetMapping("/test")
    public ReturnData ping() {
        ReturnData returnData = new ReturnData();
        returnData.setSuccess(true);
        returnData.setCode(200);
        returnData.setData("SERVER OK");
        return returnData;
    }

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
    @GetMapping("/location")
    public ReturnData location() {
        ReturnData returnData = new ReturnData();
        try {
            String address = LocationUtil.getInstance().getLocation(true);
            returnData.setSuccess(true);
            returnData.setCode(200);
            returnData.setData(address);
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
}
