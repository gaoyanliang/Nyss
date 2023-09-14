package com.example.nsyy.server.controller;

import static com.example.nsyy.server.api.response.ReturnData.ERROR.FAILED_TO_GET_LOCATION;

import com.example.nsyy.server.api.request.Notification;
import com.example.nsyy.server.api.response.ReturnData;
import com.example.nsyy.service.LocationServices;
import com.example.nsyy.service.NotificationServices;
import com.yanzhenjie.andserver.annotation.GetMapping;
import com.yanzhenjie.andserver.annotation.PostMapping;
import com.yanzhenjie.andserver.annotation.RequestBody;
import com.yanzhenjie.andserver.annotation.RestController;

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

    /**
     * TODO 待确定具体地址格式
     * @return
     */
    @GetMapping("/location")
    public ReturnData location() {
        ReturnData returnData = new ReturnData();
        try {
            String address = LocationServices.getInstance().location();
            returnData.setSuccess(true);
            returnData.setCode(200);
            returnData.setData(address);
            return returnData;
        } catch (Exception e) {
            returnData.setCode(FAILED_TO_GET_LOCATION);
            returnData.setSuccess(false);
            returnData.setErrorMsg("Failed to get location: Please enable location service first");
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
            NotificationServices.getInstance().notification(notification);
            returnData.setSuccess(true);
            returnData.setCode(200);
            return returnData;
        } catch (Exception e) {
            returnData.setCode(FAILED_TO_GET_LOCATION);
            returnData.setSuccess(false);
            returnData.setErrorMsg("Failed notification: Please try again later.");
            return returnData;
        }
    }
}
