package com.example.nsyy.server.controller;

import static com.example.nsyy.server.api.ReturnData.ERROR.FAILED_TO_GET_LOCATION;
import static com.example.nsyy.server.api.ReturnData.ERROR.UNKNOWN;

import com.example.nsyy.MainActivity;
import com.example.nsyy.message.FileHelper;
import com.example.nsyy.server.api.GroupContactParam;
import com.example.nsyy.server.api.NotificationParam;
import com.example.nsyy.server.api.ReadChatsParam;
import com.example.nsyy.server.api.ReadMessagesParam;
import com.example.nsyy.server.api.ReturnData;
import com.example.nsyy.server.api.WriteMessageParam;
import com.example.nsyy.utils.NotificationUtil;
import com.yanzhenjie.andserver.annotation.CrossOrigin;
import com.yanzhenjie.andserver.annotation.DeleteMapping;
import com.yanzhenjie.andserver.annotation.GetMapping;
import com.yanzhenjie.andserver.annotation.PostMapping;
import com.yanzhenjie.andserver.annotation.RequestBody;
import com.yanzhenjie.andserver.annotation.RequestMethod;
import com.yanzhenjie.andserver.annotation.RequestParam;
import com.yanzhenjie.andserver.annotation.RestController;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class MessageController {


    /**
     * 现在前端 不主动调用 这个接口，改由socket接收消息，弹框通知并保存消息
     * @param notification
     * @return
     */
    @CrossOrigin(methods = {RequestMethod.POST})
    @PostMapping(path = "/notification")
    public ReturnData notification(@RequestBody NotificationParam notification) {
        ReturnData returnData = new ReturnData();
        try {
            if (!notification.getTitle().isEmpty()) {
                NotificationUtil.getInstance().createNotificationForHigh(notification.title, notification.context);
            }

            if (notification.getMessage() != null) {
                FileHelper.getInstance().notificationSaveToLocal(notification.getIn_chat(),
                        notification.getCurUserId(), notification.getMessage());
            }
            returnData.setSuccess(true);
            returnData.setCode(20000);
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

            List<Map<String, Object>> messages = null;
//            // ===== 消息存本地文件 =====
//            if (readMessagesParam.getRead_type() == 0) {
//                // 通知
//                messages = FileHelper.getInstance().updateLocalDataAndReturnMsg(0, readMessagesParam.getCur_user_id(), dict);
//            }else if (readMessagesParam.getRead_type() == 1) {
//                // 私聊
//                messages = FileHelper.getInstance().updateLocalDataAndReturnMsg(1, readMessagesParam.getCur_user_id(), dict);
//            } else if (readMessagesParam.getRead_type() == 2) {
//                // 群聊
//                messages = FileHelper.getInstance().updateLocalDataAndReturnMsg(2, readMessagesParam.getCur_user_id(), dict);
//            }
//            // 更新未读状态
//            FileHelper.getInstance().updateUnread(readMessagesParam.getRead_type(), readMessagesParam.getCur_user_id(), readMessagesParam.getChat_user_id());

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
//            // ===== 消息存本地文件 =====
//            int allUnread = FileHelper.getInstance().getLocalContact(readChatsParam.getUser_id(), result);

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




    /**
     * 写入消息
     * @param writeMessageParam
     * @return
     */
    @CrossOrigin(methods = {RequestMethod.POST})
    @PostMapping(path = "/write_message")
    public ReturnData writeMessage(@RequestBody WriteMessageParam writeMessageParam) {
        ReturnData returnData = new ReturnData();
        try {
            System.out.println("MessageController.writeMessage: 接收到请求参数： " +
                    writeMessageParam.toString());

            if (writeMessageParam.getPrev_msg_id() == null || writeMessageParam.getMsg() == null) {
                returnData.setCode(UNKNOWN);
                returnData.setSuccess(false);
                returnData.setErrorMsg("Failed to write message, params is " + writeMessageParam.toString());
                return returnData;
            }

            // 程序进入后台时，调用系统通知功能
            FileHelper.getInstance().sendNotification(writeMessageParam.getChat_type(),
                    writeMessageParam.getChat_user_name(), writeMessageParam.getMsg());

            // 将消息写入本地
            FileHelper.getInstance().writeMessageToLocal(writeMessageParam.getCur_user_id(),
                    writeMessageParam.getMsg());

            // 维护联系人，未读状态
            FileHelper.getInstance().updateLocalContact(true, writeMessageParam.getChat_type(),
                    writeMessageParam.getCur_user_id(), writeMessageParam.getChat_user_id(),
                    writeMessageParam.getChat_user_name(), writeMessageParam.getIn_chat(), writeMessageParam.getMsg());


            returnData.setSuccess(true);
            returnData.setCode(20000);
            returnData.setData("write successful");
            return returnData;
        } catch (Exception e) {
            returnData.setCode(UNKNOWN);
            returnData.setSuccess(false);
            returnData.setErrorMsg("Failed to write message, params is " + writeMessageParam.toString());
            return returnData;
        }
    }


    @CrossOrigin(methods = {RequestMethod.POST})
    @PostMapping(path = "/update_group_contact")
    public ReturnData updateGroupContact(@RequestBody GroupContactParam param) {
        ReturnData returnData = new ReturnData();
        try {
            System.out.println("MessageController.updateGroupContact: 接收到请求参数： " +
                    param.toString());
            FileHelper.getInstance().updateLocalContact(true, 2, param.getCur_user_id(),
                    param.getGroup_id(), param.getGroup_name(), 1, null);

            returnData.setSuccess(true);
            returnData.setCode(20000);
            returnData.setData("update successful");
            return returnData;
        } catch (Exception e) {
            returnData.setCode(UNKNOWN);
            returnData.setSuccess(false);
            returnData.setErrorMsg("Failed to update local message, params is " + param.toString());
            return returnData;
        }
    }


    @CrossOrigin(methods = {RequestMethod.GET})
    @GetMapping(path = "/query_file")
    public ReturnData query_file(@RequestParam("file_name") String fileName,
                                 @RequestParam("cur_user_id") Integer curUserId,
                                 @RequestParam("type") Integer type) throws JSONException {

        // Reading from a file
        String dir = "";
        if (type == 0) {
            dir = "/" + FileHelper.MESSAGES_DIR + "/" + curUserId.toString() + "/";
        } else {
            dir = "/" + FileHelper.CONTACTS_DIR + "/" + curUserId.toString() + "/";
        }
        List<String> linesRead = FileHelper.getInstance().readLinesFromFile(fileName,
                FileHelper.ALL_LINE, dir);
        System.out.println("===== query " + dir + fileName + " =====");
        List<JSONObject> res = new ArrayList<>();
        for (String line : linesRead) {
            System.out.println("Message: " + line);
            res.add(new JSONObject(line));
        }


        ReturnData returnData = new ReturnData();
        returnData.setSuccess(true);
        returnData.setCode(20000);
        returnData.setData(linesRead);
        return returnData;
    }

    @CrossOrigin(methods = {RequestMethod.GET})
    @GetMapping(path = "/query_file_list")
    public ReturnData query_file_list(@RequestParam("cur_user_id") Integer curUserId,
                                 @RequestParam("type") Integer type) {

        // Reading from a file
        String dir = "";
        if (type == 0) {
            dir = "/" + FileHelper.MESSAGES_DIR + "/" + curUserId.toString() + "/";
        } else if (type == 1){
            dir = "/" + FileHelper.CONTACTS_DIR + "/" + curUserId.toString() + "/";
        } else if (type == 2) {
            dir = "/" + FileHelper.ATTACHMENTS_DIR + "/";
        }

        List<String> fileList = FileHelper.getInstance().getFileList(dir);
        ReturnData returnData = new ReturnData();
        returnData.setSuccess(true);
        returnData.setCode(20000);
        returnData.setData(fileList);
        return returnData;
    }

    @CrossOrigin(methods = {RequestMethod.DELETE})
    @DeleteMapping(path = "/delete_file")
    public ReturnData delete_file(@RequestParam("file_name") String fileName,
                                  @RequestParam("cur_user_id") Integer curUserId,
                                  @RequestParam("type") Integer type) {
        String dir = "";
        if (type == 0) {
            dir = "/" + FileHelper.MESSAGES_DIR + "/" + curUserId.toString() + "/";
        } else {
            dir = "/" + FileHelper.CONTACTS_DIR + "/" + curUserId.toString() + "/";
        }
        System.out.println("===== delete " + dir + fileName + " =====");
        FileHelper.getInstance().deleteFile(fileName, dir);

        ReturnData returnData = new ReturnData();
        returnData.setSuccess(true);
        returnData.setCode(20000);
        returnData.setData("delete ok");
        return returnData;
    }
}
