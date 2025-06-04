package com.example.nsyy.message;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MessageDatabaseHelper extends SQLiteOpenHelper {
    // 数据库版本，每次修改表结构需要递增
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "message_system.db";

    // 表名常量
    private static final String TABLE_MESSAGES = "messages";
    private static final String TABLE_CONTACTS = "contacts";
//    private static final String TABLE_ATTACHMENTS = "attachments";
//    private static final String TABLE_MESSAGE_EXTRA = "message_extra";
//    private static final String TABLE_CONTACT_EXTRA = "contact_extra";

    // 消息表核心字段
    private static final String COLUMN_ID = "mid";
    private static final String COLUMN_CHAT_TYPE = "chat_type";
    private static final String COLUMN_CONTEXT_TYPE = "context_type";
    private static final String COLUMN_SENDER_ID = "sender";
    private static final String COLUMN_SENDER_NAME = "sender_name";
    private static final String COLUMN_RECEIVER_ID = "receiver";
    private static final String COLUMN_RECEIVER_NAME = "receiver_name";
    private static final String COLUMN_GROUP_ID = "group_id";
    private static final String COLUMN_CONTEXT = "context";
    private static final String COLUMN_TIMESTAMP = "timer";
    private static final String COLUMN_IS_READ = "is_read";

    // 联系人表核心字段
    private static final String COLUMN_CONTACT_CHAT_TYPE = "chat_type";
    private static final String COLUMN_USER_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_CHAT_ID = "chat_id";
    private static final String COLUMN_LAST_MSG = "last_msg";
    private static final String COLUMN_LAST_MSG_TIME = "last_msg_time";
    private static final String COLUMN_UNREAD_COUNT = "unread";

//    // 附件表核心字段
//    private static final String COLUMN_FILE_NAME = "file_name";
//    private static final String COLUMN_FILE_PATH = "file_path";
//    private static final String COLUMN_FILE_SIZE = "file_size";
//    private static final String COLUMN_MIME_TYPE = "mime_type";

//    // 扩展表通用字段
//    private static final String COLUMN_ENTITY_ID = "entity_id";
//    private static final String COLUMN_KEY = "key";
//    private static final String COLUMN_VALUE = "value";

    private Context context;
    private static MessageDatabaseHelper instance;

    public static synchronized MessageDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new MessageDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private MessageDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    /**
     * 创建表
     * @param db The database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        createMessageTables(db);
        createContactTables(db);
//        createAttachmentTables(db);
    }

    /**
     * 升级表结构
     * @param db The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 版本迁移逻辑
        if (oldVersion < 2) {
            upgradeToVersion2(db);
        }
        if (oldVersion < 3) {
            upgradeToVersion3(db);
        }
    }

    // ======================== 表创建方法 ========================

    private void createMessageTables(SQLiteDatabase db) {
        // 消息主表
        String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_CHAT_TYPE + " INTEGER NOT NULL," // 0-通知 1-私聊 2-群聊
                + COLUMN_CONTEXT_TYPE + " INTEGER," // 0-通知 1-私聊 2-群聊
                + COLUMN_SENDER_ID + " INTEGER,"
                + COLUMN_SENDER_NAME + " TEXT,"
                + COLUMN_RECEIVER_ID + " INTEGER,"
                + COLUMN_RECEIVER_NAME + " TEXT,"
                + COLUMN_GROUP_ID + " INTEGER,"
                + COLUMN_CONTEXT + " TEXT NOT NULL,"
                + COLUMN_TIMESTAMP + " TEXT NOT NULL,"
                + COLUMN_IS_READ + " INTEGER DEFAULT 0"
                + ")";
        db.execSQL(CREATE_MESSAGES_TABLE);

        // 创建索引
        db.execSQL("CREATE INDEX idx_message_type ON " + TABLE_MESSAGES + "(" + COLUMN_CHAT_TYPE + ")");
        db.execSQL("CREATE INDEX idx_message_sender ON " + TABLE_MESSAGES + "(" + COLUMN_SENDER_ID + ")");
        db.execSQL("CREATE INDEX idx_message_receiver ON " + TABLE_MESSAGES + "(" + COLUMN_RECEIVER_ID + ")");
        db.execSQL("CREATE INDEX idx_message_group ON " + TABLE_MESSAGES + "(" + COLUMN_GROUP_ID + ")");
    }

    private void createContactTables(SQLiteDatabase db) {
        // 联系人主表
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_CONTACTS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_CONTACT_CHAT_TYPE + " INTEGER NOT NULL," // 0-通知 1-私聊 2-群聊
                + COLUMN_USER_ID + " INTEGER NOT NULL,"
                + COLUMN_CHAT_ID + " INTEGER,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_LAST_MSG + " TEXT,"
                + COLUMN_LAST_MSG_TIME + " TEXT,"
                + COLUMN_UNREAD_COUNT + " INTEGER DEFAULT 0,"
                + "UNIQUE(" + COLUMN_CONTACT_CHAT_TYPE + "," + COLUMN_USER_ID + "," + COLUMN_CHAT_ID + ")"
                + ")";
        db.execSQL(CREATE_CONTACTS_TABLE);

        // 创建索引
        db.execSQL("CREATE INDEX idx_contact_type ON " + TABLE_CONTACTS + "(" + COLUMN_CONTACT_CHAT_TYPE + ")");
        db.execSQL("CREATE INDEX idx_contact_user ON " + TABLE_CONTACTS + "(" + COLUMN_USER_ID + ")");
    }

    // ======================== 数据库升级方法 ========================

    private void upgradeToVersion2(SQLiteDatabase db) {
        System.out.println("版本2升级：添加消息已读状态字段");
        // 版本2升级：添加消息已读状态字段
//        db.execSQL("ALTER TABLE " + TABLE_MESSAGES + " ADD COLUMN " + COLUMN_IS_READ + " INTEGER DEFAULT 0");
    }

    private void upgradeToVersion3(SQLiteDatabase db) {
        System.out.println("版本3升级：添加附件MIME类型字段");
        // 版本3升级：添加附件MIME类型字段
//        db.execSQL("ALTER TABLE " + TABLE_ATTACHMENTS + " ADD COLUMN " + COLUMN_MIME_TYPE + " TEXT");
    }

    // ======================== 消息相关操作 ========================

    public long saveMessage(Integer chatType, Integer contextType, Integer senderId, String senderName,
                            Integer receiverId, String receiverName, Integer groupId, String context, Map<String, Object> contextData,
                            String timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();

        // 将Map转为JSON字符串  通知消息 context 是json结构
        String contextJson = context;
        if (chatType == 0){
            contextJson = new JSONObject(contextData).toString();
        }

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            if (chatType != null) values.put(COLUMN_CHAT_TYPE, chatType);
            if (contextType != null) values.put(COLUMN_CONTEXT_TYPE, contextType);
            if (senderId != null) values.put(COLUMN_SENDER_ID, senderId);
            if (senderName != null) values.put(COLUMN_SENDER_NAME, senderName);
            if (receiverId != null) values.put(COLUMN_RECEIVER_ID, receiverId);
            if (receiverName != null) values.put(COLUMN_RECEIVER_NAME, receiverName);
            if (groupId != null) values.put(COLUMN_GROUP_ID, groupId);
            values.put(COLUMN_CONTEXT, contextJson);
            values.put(COLUMN_TIMESTAMP, timestamp);

            long messageId = db.insert(TABLE_MESSAGES, null, values);
            db.setTransactionSuccessful();
            return messageId;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save message to db", e);
        } finally {
            db.endTransaction();
        }
    }

    public List<Map<String, Object>> getMessages(int type, Integer userId, Integer chatUserId,
                                                 Integer groupId, int start, int count) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Map<String, Object>> messages = new ArrayList<>();

        // 构建查询条件
        String selection = COLUMN_CHAT_TYPE + " = ?";
        List<String> selectionArgs = new ArrayList<>();
        selectionArgs.add(String.valueOf(type));

        if (type == 1) { // 私聊
            selection += " AND ((" + COLUMN_SENDER_ID + " = ? AND " + COLUMN_RECEIVER_ID + " = ?) ";
            selection += " OR (" + COLUMN_SENDER_ID + " = ? AND " + COLUMN_RECEIVER_ID + " = ?))";
            selectionArgs.add(String.valueOf(userId));
            selectionArgs.add(String.valueOf(chatUserId));
            selectionArgs.add(String.valueOf(chatUserId));
            selectionArgs.add(String.valueOf(userId));
        } else if (type == 2) { // 群聊
            selection += " AND " + COLUMN_GROUP_ID + " = ?";
            selectionArgs.add(String.valueOf(groupId));
        } else if (type == 0) {
            selection += " AND " + COLUMN_RECEIVER_ID + " = ?";
            selectionArgs.add(String.valueOf(userId));
        }

        // 执行查询
        Cursor cursor = db.query(TABLE_MESSAGES,
                null, // 所有列
                selection,
                selectionArgs.toArray(new String[0]),
                null, null,
                COLUMN_TIMESTAMP + " DESC", // 按时间降序
                start + "," + count); // 分页

        try {
            while (cursor.moveToNext()) {
                Map<String, Object> message = cursorToMessage(cursor, type);
                messages.add(message);
            }
        } finally {
            cursor.close();
        }

        Collections.reverse(messages);
        return messages;
    }

    @SuppressLint("Range")
    private Map<String, Object> cursorToMessage(Cursor cursor, int chat_type) {
        Map<String, Object> message = new HashMap<>();
        message.put(COLUMN_ID, getIntSafe(cursor, COLUMN_ID, 0));
        message.put(COLUMN_CHAT_TYPE, getIntSafe(cursor, COLUMN_CHAT_TYPE, 0));
        message.put(COLUMN_CONTEXT_TYPE, getIntSafe(cursor, COLUMN_CONTEXT_TYPE, 0));
        message.put(COLUMN_SENDER_ID, getIntSafe(cursor, COLUMN_SENDER_ID, 0));
        message.put(COLUMN_SENDER_NAME, getStrSafe(cursor, COLUMN_SENDER_NAME, "Unknown"));
        message.put(COLUMN_RECEIVER_ID, getIntSafe(cursor, COLUMN_RECEIVER_ID, 0));
        message.put(COLUMN_RECEIVER_NAME, getStrSafe(cursor, COLUMN_RECEIVER_NAME, "Unknown"));
        message.put(COLUMN_GROUP_ID, getIntSafe(cursor, COLUMN_GROUP_ID, 0));
        message.put(COLUMN_IS_READ, getIntSafe(cursor, COLUMN_IS_READ, 0));
        message.put(COLUMN_TIMESTAMP, getStrSafe(cursor, COLUMN_TIMESTAMP, "Unknown"));

        String context_str = getStrSafe(cursor, COLUMN_CONTEXT, "");
        if (chat_type == 0) {
            try {
                JSONObject jsonObject = new JSONObject(context_str);
                message.put(COLUMN_CONTEXT, jsonToMap(jsonObject));
            } catch (JSONException e) {
                message.put(COLUMN_CONTEXT, context_str);
            }
        } else {
            message.put(COLUMN_CONTEXT, context_str);
        }

        return message;
    }

    // ======================== 联系人相关操作 ========================

    @SuppressLint("Range")
    public void updateContact(int type, Integer userId, Integer chatId, String name, String lastContext,
                              Map<String, Object> lastMsg, String lastMsgTime, int unreadCount) {
        SQLiteDatabase db = this.getWritableDatabase();

        db.beginTransaction();
        try {
            String selection = "";
            List<String> selectionArgs = new ArrayList<>();

            if (type == 0) {
                selection = COLUMN_CONTACT_CHAT_TYPE + " = ? AND " + COLUMN_USER_ID + " = ? ";
                selectionArgs.add(String.valueOf(type));
                selectionArgs.add(String.valueOf(userId));
            } else {
                selection = COLUMN_CONTACT_CHAT_TYPE + " = ? AND " + COLUMN_USER_ID + " = ? AND " + COLUMN_CHAT_ID + " = ? ";
                selectionArgs.add(String.valueOf(type));
                selectionArgs.add(String.valueOf(userId));
                selectionArgs.add(String.valueOf(chatId));
            }

            // 检查联系人是否存在
            Cursor cursor = db.query(TABLE_CONTACTS,
                    new String[]{COLUMN_ID, COLUMN_UNREAD_COUNT},  // 查询 ID 和 unreadCount
                    selection,
                    selectionArgs.toArray(new String[0]),
                    null, null, null);

            long contactId = -1;
            int oldUnreadCount = 0;  // 默认 0，如果不存在则不会进入更新逻辑

            if (cursor.moveToFirst()) {
                contactId = cursor.getLong(cursor.getColumnIndex(COLUMN_ID));
                oldUnreadCount = cursor.getInt(cursor.getColumnIndex(COLUMN_UNREAD_COUNT));  // 获取旧的 unreadCount
            }
            cursor.close();

            String lastMsgJson = type == 0 ? new JSONObject(lastMsg).toString() : lastContext;

            // 插入或更新联系人
            ContentValues values = new ContentValues();
            values.put(COLUMN_CONTACT_CHAT_TYPE, type);
            values.put(COLUMN_USER_ID, userId);
            if (chatId != null) values.put(COLUMN_CHAT_ID, chatId);
            values.put(COLUMN_NAME, name);
            values.put(COLUMN_LAST_MSG, lastMsgJson);
            values.put(COLUMN_LAST_MSG_TIME, lastMsgTime);

            // 新增时 unreadCount = 1（除非传入其他值）
            // 更新时 unreadCount = oldUnreadCount + 1
            values.put(COLUMN_UNREAD_COUNT, contactId == -1 ? (unreadCount == 0 ? 1 : unreadCount) : oldUnreadCount + 1);


            if (contactId == -1) {
                contactId = db.insert(TABLE_CONTACTS, null, values);
            } else {
                db.update(TABLE_CONTACTS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(contactId)});
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<Map<String, Object>> getContacts(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Map<String, Object>> contacts = new ArrayList<>();

        Cursor cursor = db.query(TABLE_CONTACTS,
                null,
                COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null, null,
                COLUMN_LAST_MSG_TIME + " DESC");

        try {
            while (cursor.moveToNext()) {
                Map<String, Object> contact = cursorToContact(cursor);
                contacts.add(contact);
            }
        } finally {
            cursor.close();
        }

        return contacts;
    }

    private Map<String, Object> cursorToContact(Cursor cursor) {
        Map<String, Object> contact = new HashMap<>();

        contact.put(COLUMN_ID, getIntSafe(cursor, COLUMN_ID, 0));
        contact.put(COLUMN_CONTACT_CHAT_TYPE, getIntSafe(cursor, COLUMN_CONTACT_CHAT_TYPE, 0));
        contact.put(COLUMN_USER_ID, getIntSafe(cursor, COLUMN_USER_ID, 0));
        contact.put(COLUMN_CHAT_ID, getIntSafe(cursor, COLUMN_CHAT_ID, 0));
        contact.put(COLUMN_NAME, getStrSafe(cursor, COLUMN_NAME, "Unknown"));
        contact.put(COLUMN_LAST_MSG_TIME, getStrSafe(cursor, COLUMN_LAST_MSG_TIME, "Unknown"));
        contact.put(COLUMN_UNREAD_COUNT, getIntSafe(cursor, COLUMN_UNREAD_COUNT, 0));

        String context_str = getStrSafe(cursor, COLUMN_LAST_MSG, "");
        try {
            JSONObject jsonObject = new JSONObject(context_str);
            contact.put(COLUMN_LAST_MSG, jsonToMap(jsonObject));
        } catch (JSONException e) {
            contact.put(COLUMN_LAST_MSG, context_str);
        }

        return contact;
    }



    private int getIntSafe(Cursor cursor, String columnName, int defaultValue) {
        try {
            int index = cursor.getColumnIndex(columnName);
            return index != -1 ? cursor.getInt(index) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String getStrSafe(Cursor cursor, String columnName, String defaultValue) {
        try {
            int index = cursor.getColumnIndex(columnName);
            return index != -1 ? cursor.getString(index) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // ======================== 其他工具方法 ========================

    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MESSAGES, null, null);
        db.delete(TABLE_CONTACTS, null, null);
//        db.delete(TABLE_MESSAGE_EXTRA, null, null);
//        db.delete(TABLE_CONTACT_EXTRA, null, null);
//        db.delete(TABLE_ATTACHMENTS, null, null);
    }

    // ======================== 原FileHelper方法适配 ========================

    public void notificationSaveToLocal(int inChat, int curUserId, Map<String, Object> message) {
        try {
            int chatType = (int) message.get("chat_type");
            Integer contextType = (Integer) message.getOrDefault("context_type", 0);
            if (contextType == null) {
                contextType = 0; // 默认值
            }
            Integer senderId = (Integer) message.getOrDefault("sender", 0);
            if (senderId == null) {
                senderId = 0; // 默认值
            }
            String senderName = (String) message.getOrDefault("sender_name", "");
            Integer receiverId = (Integer) message.getOrDefault("receiver", 0);
            if (receiverId == null) {
                receiverId = 0; // 默认值
            }
            String receiverName = (String) message.getOrDefault("receiver_name", "");
            Integer groupId = (Integer) message.getOrDefault("group_id", 0);
            if (groupId == null) {
                groupId = 0; // 默认值
            }
            Map<String, Object> contextData;
            String context = "";
            if (chatType == 0) {
                contextData = (Map) message.getOrDefault("context", "");
            } else {
                contextData = null;
                context = (String) message.getOrDefault("context", "");
            }
            String timestamp = (String) message.getOrDefault("timer", "");

            // 保存消息至db
            saveMessage(chatType, contextType, senderId, senderName, receiverId, receiverName,
                    groupId, context, contextData, timestamp);

            int chat_id = receiverId;
            String chat_name = "通知消息";
            if (chatType == 1) {
                chat_id = curUserId == senderId ? receiverId : senderId;
                chat_name = curUserId == senderId ? receiverName : senderName;
            } else if (chatType == 2) {
                chat_name = receiverName;
            }
            // 更新联系人
             updateContact(chatType, curUserId, chat_id, chat_name, context, contextData, timestamp, inChat == 0 ? 1 : 0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Map<String, Object>> queryMessage(int type, int curUserId, Map<String, String> dict) {
        int start = Integer.parseInt(dict.getOrDefault("start", "0"));
        int count = Integer.parseInt(dict.getOrDefault("count", "20"));
        Integer chatUserId = Integer.parseInt(dict.get("chat_user_id"));
        Integer groupId = Integer.parseInt(dict.get("chat_user_id"));
        return getMessages(type, curUserId, chatUserId, groupId, start, count);
    }

    public void updateUnread(int chatType, int curUserId, int chatUserId) {
        SQLiteDatabase db = this.getWritableDatabase();

        String whereClause;
        String[] whereArgs;

        if (chatType == 0) { // 通知
            whereClause = COLUMN_CONTACT_CHAT_TYPE + " = ? AND " + COLUMN_USER_ID + " = ?";
            whereArgs = new String[]{String.valueOf(chatType), String.valueOf(curUserId)};
        } else { // 私聊或群聊
            whereClause = COLUMN_CONTACT_CHAT_TYPE + " = ? AND " + COLUMN_CHAT_ID + " = ?";
            whereArgs = new String[]{String.valueOf(chatType), String.valueOf(chatUserId)};
        }

        ContentValues values = new ContentValues();
        values.put(COLUMN_UNREAD_COUNT, 0);

        db.update(TABLE_CONTACTS, values, whereClause, whereArgs);
    }

    public int getLocalContact(int curUserId, List<Map<String, Object>> result) {
        int allUnread = 0;

        List<Map<String, Object>> contacts = getContacts(curUserId); // 获取所有类型的联系人

        for (Map<String, Object> contact : contacts) {
            result.add(contact);
            allUnread += (int) contact.get(COLUMN_UNREAD_COUNT);
        }

        return allUnread;
    }







    // ======================== JSON转换工具方法 ========================

    public static Map<String, Object> jsonToMap(JSONObject json) throws JSONException {
        Map<String, Object> map = new HashMap<>();
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = json.get(key);
            if (value instanceof JSONObject) {
                value = jsonToMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }

    public static List<String> jsonArrayToList(JSONArray jsonArray) throws JSONException {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            list.add(jsonArray.getString(i));
        }
        return list;
    }
}
