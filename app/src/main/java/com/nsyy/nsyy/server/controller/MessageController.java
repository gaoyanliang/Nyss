package com.nsyy.nsyy.server.controller;

import static com.nsyy.nsyy.server.api.ReturnData.ERROR.UNKNOWN;

import com.nsyy.nsyy.MainActivity;
import com.nsyy.nsyy.server.api.ReadChatsParam;
import com.nsyy.nsyy.server.api.ReadMessagesParam;
import com.nsyy.nsyy.server.api.ReturnData;
import com.yanzhenjie.andserver.annotation.CrossOrigin;
import com.yanzhenjie.andserver.annotation.DeleteMapping;
import com.yanzhenjie.andserver.annotation.PostMapping;
import com.yanzhenjie.andserver.annotation.RequestBody;
import com.yanzhenjie.andserver.annotation.RequestMethod;
import com.yanzhenjie.andserver.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class MessageController {

    /**
     * 读取聊天消息
     * @param readMessagesParam
     * @return
     */
    @CrossOrigin(methods = {RequestMethod.POST})
    @PostMapping(path = "/read_messages")
    public ReturnData readMessages(@RequestBody ReadMessagesParam readMessagesParam) {
        ReturnData returnData = new ReturnData();
        try {
            System.out.println("MessageController.readMessages: 接收到请求参数: " + readMessagesParam.toString());

            Map<String, String> dict = new HashMap<>();
            dict.put("url", readMessagesParam.getUrl());
            dict.put("cur_user_id", Integer.toString(readMessagesParam.getCur_user_id()));
            dict.put("read_type", Integer.toString(readMessagesParam.getRead_type()));

            if (readMessagesParam.getChat_user_id() != null) {
                dict.put("chat_user_id", Integer.toString(readMessagesParam.getChat_user_id()));
            }
            dict.put("start", Integer.toString(readMessagesParam.getStart()));
            dict.put("count", Integer.toString(readMessagesParam.getCount()));

            dict.put("keyword", readMessagesParam.getKeyword());
            dict.put("start_time_str", readMessagesParam.getStart_time_str());
            dict.put("end_time_str", readMessagesParam.getEnd_time_str());

            List<Map<String, Object>> messages = null;
            // ===== 消息存内嵌数据库 =====
            messages = MainActivity.getDatabaseHelper().queryMessage(readMessagesParam.getRead_type(),
                    readMessagesParam.getCur_user_id(), dict);
            MainActivity.getDatabaseHelper().updateUnread(readMessagesParam.getRead_type(),
                    readMessagesParam.getCur_user_id(), readMessagesParam.getChat_user_id());

            returnData.setSuccess(true);
            returnData.setCode(20000);
            returnData.setData(messages);
            return returnData;
        } catch (Exception e) {
            returnData.setCode(UNKNOWN);
            returnData.setSuccess(false);
            returnData.setErrorMsg("Failed to read notification message, params is " + readMessagesParam.toString());
            return returnData;
        }
    }


    @CrossOrigin(methods = {RequestMethod.POST})
    @PostMapping(path = "/read_chats")
    public ReturnData readChats(@RequestBody ReadChatsParam readChatsParam) {
        System.out.println("MessageController.readChats: 接收到请求参数: " + readChatsParam.toString());

        List<Map<String, Object>> result = new ArrayList<>();
        ReturnData returnData = new ReturnData();
        try {
            // ===== 消息存内嵌数据库 =====
            int allUnread = MainActivity.getDatabaseHelper().getLocalContact(readChatsParam.getUser_id(), result);
            returnData.setSuccess(true);
            returnData.setCode(20000);
            returnData.setData(result);
            returnData.setAll_unread(allUnread);
            return returnData;
        } catch (Exception e) {
            returnData.setCode(UNKNOWN);
            returnData.setSuccess(false);
            returnData.setErrorMsg("Failed to get contact list, params is " + readChatsParam.toString());
            return returnData;
        }
    }

    @CrossOrigin(methods = {RequestMethod.DELETE})
    @DeleteMapping(path = "/delete_db_data")
    public ReturnData deleteDbData() {
        ReturnData returnData = new ReturnData();
        try {
            System.out.println("MessageController.deleteDbData: 接收到请求参数： ");
            MainActivity.getDatabaseHelper().clearAllData();

            returnData.setSuccess(true);
            returnData.setCode(20000);
            returnData.setData("delete successful");
            return returnData;
        } catch (Exception e) {
            returnData.setCode(UNKNOWN);
            returnData.setSuccess(false);
            returnData.setErrorMsg("Failed to write message");
            return returnData;
        }
    }


    @CrossOrigin(methods = {RequestMethod.POST})
    @PostMapping(path = "/update_unread")
    public ReturnData updateUnread(@RequestBody ReadMessagesParam readMessagesParam) {
        ReturnData returnData = new ReturnData();
        try {
            System.out.println("MessageController.updateUnread: 接收到请求参数： " + readMessagesParam);
            MainActivity.getDatabaseHelper().updateUnread(readMessagesParam.getRead_type(),
                    readMessagesParam.getCur_user_id(), readMessagesParam.getChat_user_id());

            returnData.setSuccess(true);
            returnData.setCode(20000);
            returnData.setData("update successful");
            return returnData;
        } catch (Exception e) {
            returnData.setCode(UNKNOWN);
            returnData.setSuccess(false);
            returnData.setErrorMsg("Failed to write message");
            return returnData;
        }
    }


    @CrossOrigin(methods = {RequestMethod.POST})
    @PostMapping(path = "/delete_contact")
    public ReturnData deleteContact(@RequestBody ReadMessagesParam readMessagesParam) {
        ReturnData returnData = new ReturnData();
        try {
            System.out.println("MessageController.deleteContact: 接收到请求参数： " + readMessagesParam);
            MainActivity.getDatabaseHelper().deleteContact(readMessagesParam.getRead_type(),
                    readMessagesParam.getCur_user_id(), readMessagesParam.getChat_user_id());

            returnData.setSuccess(true);
            returnData.setCode(20000);
            returnData.setData("delete contact successful");
            return returnData;
        } catch (Exception e) {
            returnData.setCode(UNKNOWN);
            returnData.setSuccess(false);
            returnData.setErrorMsg("Failed to delete_contact");
            return returnData;
        }
    }


}
