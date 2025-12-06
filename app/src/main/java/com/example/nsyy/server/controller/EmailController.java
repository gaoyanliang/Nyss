package com.example.nsyy.server.controller;

import static com.example.nsyy.server.api.ReturnData.ERROR.UNKNOWN;

import com.example.nsyy.MainActivity;
import com.example.nsyy.server.api.QueryEmailParam;
import com.example.nsyy.server.api.ReturnData;
import com.example.nsyy.utils.SocketUtil;
import com.yanzhenjie.andserver.annotation.CrossOrigin;
import com.yanzhenjie.andserver.annotation.PostMapping;
import com.yanzhenjie.andserver.annotation.RequestBody;
import com.yanzhenjie.andserver.annotation.RequestMethod;
import com.yanzhenjie.andserver.annotation.RestController;

import java.util.Map;

@RestController
public class EmailController {

    /**
     * 查询邮件
     * @param param
     * @return
     */
    @CrossOrigin(methods = {RequestMethod.POST})
    @PostMapping(path = "/query_mail")
    public ReturnData appQueryEmail(@RequestBody QueryEmailParam param) {
        ReturnData returnData = new ReturnData();
        try {
            Map<String, Object> email = MainActivity.getEmailHelper().queryEmail(param.getMessage_id(),
                    param.getUser_account(), param.getMailbox());
            returnData.setSuccess(true);
            returnData.setCode(20000);
            returnData.setData(email);
            return returnData;
        } catch (Exception e) {
            returnData.setCode(UNKNOWN);
            returnData.setSuccess(false);
            returnData.setErrorMsg("query email failed, Please try again later." + param.toString());
            return returnData;
        }
    }

    @CrossOrigin(methods = {RequestMethod.POST})
    @PostMapping(path = "/delete_mail")
    public ReturnData appDeleteEmail(@RequestBody QueryEmailParam param) {
        ReturnData returnData = new ReturnData();
        try {
            MainActivity.getEmailHelper().deleteEmail(param.getMessage_id());
            returnData.setSuccess(true);
            returnData.setCode(20000);
            return returnData;
        } catch (Exception e) {
            returnData.setCode(UNKNOWN);
            returnData.setSuccess(false);
            returnData.setErrorMsg("delete email failed, Please try again later." + param.toString());
            return returnData;
        }
    }

    @CrossOrigin(methods = {RequestMethod.POST})
    @PostMapping(path = "/clear_mail")
    public ReturnData clearAllData() {
        ReturnData returnData = new ReturnData();
        try {
            MainActivity.getEmailHelper().clearAllData();
            returnData.setSuccess(true);
            returnData.setCode(20000);
            returnData.setData("delete successful");
            return returnData;
        } catch (Exception e) {
            returnData.setCode(UNKNOWN);
            returnData.setSuccess(false);
            returnData.setErrorMsg("delete failed, Please try again later.");
            return returnData;
        }
    }


    @CrossOrigin(methods = {RequestMethod.POST})
    @PostMapping(path = "/socket_connect")
    public ReturnData socket_connect() {
        SocketUtil.getInstance().connect();
        ReturnData returnData = new ReturnData();
        returnData.setSuccess(true);
        returnData.setCode(20000);
        returnData.setData("connected");
        return returnData;
    }

    @CrossOrigin(methods = {RequestMethod.POST})
    @PostMapping(path = "/socket_disconnect")
    public ReturnData socket_disconnect() {
        SocketUtil.getInstance().disconnect();
        ReturnData returnData = new ReturnData();
        returnData.setSuccess(true);
        returnData.setCode(20000);
        returnData.setData("disconnected");
        return returnData;
    }

}
