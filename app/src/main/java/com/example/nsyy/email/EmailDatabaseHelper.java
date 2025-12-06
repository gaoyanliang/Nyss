package com.example.nsyy.email;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class EmailDatabaseHelper extends SQLiteOpenHelper {
    // 邮件详情查询地址
//    public static final String FETCH_EMAIL_URL = "http://192.168.124.9:8080/gyl/workstation/mail/query_mail";
    public static final String FETCH_EMAIL_URL = "http://oa.nsyy.com.cn:6080/gyl/workstation/mail/query_mail";

    // 数据库版本，每次修改表结构需要递增
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "email_system.db";

    // 邮件表
    private static final String TABLE_EMAIL = "email";

    // 邮件表核心字段
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_MESSAGE_ID = "message_id";
    private static final String COLUMN_UNREAD = "unread";
    private static final String COLUMN_SUBJECT = "subject";
    private static final String COLUMN_FROM = "froms";
    private static final String COLUMN_TO = "tos";
    private static final String COLUMN_CC = "cc";
    private static final String COLUMN_BCC = "bcc";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_FLAGS = "flags";
    private static final String COLUMN_REPLY = "reply";
    private static final String COLUMN_BODY = "body";
    private static final String COLUMN_ATTACHMENTS = "attachments";
    private static final String COLUMN_NAMES = "names";
    private static final String COLUMN_EXPAND = "expand";


    private static EmailDatabaseHelper instance;
    public static synchronized EmailDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new EmailDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }
    private EmailDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * 创建表
     * @param db The database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        createMessageTables(db);
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
        String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_EMAIL + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_MESSAGE_ID + " TEXT UNIQUE,"
                + COLUMN_UNREAD + " INTEGER NOT NULL,"
                + COLUMN_SUBJECT + " TEXT,"
                + COLUMN_FROM + " TEXT,"
                + COLUMN_TO + " TEXT,"
                + COLUMN_CC + " TEXT,"
                + COLUMN_BCC + " TEXT,"
                + COLUMN_DATE + " TEXT,"
                + COLUMN_FLAGS + " TEXT,"
                + COLUMN_REPLY + " TEXT,"
                + COLUMN_BODY + " TEXT,"
                + COLUMN_ATTACHMENTS + " TEXT,"
                + COLUMN_NAMES + " TEXT,"
                + COLUMN_EXPAND + " TEXT "
                + ")";
        db.execSQL(CREATE_MESSAGES_TABLE);
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



    // ======================== 邮件相关操作 ========================

    public Map<String, Object> queryEmail(String messageId, String userAccount, String mailbox) {
        Map<String, Object> email = getEmail(messageId);
        if (email == null || email.isEmpty()) {
            email = readEmailFromServerAndSave(messageId, userAccount, mailbox);
        }

        if (email != null || !email.isEmpty()) {
            return email;
        } else {
            return new HashMap<>();
        }
    }

    // 删除邮件
    public int deleteEmail(String messageId) {
        // 构建删除条件
        String selection = COLUMN_MESSAGE_ID + " = ?";
        List<String> selectionArgs = new ArrayList<>();
        selectionArgs.add(messageId);

        int deletedRows = 0;
        try {
            // 获取可写的数据库实例
            SQLiteDatabase db = this.getWritableDatabase();
            // 执行删除操作
            deletedRows = db.delete(TABLE_EMAIL, selection, selectionArgs.toArray(new String[0]));
        } catch (Exception e) {
            System.out.println("删除邮件异常 message_id = " + messageId + e.toString());
        }

        return deletedRows; // 返回被删除的行数
    }


    // 保存邮件
    public void save(String messageId, Integer unread, String subject, String froms,
                          String tos, String cc, String bcc, String date, String flags,
                          String reply, String body, String attachments, String names, String expand) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_MESSAGE_ID, messageId);
            values.put(COLUMN_UNREAD, unread);
            values.put(COLUMN_SUBJECT, subject);
            values.put(COLUMN_FROM, froms);
            values.put(COLUMN_TO, tos);
            values.put(COLUMN_BODY, body);
            values.put(COLUMN_DATE, date);
            if (cc != null) values.put(COLUMN_CC, cc);
            if (bcc != null) values.put(COLUMN_BCC, bcc);
            if (reply != null) values.put(COLUMN_REPLY, reply);
            if (flags != null) values.put(COLUMN_REPLY, flags);
            if (attachments != null) values.put(COLUMN_ATTACHMENTS, attachments);
            if (names != null) values.put(COLUMN_NAMES, names);
            if (expand != null) values.put(COLUMN_EXPAND, expand);

            db.beginTransaction();
            long result = db.insertWithOnConflict(TABLE_EMAIL, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            if (result == -1) {
                throw new RuntimeException("邮件数据插入失败");
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            throw new RuntimeException("数据库插入操作异常", e);
        } finally {
            db.endTransaction();
        }
    }

    // 查询邮件
    public Map<String, Object> getEmail(String messageId) {
        // 构建查询条件
        String selection = COLUMN_MESSAGE_ID + " = ?";
        List<String> selectionArgs = new ArrayList<>();
        selectionArgs.add(messageId);

        Cursor cursor = null;
        try {
            // 执行查询
            SQLiteDatabase db = this.getReadableDatabase();
            cursor = db.query(TABLE_EMAIL,
                    null, // 所有列
                    selection, selectionArgs.toArray(new String[0]),
                    null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursorToEmail(cursor);
            }
        } catch (Exception e) {
            System.out.println("查询邮件异常 message_id = " + messageId + e.toString());
        } finally {
            cursor.close();
        }
        return null;
    }

    @SuppressLint("Range")
    private Map<String, Object> cursorToEmail(Cursor cursor) {
        Map<String, Object> message = new HashMap<>();
        message.put(COLUMN_ID, getIntSafe(cursor, COLUMN_ID, 0));
        message.put(COLUMN_MESSAGE_ID, getStrSafe(cursor, COLUMN_MESSAGE_ID, ""));
        message.put("Unread", getIntSafe(cursor, COLUMN_UNREAD, 1));
        message.put("Subject", getStrSafe(cursor, COLUMN_SUBJECT, ""));
        message.put("From", getStrSafe(cursor, COLUMN_FROM, ""));
        message.put("To", getStrSafe(cursor, COLUMN_TO, ""));
        message.put("CC", getStrSafe(cursor, COLUMN_CC, ""));
        message.put("Bcc", getStrSafe(cursor, COLUMN_BCC, ""));
        message.put("Date", getStrSafe(cursor, COLUMN_DATE, ""));
        message.put("ReplyToList", getStrSafe(cursor, COLUMN_REPLY, ""));
        message.put(COLUMN_BODY, getStrSafe(cursor, COLUMN_BODY, ""));

        String context_str = getStrSafe(cursor, COLUMN_ATTACHMENTS, "");
        try {
            JSONArray jsonObject = new JSONArray(context_str);
            message.put(COLUMN_ATTACHMENTS, jsonToList(jsonObject));
        } catch (JSONException e) {
            message.put(COLUMN_ATTACHMENTS, new ArrayList<>());
        }

        context_str = getStrSafe(cursor, COLUMN_FLAGS, "");
        try {
            JSONArray jsonObject = new JSONArray(context_str);
            message.put(COLUMN_FLAGS, jsonToList(jsonObject));
        } catch (JSONException e) {
            message.put(COLUMN_FLAGS, new ArrayList<>());
        }

        context_str = getStrSafe(cursor, COLUMN_NAMES, "");
        try {
            JSONObject jsonObject = new JSONObject(context_str);
            message.put(COLUMN_NAMES, jsonToMap(jsonObject));
        } catch (JSONException e) {
            message.put(COLUMN_NAMES, new JSONObject());
        }

        context_str = getStrSafe(cursor, COLUMN_EXPAND, "");
        try {
            JSONObject jsonObject = new JSONObject(context_str);
            message.put(COLUMN_EXPAND, jsonToMap(jsonObject));
        } catch (JSONException e) {
            message.put(COLUMN_EXPAND, new JSONObject());
        }

        return message;
    }




    /**
     * 从服务器获取邮件
     */
    public Map<String, Object> readEmailFromServerAndSave(String messageId, String userAccount, String mailbox) {
        try {

            Map<String, Object> dict = new HashMap<>();
            dict.put("user_account", userAccount);
            dict.put("mailbox", mailbox);
            dict.put("message_id", messageId);
            String jsonInputString = dictToJsonStr(dict);

            // Specify the URL for the HTTP POST request
            URL url = new URL(FETCH_EMAIL_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");

            // Write the JSON payload to the request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Get the response code
            int responseCode = connection.getResponseCode();
            System.out.println("==> 邮件读取状态: " + responseCode);

            // Read the response from the server
            String responseData = null;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                responseData = response.toString();
                System.out.println("==> 邮件查询返回值: " + responseData);
            }
            // Close the connection
            connection.disconnect();

            // 查询成功，将查询到的消息写入文件
            JSONObject jsonObject = new JSONObject(responseData);
            int code = jsonObject.getInt("code");
            if (code == 20000) {
                JSONObject email = jsonObject.getJSONObject("data");
                save(email.getString("message_id"), email.getBoolean("Unread") ? 1 : 0,
                        email.getString("Subject"), email.getString("From"),
                        email.getString("To"), email.getString("CC"), email.getString("Bcc"),
                        email.getString("Date"), email.getString("flags"),
                        email.getString("ReplyToList"), email.getString("body"),
                        email.getString("attachments"), email.getString("names"), null);

                //  返回查询到的内容
                Map<String, Object> message = new HashMap<>();
                message.put(COLUMN_MESSAGE_ID, email.isNull(COLUMN_MESSAGE_ID) ? "" : email.getString(COLUMN_MESSAGE_ID));
                message.put("Unread", email.optBoolean("Unread", false));
                message.put("Subject", email.isNull("Subject") ? "" : email.getString("Subject"));
                message.put("From", email.isNull("From") ? "" : email.getString("From"));
                message.put("To", email.isNull("To") ? "" : email.getString("To"));
                message.put("CC", email.isNull("CC") ? "" : email.getString("CC"));
                message.put("Bcc", email.isNull("Bcc") ? "" : email.getString("Bcc"));
                message.put("Date", email.isNull("Date") ? "" : email.getString("Date"));
                message.put("ReplyToList", email.isNull("ReplyToList") ? "" : email.getString("ReplyToList"));
                message.put(COLUMN_BODY, email.isNull("body") ? "" : email.getString("body"));

                String context_str = email.optString("attachments", null);
                try {
                    JSONArray jsonArray = new JSONArray(context_str);
                    message.put(COLUMN_ATTACHMENTS, jsonToList(jsonArray));
                } catch (JSONException e) {
                    message.put(COLUMN_ATTACHMENTS, new ArrayList<>());
                }

                context_str = email.getString("flags");
                try {
                    JSONArray jsonArray = new JSONArray(context_str);
                    message.put(COLUMN_FLAGS, jsonToList(jsonArray));
                } catch (JSONException e) {
                    message.put(COLUMN_FLAGS, new ArrayList<>());
                }

                context_str = email.getString("names");
                try {
                    JSONObject jsonArray = new JSONObject(context_str);
                    message.put(COLUMN_NAMES, jsonToMap(jsonArray));
                } catch (JSONException e) {
                    message.put(COLUMN_NAMES, new JSONObject());
                }

                return message;
            } else {
                return null;
            }
        } catch (Exception e) {
            System.out.println("==> 邮件查询异常: " + e.toString());
        }
        return null;
    }


    private int getIntSafe(Cursor cursor, String columnName, int defaultValue) {
        try {
            int index = cursor.getColumnIndex(columnName);
            return index != -1 ? cursor.getInt(index) : defaultValue;
        } catch (Exception e) {
            System.out.println("数据读取失败" + e.toString());
            return defaultValue;
        }
    }

    private String getStrSafe(Cursor cursor, String columnName, String defaultValue) {
        try {
            int index = cursor.getColumnIndex(columnName);
            if (index != -1) {
                String value = cursor.getString(index);
                if (value == null || value.equals("null") || value.equals("[]") || value.equals("{}") || value.equals("NULL")) {
                    return defaultValue;
                }
                return value;
            } else {
                return defaultValue;
            }
//            return index != -1 ? cursor.getString(index) : defaultValue;
        } catch (Exception e) {
            System.out.println("数据读取失败" + e.toString());
            return defaultValue;
        }
    }

    // ======================== 其他工具方法 ========================

    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_EMAIL, null, null);
//        db.delete(TABLE_CONTACTS, null, null);
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

    public static List<Map<String, Object>> jsonToList(JSONArray jsonArray) throws JSONException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            Object value = jsonArray.get(i);
            if (value instanceof JSONObject) {
                list.add(jsonToMap((JSONObject) value));
            } else {
                // 如果数组中包含非对象元素（如字符串、数字等），可根据需求处理
                // 但返回类型是 List<Map<...>>，无法容纳非 Map 元素，所以通常应抛出异常或跳过
                throw new JSONException("Array element at index " + i + " is not a JSONObject");
            }
        }
        return list;
    }

    public static List<String> jsonArrayToList(JSONArray jsonArray) throws JSONException {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            list.add(jsonArray.getString(i));
        }
        return list;
    }

    public String dictToJsonStr(Map<String, Object> dict) {
        String jsonString = "";
        try {
            // Create a JSON object
            JSONObject jsonObject = new JSONObject();
            for (Map.Entry<String, Object> entry : dict.entrySet()) {
                jsonObject.put(entry.getKey(), entry.getValue());
            }

            jsonString = jsonObject.toString();
            System.out.println("JSON String: " + jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonString;
    }



}
