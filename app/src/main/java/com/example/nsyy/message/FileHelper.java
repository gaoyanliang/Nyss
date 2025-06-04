package com.example.nsyy.message;

import android.content.Context;

import com.example.nsyy.utils.NotificationUtil;
import com.yanzhenjie.andserver.http.multipart.MultipartFile;
import com.alibaba.fastjson.JSON;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FileHelper {

    // 消息存储目录
    public static String MESSAGES_DIR = "MESSAGES";
    // 聊天列表存储目录
    public static String CONTACTS_DIR = "CONTACTS";
    // 附件目录
    public static String ATTACHMENTS_DIR = "ATTACHMENTS";

    public static String NOTIFICATION_FILE_HEADER = "nsyy_notification_message_";
    public static String PRIVATE_FILE_HEADER = "nsyy_private_message_";
    public static String GROUP_FILE_HEADER = "nsyy_group_message_";

    public static boolean RUN_IN_BACKGROUND = false;

    // 读取所有行数据
    public static int ALL_LINE = -1;
    // 读取最后一行数据
    public static int LAST_LINE = 1;

    private volatile static FileHelper uniqueInstance;
    private Context context;

    public void setContext(Context context) {
        this.context = context;
    }

    //采用Double CheckLock(DCL)实现单例
    public static FileHelper getInstance() {
        if (uniqueInstance == null) {
            synchronized (FileHelper.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new FileHelper();
                }
            }
        }
        return uniqueInstance;
    }

    public void notificationSaveToLocal(int inChat, int curUserId, Map<String, Object> message) {
        Object value = message.get("chat_type");
        int chatType = (value != null) ? ((Number) value).intValue() : 0; // 默认值

        value = message.get("sender");
        int sender = (value != null) ? ((Number) value).intValue() : 0; // 默认值

        value = message.get("receiver");
        int receiver = (value != null) ? ((Number) value).intValue() : 0; // 默认值

        String sender_name = (String) message.getOrDefault("sender_name", "Unknown");
        if (sender == curUserId) {
            sender = receiver;
            sender_name = (String) message.getOrDefault("receiver_name", "Unknown");
        }

        String json = JSON.toJSONString(message);
        updateLocalContact(true, chatType, curUserId, sender, sender_name, inChat, json);

        writeMessageToLocal(curUserId, json);
    }


    /**
     * 读取消息
     * 1. 先从服务器加载最新消息，写入本地文件
     * 2. 从本地文件中查找指定消息
     *
     * @param type 0-通知消息 1-私聊 2-群聊
     * @param dict
     * @return
     */
    public List<Map<String, Object>> updateLocalDataAndReturnMsg(int type, int curUserId, Map<String, String> dict) {
        List<Map<String, Object>> messages = new ArrayList<>();

        // 1. 构建文件名（与原逻辑相同）
        String fileName = buildFileName(type, dict);
        String dir = "/" + MESSAGES_DIR + "/" + curUserId + "/";

        // 2. 从文件读取所有消息行
        List<String> messageInFile = readLinesFromFile(fileName, ALL_LINE, dir);

        // 3. 解析分页参数（带默认值）
        int start = Integer.parseInt(dict.getOrDefault("start", "0"));
        int count = Integer.parseInt(dict.getOrDefault("count", String.valueOf(messageInFile.size())));

        // 4. 计算分页范围
        int totalMessages = messageInFile.size();
        int fromIndex = Math.max(0, totalMessages - start - count); // 防止负数
        int toIndex = Math.min(totalMessages, totalMessages - start); // 防止越界

        // 5. 截取子列表（最新消息在文件末尾）
        List<String> paginatedMessages = messageInFile.subList(fromIndex, toIndex);

        // 6. 转换为JSON格式
        for (String msg : paginatedMessages) {
            try {
                JSONObject jsonObject = new JSONObject(msg);
                Map<String, Object> jsonMsg = jsonToMap(jsonObject);

                // 特殊处理通知消息的context
                if (type == 0) {
                    jsonMsg.put("context", jsonToMap(new JSONObject((Map) jsonMsg.get("context"))));
                }

                messages.add(jsonMsg);

                // 达到数量限制时停止
                if (messages.size() >= count) break;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

//        Collections.reverse(messages);
        return messages;
    }


    // 提取文件名构建逻辑
    private String buildFileName(int type, Map<String, String> dict) {
        switch (type) {
            case 0: // 通知
                return NOTIFICATION_FILE_HEADER + dict.get("cur_user_id");
            case 1: // 私聊
                int sender = Integer.parseInt(dict.get("cur_user_id"));
                int receiver = Integer.parseInt(dict.get("chat_user_id"));
                return PRIVATE_FILE_HEADER + Math.min(sender, receiver) + "_" + Math.max(sender, receiver);
            case 2: // 群聊
                return GROUP_FILE_HEADER + dict.get("chat_user_id");
            default:
                throw new IllegalArgumentException("Invalid message type: " + type);
        }
    }


    /**
     * 更新未读数量
     * @param chatType
     * @param curUserId
     * @param chatUserId
     */
    public void updateUnread(int chatType, int curUserId, int chatUserId) {
        System.out.println("===> FileHelper.updateUnread 更新未读状态： chat_type=" + chatType +
                " cur_user_id=" + curUserId + " chat_user_id=" + chatUserId);
        String fileName = "";
        if (chatType == 0) {
            fileName = "notification";
        } else if (chatType == 1) {
            fileName = "private_" + chatUserId;
        } else if (chatType == 2) {
            fileName = "group_" + chatUserId;
        }

        String dir = "/" + CONTACTS_DIR + "/" + curUserId + "/";
        List<String> msgs = readLinesFromFile(fileName, LAST_LINE, dir);
        if (!msgs.isEmpty()) {
            try {
                // Convert JSON string to JSON object
                JSONObject jsonObject = new JSONObject(msgs.get(0));
                if (jsonObject.getInt("unread") == 0) {
                    return ;
                }
                jsonObject.put("unread", 0);
                List<String> newContact = new ArrayList<>();
                newContact.add(jsonObject.toString());
                writeLinesToFile(newContact, fileName, dir, false);
            } catch (JSONException e) {
                System.out.println("===> FileHelper.updateUnread 更新未读状态： 状态更新失败");
            }
        }

    }


    /**
     * 读取本地聊天列表
     * @param curUserId
     * @return
     */
    public int getLocalContact(int curUserId, List<Map<String, Object>> result) {
        int allUnread = 0;
        String dir = "/" + CONTACTS_DIR + "/" + curUserId + "/";

        // 获取应用的私有文件目录
        File directory = new File(context.getFilesDir() + dir);
        if (directory.isDirectory()) {
            System.out.println("===> 开始遍历目录: " + directory.getAbsolutePath());
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        // 子目录不处理（正常情况不会出现子目录）
                    } else {
                        // 处理文件，例如打印文件路径
                        System.out.println("读取 File: " + file.getAbsolutePath());
                        List<String> allLines = new ArrayList<>();

                        try (FileInputStream fis = new FileInputStream(file.getPath());
                             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {

                            // Read all line data
                            String line;
                            while ((line = reader.readLine()) != null) {
                                allLines.add(line);
                            }

                            String msg = "";
                            if (allLines.size() > 0){
                                msg = allLines.get(allLines.size() - 1);
                            } else {
                                msg = allLines.get(0);
                            }
                            System.out.println("===> File 内容: " + msg);

                            // Convert JSON string to JSON object
                            JSONObject jsonObject = new JSONObject(msg);
                            Map<String, Object> map = jsonToMap(jsonObject);
                            if (0 == ((Integer) map.get("chat_type"))) {
                                map.put("last_msg", jsonToMap(new JSONObject(((String) map.get("last_msg")))));
                            }
                            result.add(map);
                            int unread = jsonObject.getInt("unread");
                            allUnread += unread;

                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
        return allUnread;
    }


    /**
     * 将消息写入本地文件
     * @param userId
     * @param msg
     */
    public void writeMessageToLocal(int userId, String msg) {
        JSONObject newMessage = null;
        String fileName = "";

        String dir = "/" + MESSAGES_DIR + "/" + userId + "/";

        List<String> writemsg = new ArrayList<>();
        writemsg.add(msg);
        try {
            // Convert JSON string to JSON object
            newMessage = new JSONObject(msg);
            int type = Integer.parseInt(newMessage.getString("chat_type"));

            if (type == 0) {
                // 通知类型
                String receiver = newMessage.getString("receiver");
                if (receiver.contains(Integer.toString(userId))) {
                    fileName = NOTIFICATION_FILE_HEADER + Integer.toString(userId);
                    writeLinesToFile(writemsg, fileName, dir, true);
                }
            } else if (type == 1) {
                // 私聊
                String sender = newMessage.getString("sender");
                String receiver = newMessage.getString("receiver");
                if (Integer.parseInt(sender) <= Integer.parseInt(receiver)) {
                    fileName = PRIVATE_FILE_HEADER + sender + "_" + receiver;
                } else {
                    fileName = PRIVATE_FILE_HEADER + receiver + "_" + sender;
                }
                writeLinesToFile(writemsg, fileName, dir, true);

            } else if (type == 2) {
                // 群聊
                String groupId = newMessage.getString("group_id");
                fileName = GROUP_FILE_HEADER + groupId;
                writeLinesToFile(writemsg, fileName, dir, true);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * 调用系统消息通知
     * @param chatType
     * @param chatUserName
     * @param msg
     * @throws JSONException
     */
    public void sendNotification(int chatType, String chatUserName, String msg) throws JSONException {
        if (!RUN_IN_BACKGROUND) {
            return ;
        }
        System.out.println("===> app在后台运行，推送系统通知: " + msg);

        if (chatType == 0) {
            JSONObject jsonObject = new JSONObject(msg);
            String context = jsonObject.getString("context");

            jsonObject = new JSONObject(context);
            String title = jsonObject.getString("title");
            String description = jsonObject.getString("description");

            NotificationUtil.getInstance().createNotificationForHigh(title, description);
        } else if (chatType == 1) {
            NotificationUtil.getInstance().createNotificationForHigh("新消息通知", "一条来自 " + chatUserName + " 的新消息");
        } else if (chatType == 2) {
            NotificationUtil.getInstance().createNotificationForHigh("新群聊通知", "一条来自 " + chatUserName + " 的新消息");
        }
    }

    /**
     * 维护聊天列表
     * @param fromMsgPush
     * @param chatType
     * @param curUserId
     * @param chatUserId
     * @param chatUserName
     * @param inChat
     * @param msg
     */
    public void updateLocalContact(boolean fromMsgPush, int chatType, int curUserId, int chatUserId,
                                   String chatUserName, int inChat, String msg) {
        System.out.println("===> 更新本地聊天人列表 fromMsgPush = " + fromMsgPush);
         String dir = "/" + CONTACTS_DIR + "/" + curUserId + "/";
        String fileName = "";

        // 只有chatType =2 时 msg 有可能为空（新建群&确认入群时维护联系人需要）
        if (msg == null && chatType != 2) {
            System.out.println("===> 维护聊天列表时，msg 解析失败 msg = " + msg);
            return;
        }

        JSONObject jsonObject = null;
        if (msg != null) {
            try {
                jsonObject = new JSONObject(msg);
            } catch (JSONException e) {
                System.out.println("===> 维护聊天列表时，msg 解析失败 msg = " + msg);
                return;
            }
        }

        Map<String, Object> chats = new HashMap<>();
        chats.put("chat_type", chatType);
        if (chatType == 0) {
            fileName = "notification";
            if (!fromMsgPush) {
                try {
//                    int lastMsgId = jsonObject.getInt("last_msg_id");
                    String lastMsg = jsonObject.getString("last_msg");
                    String lastMsgTime = jsonObject.getString("last_msg_time");
                    int unread = jsonObject.getInt("unread");

                    chats.put("id", curUserId);
                    chats.put("name", "通知消息");
//                    chats.put("last_msg_id", lastMsgId);
                    chats.put("last_msg", lastMsg);
                    chats.put("last_msg_time", lastMsgTime);
                    chats.put("unread", unread);
                } catch (JSONException e) {
                    System.out.println("===> 维护聊天列表时，msg 解析失败 msg = " + msg);
                    return;
                }
            } else {
                try {
//                    int lastMsgId = jsonObject.getInt("id");
                    String lastMsg = jsonObject.getString("context");
                    String lastMsgTime = jsonObject.getString("timer");

                    chats.put("id", curUserId);
                    chats.put("name", "通知消息");
//                    chats.put("last_msg_id", lastMsgId);
                    chats.put("last_msg", lastMsg);
                    chats.put("last_msg_time", lastMsgTime);
                } catch (JSONException e) {
                    System.out.println("===> 维护聊天列表时，msg 解析失败 msg = " + msg);
                    return;
                }
            }
        } else if (chatType == 1) {
            fileName = "private_" + chatUserId;
            if (!fromMsgPush) {
                try {
//                    int lastMsgId = jsonObject.getInt("last_msg_id");
                    String lastMsg = jsonObject.getString("last_msg");
                    String lastMsgTime = jsonObject.getString("last_msg_time");
                    int unread = jsonObject.getInt("unread");

                    chats.put("id", curUserId);
                    chats.put("chat_id", chatUserId);
                    chats.put("name", chatUserName);
//                    chats.put("last_msg_id", lastMsgId);
                    chats.put("last_msg", lastMsg);
                    chats.put("last_msg_time", lastMsgTime);
                    chats.put("unread", unread);
                } catch (JSONException e) {
                    System.out.println("===> 维护聊天列表时，msg 解析失败 msg = " + msg);
                    return;
                }
            } else {
                try {
//                    int lastMsgId = jsonObject.getInt("id");
                    String lastMsg = jsonObject.getString("context");
                    String lastMsgTime = jsonObject.getString("timer");

                    chats.put("id", curUserId);
                    chats.put("chat_id", chatUserId);
                    chats.put("name", chatUserName);
//                    chats.put("last_msg_id", lastMsgId);
                    chats.put("last_msg", lastMsg);
                    chats.put("last_msg_time", lastMsgTime);
                } catch (JSONException e) {
                    System.out.println("===> 维护聊天列表时，msg 解析失败 msg = " + msg);
                    return;
                }
            }

        } else if (chatType == 2) {
            fileName = "group_" + chatUserId;
            if (msg == null) {
                File file = new File(context.getFilesDir() + dir, fileName);
                if (file.exists()) {
                    return ;
                }
                chats.put("id", chatUserId);
                chats.put("name", chatUserName);
                chats.put("unread", 0);
            } else {
                if (!fromMsgPush) {
                    try {
//                        int lastMsgId = jsonObject.getInt("last_msg_id");
                        String lastMsg = jsonObject.getString("last_msg");
                        String lastMsgTime = jsonObject.getString("last_msg_time");
                        int unread = jsonObject.getInt("unread");

                        chats.put("id", chatUserId);
                        chats.put("name", chatUserName);
//                        chats.put("last_msg_id", lastMsgId);
                        chats.put("last_msg", lastMsg);
                        chats.put("last_msg_time", lastMsgTime);
                        chats.put("unread", unread);
                    } catch (JSONException e) {
                        System.out.println("===> 维护聊天列表时，msg 解析失败 msg = " + msg);
                        return;
                    }
                } else {
                    try {
//                        int lastMsgId = jsonObject.getInt("id");
                        String lastMsg = jsonObject.getString("context");
                        String lastMsgTime = jsonObject.getString("timer");

                        chats.put("id", chatUserId);
                        chats.put("name", chatUserName);
//                        chats.put("last_msg_id", lastMsgId);
                        chats.put("last_msg", lastMsg);
                        chats.put("last_msg_time", lastMsgTime);
                    } catch (JSONException e) {
                        System.out.println("===> 维护聊天列表时，msg 解析失败 msg = " + msg);
                        return;
                    }
                }

            }

        }

        if (fromMsgPush) {
            // in_chat=0 当前不在聊天框， in_chat=1 当前在聊天框
            int unread = 0;
            if (inChat == 0) {
                unread = 1;
                List<String> msgs = readLinesFromFile(fileName, LAST_LINE, dir);
                if (!msgs.isEmpty()) {
                    try {
                        JSONObject json = new JSONObject(msgs.get(0));
                        unread += json.getInt("unread");
                    } catch (JSONException e) {
                        System.out.println("===> 维护聊天列表时，msg 解析失败 msg = " + msg);
                        return ;
                    }
                }
            }
            chats.put("unread", unread);
        }

        List<String> writeMsgs = new ArrayList<>(1);
        String wmsg = dictToJsonStr(chats);
        writeMsgs.add(wmsg);
        writeLinesToFile(writeMsgs, fileName, dir, false);
    }



    // 按行写入文件
    public void writeLinesToFile(List<String> lines, String fileName, String dir, boolean append) {
        // Get the file path for internal storage
        String filePath = context.getFilesDir() + dir + fileName;
        if (append) {
            System.out.println("准备追加 " + lines.size() + " 条消息到文件 " + filePath);
        } else {
            System.out.println("准备更新文件 " + filePath);
        }

        if (lines.isEmpty()) {
            return ;
        }

        // 检查文件是否存在
        File file = new File(filePath);
        // 获取文件所在目录
        File directory = file.getParentFile();

        // 检查目录是否存在，如果不存在，则创建
        if (!directory.exists()) {
            boolean created = directory.mkdirs();

            if (created) {
                System.out.println("Directory created successfully.");
            } else {
                System.out.println("Failed to create directory.");
            }
        }

        if (!file.exists()) {
            try {
                // 创建文件
                boolean created = file.createNewFile();
                if (created) {
                    System.out.println("File created successfully.");
                } else {
                    System.out.println("Failed to create file.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("File already exists.");
        }


        try (FileOutputStream fos = new FileOutputStream(filePath, append);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos))) {

            for (String line : lines) {
                writer.write(line);
                writer.newLine();  // Add a newline character after each line
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // 按行读取文件数据
    // -1 读取所有数据
    // 1 读取最后一行的消息
    public List<String> readLinesFromFile(String fileName, int count, String dir) {
        // Get the file path for internal storage
        String filePath = context.getFilesDir() + dir + fileName;
        System.out.println("===> 读取文件： " + filePath);
        List<String> allLines = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {

            // Read all line data
            String line;
            while ((line = reader.readLine()) != null) {
                allLines.add(line);
            }

            int size = allLines.size();
            if (count == LAST_LINE && size > 0){
                String lastLine = allLines.get(size - 1);
                allLines.clear();
                allLines.add(lastLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return allLines;
    }


    public void deleteFile(String fileName, String dir) {
        try {
            // Get the file path for internal storage
            String filePath = context.getFilesDir() + dir + fileName;
            File fileToDelete = new File(filePath);

            // Check if the file exists before attempting to delete
            if (fileToDelete.exists()) {
                if (fileToDelete.delete()) {
                    System.out.println("File deleted successfully.");
                } else {
                    System.err.println("Unable to delete the file.");
                }
            } else {
                System.err.println("File does not exist.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 查询指定目录下的文件列表
     * @param dir
     * @return
     */
    public List<String> getFileList(String dir) {
        System.out.println("===> 查询指定目录下的文件列表");

        List<String> fileList = new ArrayList<>();
        // 获取应用的私有文件目录
        File directory = new File(context.getFilesDir() + dir);
        if (directory.isDirectory()) {
            System.out.println("===> 开始遍历目录: " + directory.getAbsolutePath());
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    System.out.println("===> " + file.getName());
                    fileList.add(file.getName());
                }
            }
        }
        return fileList;
    }

    /**
     * 将 JSONArray 转换为 List<String>
     * @param jsonArray
     * @return
     */
    public static List<String> jsonArrayToList(JSONArray jsonArray) {
        List<String> list = new ArrayList<>();

        try {
            // Iterate through the JSONArray and add each item to the list
            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(jsonArray.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return list;
    }

    public static Map<String, Object> jsonToMap(JSONObject json) throws JSONException {
        Map<String, Object> map = new HashMap<>();

        // Iterate over the keys in the JSONObject
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = json.get(key);

            // If the value is another JSONObject, recursively convert it to a Map
            if (value instanceof JSONObject) {
                value = jsonToMap((JSONObject) value);
            }

            // Put the key-value pair into the map
            map.put(key, value);
        }

        return map;
    }

    public String dictToJsonStr(Map<String, Object> dict) {
        String jsonString = "";
        try {
            // Create a JSON object
            JSONObject jsonObject = new JSONObject();
            for (Map.Entry<String, Object> entry : dict.entrySet()) {
                jsonObject.put(entry.getKey(), entry.getValue());
            }

            // Convert JSON object to JSON string
            jsonString = jsonObject.toString();

            // Use the resulting JSON string as needed
            System.out.println("JSON String: " + jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonString;
    }




    // ======================== 文件上传和下载 ========================

    public void uploadFile(MultipartFile file) throws Exception {
        String dir = "/" + ATTACHMENTS_DIR + "/";
        String filePath = context.getFilesDir() + dir + file.getFilename();
        // 定义文件保存路径
        File destFile = new File(filePath);

        // 获取文件所在目录
        File directory = destFile.getParentFile();
        // 检查目录是否存在，如果不存在，则创建
        if (!directory.exists()) {
            boolean created = directory.mkdirs();

            if (created) {
                System.out.println("Directory created successfully.");
            } else {
                System.out.println("Failed to create directory.");
            }
        }

        if (destFile.exists()) {
            throw new Exception("文件已存在");
        }

        // 将文件保存到本地
        file.transferTo(destFile);
    }


    public FileInputStream downloadFile(String fileName) throws Exception {
        String dir = "/" + ATTACHMENTS_DIR + "/";
        String filePath = context.getFilesDir() + dir + fileName;
        // 定义文件保存路径
        File destFile = new File(filePath);

        // 检查目录 & 文件 是否存在
        if (!destFile.getParentFile().exists() || !destFile.exists()) {
            throw new Exception("文件已存在");
        }

        // 读取文件内容
        FileInputStream inputStream = new FileInputStream(destFile);
        return inputStream;
    }

}
