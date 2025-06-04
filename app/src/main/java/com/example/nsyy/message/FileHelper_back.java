package com.example.nsyy.message;

import android.content.Context;

import com.example.nsyy.utils.NotificationUtil;
import com.yanzhenjie.andserver.http.multipart.MultipartFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class FileHelper_back {

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

    private volatile static FileHelper_back uniqueInstance;
    private Context context;

    public void setContext(Context context) {
        this.context = context;
    }

    //采用Double CheckLock(DCL)实现单例
    public static FileHelper_back getInstance() {
        if (uniqueInstance == null) {
            synchronized (FileHelper_back.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new FileHelper_back();
                }
            }
        }
        return uniqueInstance;
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

        // 从服务器加载最新数据更新到本地
        String fileName = updateLocalDataByServer(type, dict, false);

        String dir = "/" + MESSAGES_DIR + "/" + curUserId + "/";
        // 从文件中读取消息
        List<String> messageInFile = readLinesFromFile(fileName, ALL_LINE, dir);
        int start = Integer.parseInt(dict.get("start"));
        int count = Integer.parseInt(dict.get("count"));

        // 使用 ListIterator 反向遍历 List
        ListIterator<String> listIterator = messageInFile.listIterator(messageInFile.size());
        while (listIterator.hasPrevious()) {
            String msg = listIterator.previous();
            try {
                // Convert JSON string to JSON object
                JSONObject jsonObject = new JSONObject(msg);
                String idStr = jsonObject.getString("id");
                int id = Integer.parseInt(idStr);
                if (id > start) {
                    JSONObject object = new JSONObject(msg);
                    Map<String, Object> jsonmsg = jsonToMap(object);
                    if (0 == ((Number) jsonmsg.get("chat_type")).intValue()) {
                        jsonmsg.put("context", jsonToMap(new JSONObject(((String) jsonmsg.get("context")))));
                    }
                    messages.add(jsonmsg);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (messages.size() >= count){
                break;
            }
        }
        Collections.reverse(messages);
        return messages;
    }


    public String updateLocalDataByServer(int type, Map<String, String> dict, boolean async) {
        // 根据文件中是否存在消息，更新消息
        String curUserId = dict.get("cur_user_id");
        String dir = "/" + MESSAGES_DIR + "/" + curUserId + "/";
        String fileName = "";
        if (type == 0) {
            // 通知消息
            String cur_user_id = dict.get("cur_user_id");
            fileName = "nsyy_notification_message_" + cur_user_id;
            List<String> lastMsg = readLinesFromFile(fileName, LAST_LINE, dir);

            if (!lastMsg.isEmpty()) {
                // 文件存在消息，根据最后一条消息进行更新
                try {
                    // Convert JSON string to JSON object
                    JSONObject jsonObject = new JSONObject(lastMsg.get(0));
                    String id = jsonObject.getString("id");

                    Map<String, Object> params = new HashMap<>();
                    params.put("read_type", dict.get("read_type"));
                    params.put("cur_user_id", cur_user_id);
                    params.put("start", id);
                    params.put("count", Integer.toString(500));

                    if (async) {
                        asynchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), dir, fileName);
                    } else {
                        synchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), dir, fileName);
                    }
                } catch (JSONException e) {
                    System.out.println("===> FileHelper.updateLocalDataByServer error");
                    e.printStackTrace();
                }
            } else {
                // 文件不存在消息，直接从数据库查询
                Map<String, Object> params = new HashMap<>();
                params.put("read_type", dict.get("read_type"));
                params.put("cur_user_id", cur_user_id);
                params.put("start", Integer.toString(-1));
                params.put("count", Integer.toString(500));

                if (async) {
                    asynchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), dir, fileName);
                } else {
                    synchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), dir, fileName);
                }
            }
        } else if (type == 1) {
            // 私聊消息
            String sender = dict.get("cur_user_id");
            String receiver = dict.get("chat_user_id");

            if (Integer.parseInt(sender) <= Integer.parseInt(receiver)) {
                fileName =  "nsyy_private_message_" + sender + "_" + receiver;
            } else {
                fileName =  "nsyy_private_message_" + receiver + "_" + sender;
            }

            List<String> lastMsg = readLinesFromFile(fileName, LAST_LINE, dir);
            if (!lastMsg.isEmpty()) {
                // 文件存在消息，根据最后一条消息进行更新
                try {
                    // Convert JSON string to JSON object
                    JSONObject jsonObject = new JSONObject(lastMsg.get(0));
                    String id = jsonObject.getString("id");

                    Map<String, Object> params = new HashMap<>();
                    params.put("read_type", dict.get("read_type"));
                    params.put("cur_user_id", sender);
                    params.put("chat_user_id", receiver);
                    params.put("start", id);
                    params.put("count", Integer.toString(500));

                    if (async) {
                        asynchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), dir, fileName);
                    } else {
                        synchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), dir, fileName);
                    }

                } catch (JSONException e) {
                    System.out.println("===> FileHelper.updateLocalDataByServer error");
                    e.printStackTrace();
                }
            } else {
                // 文件不存在消息，直接从数据库查询
                Map<String, Object> params = new HashMap<>();
                params.put("read_type", dict.get("read_type"));
                params.put("cur_user_id", sender);
                params.put("chat_user_id", receiver);
                params.put("start", Integer.toString(-1));
                params.put("count", Integer.toString(500));

                if (async) {
                    asynchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), dir, fileName);
                } else {
                    synchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), dir, fileName);
                }
            }


        } else if (type == 2) {
            // 群聊消息
            String cur_user_id = dict.get("cur_user_id");
            String chat_user_id = dict.get("chat_user_id");

            fileName =  "nsyy_group_message_" + chat_user_id;
            List<String> lastMsg = readLinesFromFile(fileName, LAST_LINE, dir);
            if (!lastMsg.isEmpty()) {
                // 文件存在消息，根据最后一条消息进行更新
                try {
                    // Convert JSON string to JSON object
                    JSONObject jsonObject = new JSONObject(lastMsg.get(0));
                    String id = jsonObject.getString("id");

                    Map<String, Object> params = new HashMap<>();
                    params.put("read_type", dict.get("read_type"));
                    params.put("cur_user_id", cur_user_id);
                    params.put("chat_user_id", chat_user_id);
                    params.put("start", id);
                    params.put("count", Integer.toString(500));

                    if (async) {
                        asynchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), dir, fileName);
                    } else {
                        synchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), dir, fileName);
                    }

                } catch (JSONException e) {
                    System.out.println("===> FileHelper.updateLocalDataByServer error");
                    e.printStackTrace();
                }
            } else {
                // 文件不存在消息，直接从数据库查询
                Map<String, Object> params = new HashMap<>();
                params.put("read_type", dict.get("read_type"));
                params.put("cur_user_id", cur_user_id);
                params.put("chat_user_id", chat_user_id);
                params.put("start", Integer.toString(-1));
                params.put("count", Integer.toString(500));

                if (async) {
                    asynchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), dir, fileName);
                } else {
                    synchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), dir, fileName);
                }
            }
        }

        return fileName;
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
     * @param prevMsgId
     * @param userId
     * @param msg
     */
    public void writeMessageToLocal(int prevMsgId, int userId, String msg) {
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
                    fileName = "nsyy_notification_message_" + Integer.toString(userId);
                    List<String> lastMsg = readLinesFromFile(fileName, LAST_LINE, dir);
                    if (lastMsg.isEmpty()) {
                        // 文件之前不存在，直接存入
                        writeLinesToFile(writemsg, fileName, dir, true);
                    } else {
                        // Convert JSON string to JSON object
                        JSONObject jsonObject = new JSONObject(lastMsg.get(0));
                        int id = jsonObject.getInt("id");

                        if (id == prevMsgId) {
                            writeLinesToFile(writemsg, fileName, dir, true);
                        }
                    }
                }
            } else if (type == 1) {
                // 私聊
                String sender = newMessage.getString("sender");
                String receiver = newMessage.getString("receiver");
                if (Integer.parseInt(sender) <= Integer.parseInt(receiver)) {
                    fileName = "nsyy_private_message_" + sender + "_" + receiver;
                } else {
                    fileName = "nsyy_private_message_" + receiver + "_" + sender;
                }
                List<String> lastMsg = readLinesFromFile(fileName, LAST_LINE, dir);
                if (lastMsg.isEmpty()) {
                    // 文件之前不存在，直接存入
                    writeLinesToFile(writemsg, fileName, dir, true);
                } else {
                    // Convert JSON string to JSON object
                    JSONObject jsonObject = new JSONObject(lastMsg.get(0));
                    int id = jsonObject.getInt("id");

                    if (id == prevMsgId) {
                        writeLinesToFile(writemsg, fileName, dir, true);
                    }
                }
            } else if (type == 2) {
                // 群聊
                String groupId = newMessage.getString("group_id");
                fileName = "nsyy_group_message_" + groupId;
                List<String> lastMsg = readLinesFromFile(fileName, LAST_LINE, dir);
                if (lastMsg.isEmpty()) {
                    // 文件之前不存在，直接存入
                    writeLinesToFile(writemsg, fileName, dir, true);
                } else {
                    // Convert JSON string to JSON object
                    JSONObject jsonObject = new JSONObject(lastMsg.get(0));
                    int id = jsonObject.getInt("id");

                    if (id == prevMsgId) {
                        writeLinesToFile(writemsg, fileName, dir, true);
                    }
                }
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
                    int lastMsgId = jsonObject.getInt("last_msg_id");
                    String lastMsg = jsonObject.getString("last_msg");
                    String lastMsgTime = jsonObject.getString("last_msg_time");
                    int unread = jsonObject.getInt("unread");

                    chats.put("id", curUserId);
                    chats.put("name", "通知消息");
                    chats.put("last_msg_id", lastMsgId);
                    chats.put("last_msg", lastMsg);
                    chats.put("last_msg_time", lastMsgTime);
                    chats.put("unread", unread);
                } catch (JSONException e) {
                    System.out.println("===> 维护聊天列表时，msg 解析失败 msg = " + msg);
                    return;
                }
            } else {
                try {
                    int lastMsgId = jsonObject.getInt("id");
                    String lastMsg = jsonObject.getString("context");
                    String lastMsgTime = jsonObject.getString("timer");

                    chats.put("id", curUserId);
                    chats.put("name", "通知消息");
                    chats.put("last_msg_id", lastMsgId);
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
                    int lastMsgId = jsonObject.getInt("last_msg_id");
                    String lastMsg = jsonObject.getString("last_msg");
                    String lastMsgTime = jsonObject.getString("last_msg_time");
                    int unread = jsonObject.getInt("unread");

                    chats.put("id", curUserId);
                    chats.put("chat_id", chatUserId);
                    chats.put("name", chatUserName);
                    chats.put("last_msg_id", lastMsgId);
                    chats.put("last_msg", lastMsg);
                    chats.put("last_msg_time", lastMsgTime);
                    chats.put("unread", unread);
                } catch (JSONException e) {
                    System.out.println("===> 维护聊天列表时，msg 解析失败 msg = " + msg);
                    return;
                }
            } else {
                try {
                    int lastMsgId = jsonObject.getInt("id");
                    String lastMsg = jsonObject.getString("context");
                    String lastMsgTime = jsonObject.getString("timer");

                    chats.put("id", curUserId);
                    chats.put("chat_id", chatUserId);
                    chats.put("name", chatUserName);
                    chats.put("last_msg_id", lastMsgId);
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
                        int lastMsgId = jsonObject.getInt("last_msg_id");
                        String lastMsg = jsonObject.getString("last_msg");
                        String lastMsgTime = jsonObject.getString("last_msg_time");
                        int unread = jsonObject.getInt("unread");

                        chats.put("id", chatUserId);
                        chats.put("name", chatUserName);
                        chats.put("last_msg_id", lastMsgId);
                        chats.put("last_msg", lastMsg);
                        chats.put("last_msg_time", lastMsgTime);
                        chats.put("unread", unread);
                    } catch (JSONException e) {
                        System.out.println("===> 维护聊天列表时，msg 解析失败 msg = " + msg);
                        return;
                    }
                } else {
                    try {
                        int lastMsgId = jsonObject.getInt("id");
                        String lastMsg = jsonObject.getString("context");
                        String lastMsgTime = jsonObject.getString("timer");

                        chats.put("id", chatUserId);
                        chats.put("name", chatUserName);
                        chats.put("last_msg_id", lastMsgId);
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


    /**
     * 从服务器加载离线消息，更新本地消息
     * @param type
     * @param dict
     */
//    public void updateLocalData(int type, Map<String, String> dict) {
//        // 根据文件中是否存在消息，更新消息
//        String fileName = "";
//        String dir = MESSAGES_DIR + "/" + dict.get("cur_user_id") + "/";
//        if (type == 0) {
//            // 通知消息
//            String cur_user_id = dict.get("cur_user_id");
//            fileName = "nsyy_notification_message_" + cur_user_id;
//            List<String> lastMsg = readLinesFromFile(fileName, LAST_LINE, dir);
//
//            if (!lastMsg.isEmpty()) {
//                // 文件存在消息，根据最后一条消息进行更新
//                try {
//                    // Convert JSON string to JSON object
//                    JSONObject jsonObject = new JSONObject(lastMsg.get(0));
//                    String id = jsonObject.getString("id");
//
//                    Map<String, Object> params = new HashMap<>();
//                    params.put("read_type", dict.get("read_type"));
//                    params.put("cur_user_id", dict.get("cur_user_id"));
//                    params.put("chat_user_id", dict.get("chat_user_id"));
//                    params.put("start", Integer.toString(Integer.parseInt(id) + 1));
//                    params.put("count", Integer.toString(500));
//
//                    synchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), Integer.parseInt(dict.get("cur_user_id")), fileName);
//
//                } catch (JSONException e) {
//                    System.out.println("===> FileHelper.updateLocalDataAndReturnMsg error");
//                    e.printStackTrace();
//                }
//            } else {
//                // 文件不存在消息，直接从数据库查询
//                Map<String, Object> params = new HashMap<>();
//                params.put("read_type", dict.get("read_type"));
//                params.put("cur_user_id", dict.get("cur_user_id"));
//                params.put("chat_user_id", dict.get("chat_user_id"));
//                params.put("start", Integer.toString(-1));
//                params.put("count", Integer.toString(500));
//
//                synchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), Integer.parseInt(dict.get("cur_user_id")), fileName);
//            }
//        } else if (type == 1) {
//            // 私聊消息
//            String sender = dict.get("cur_user_id");
//            String receiver = dict.get("chat_user_id");
//
//            if (Integer.parseInt(sender) <= Integer.parseInt(receiver)) {
//                fileName =  "nsyy_private_message_" + sender + "_" + receiver;
//            } else {
//                fileName =  "nsyy_private_message_" + receiver + "_" + sender;
//            }
//
//            List<String> lastMsg = readLinesFromFile(fileName, LAST_LINE, dir);
//            if (!lastMsg.isEmpty()) {
//                // 文件存在消息，根据最后一条消息进行更新
//                try {
//                    // Convert JSON string to JSON object
//                    JSONObject jsonObject = new JSONObject(lastMsg.get(0));
//                    String id = jsonObject.getString("id");
//
//                    Map<String, Object> params = new HashMap<>();
//                    params.put("read_type", dict.get("read_type"));
//                    params.put("cur_user_id", sender);
//                    params.put("chat_user_id", receiver);
//                    params.put("start", Integer.toString(Integer.parseInt(id) + 1));
//                    params.put("count", Integer.toString(500));
//
//                    synchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), Integer.parseInt(sender), fileName);
//
//                } catch (JSONException e) {
//                    System.out.println("===> FileHelper.updateLocalDataAndReturnMsg error");
//                    e.printStackTrace();
//                }
//            } else {
//                // 文件不存在消息，直接从数据库查询
//                Map<String, Object> params = new HashMap<>();
//                params.put("read_type", dict.get("read_type"));
//                params.put("cur_user_id", sender);
//                params.put("chat_user_id", receiver);
//                params.put("start", Integer.toString(-1));
//                params.put("count", Integer.toString(500));
//
//                synchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), Integer.parseInt(sender), fileName);
//            }
//
//
//        } else if (type == 2) {
//            // 群聊消息
//            String sender = dict.get("cur_user_id");
//            String receiver = dict.get("chat_user_id");
//
//            fileName =  "nsyy_group_message_" + receiver;
//            List<String> lastMsg = readLinesFromFile(fileName, LAST_LINE, dir);
//            if (!lastMsg.isEmpty()) {
//                // 文件存在消息，根据最后一条消息进行更新
//                try {
//                    // Convert JSON string to JSON object
//                    JSONObject jsonObject = new JSONObject(lastMsg.get(0));
//                    String id = jsonObject.getString("id");
//
//                    Map<String, Object> params = new HashMap<>();
//                    params.put("read_type", dict.get("read_type"));
//                    params.put("cur_user_id", sender);
//                    params.put("chat_user_id", receiver);
//                    params.put("start", Integer.toString(Integer.parseInt(id) + 1));
//                    params.put("count", Integer.toString(500));
//
//                    synchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), Integer.parseInt(sender), fileName);
//
//                } catch (JSONException e) {
//                    System.out.println("===> FileHelper.updateLocalDataAndReturnMsg error");
//                    e.printStackTrace();
//                }
//            } else {
//                // 文件不存在消息，直接从数据库查询
//                Map<String, Object> params = new HashMap<>();
//                params.put("read_type", dict.get("read_type"));
//                params.put("cur_user_id", sender);
//                params.put("chat_user_id", receiver);
//                params.put("start", Integer.toString(-1));
//                params.put("count", Integer.toString(500));
//
//                synchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), Integer.parseInt(sender), fileName);
//            }
//        }
//
//    }


    /**
     * 同步更新聊天人列表
     * @param urlString
     * @param userId
     */
    public void synchronousUpdateContactList(String urlString, int userId) {
        try {
            System.out.println("===> fetch contact list from server: url = " + urlString + "  user_id = " + userId);
            // Specify the URL for the HTTP POST request
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);

            // Set the request headers (optional)
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");

            // Write the JSON payload to the request body
            Map<String, Object> param = new HashMap<>();
            param.put("user_id", userId);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = dictToJsonStr(param).getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Read the response from the server
            String responseData = null;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                responseData = response.toString();
                System.out.println("Response: " + responseData);
            }

            // Convert JSON string to JSON object
            JSONObject jsonObject = new JSONObject(responseData);
            int code = jsonObject.getInt("code");
            // 查询成功，将查询到的消息写入文件
            if (code == 20000) {
                JSONArray msgs = jsonObject.getJSONArray("data");
                for (int i = 0; i < msgs.length(); i++) {
                    JSONObject contact = msgs.getJSONObject(i);
                    int chatType = contact.getInt("chat_type");
                    if (chatType == 0) {
                        int id = contact.getInt("id");
                        String name = contact.getString("name");
                        updateLocalContact(false, chatType, id, id, name, 0, contact.toString());
                    } else if (chatType == 1) {
                        int chatId = contact.getInt("chat_id");
                        String name = contact.getString("name");
                        updateLocalContact(false, chatType, userId, chatId, name, 0, contact.toString());
                    } else if (chatType == 2) {
                        int chatId = contact.getInt("id");
                        String name = contact.getString("name");
                        if (contact.has("last_msg")) {
                            updateLocalContact(false, chatType, userId, chatId, name, 0, contact.toString());
                        } else {
                            updateLocalContact(false, chatType, userId, chatId, name, 0, null);
                        }

                    }
                }
            }

            // Close the connection
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }



    /**
     * 异步更新离线消息
     * @param url
     * @param curUserId
     */
    public void asynchronousUpdateMessage(String url, int curUserId) {
        System.out.println("===> 开始异步更新离线消息");
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
                            int chatType = jsonObject.getInt("chat_type");
                            Map<String, String> param = new HashMap<>();
                            param.put("url", url);

                            if (chatType == 0) {
                                param.put("read_type", Integer.toString(chatType));
                                param.put("cur_user_id", Integer.toString(curUserId));
                                updateLocalDataByServer(chatType, param, true);
                            } else if (chatType == 1) {
                                int chatId = jsonObject.getInt("chat_id");
                                param.put("read_type", Integer.toString(chatType));
                                param.put("cur_user_id", Integer.toString(curUserId));
                                param.put("chat_user_id", Integer.toString(chatId));
                                updateLocalDataByServer(chatType, param, true);
                            } else if (chatType == 2) {
                                int chatId = jsonObject.getInt("id");
                                param.put("read_type", Integer.toString(chatType));
                                param.put("cur_user_id", Integer.toString(curUserId));
                                param.put("chat_user_id", Integer.toString(chatId));
                                updateLocalDataByServer(chatType, param, true);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }

    }


    /**
     * 同步处理 http post 请求
     *
     * @param urlString
     * @param jsonInputString
     * @param fileName
     */
    public void synchronousHttpPostRequest(String urlString, String jsonInputString, String dir, String fileName) {
        try {
            // Specify the URL for the HTTP POST request
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);

            // Set the request headers (optional)
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");

            // Write the JSON payload to the request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Get the response code
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // Read the response from the server
            String responseData = null;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                responseData = response.toString();
                System.out.println("Response: " + responseData);
            }

            // Convert JSON string to JSON object
            JSONObject jsonObject = new JSONObject(responseData);
            int code = jsonObject.getInt("code");
            // 查询成功，将查询到的消息写入文件
            if (code == 20000) {
                JSONArray msgs = jsonObject.getJSONArray("data");
                writeLinesToFile(jsonArrayToList(msgs), fileName, dir, true);
            }

            // Close the connection
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 异步处理 http post 请求
     *
     * @param urlString
     * @param jsonInputString
     * @param fileName
     */
    public void asynchronousHttpPostRequest(String urlString, String jsonInputString, String dir, String fileName)  {
        AsyncHttpTask task = new AsyncHttpTask(fileName, dir);
        task.execute(urlString, jsonInputString);
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
        System.out.println("读取文件： " + filePath);
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
