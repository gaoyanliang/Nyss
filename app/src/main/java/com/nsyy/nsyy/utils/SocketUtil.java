package com.nsyy.nsyy.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.nsyy.nsyy.MainActivity;
import com.nsyy.nsyy.config.MySharedPreferences;
import com.nsyy.nsyy.config.NsyyConfig;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Map;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SocketUtil {
    private volatile static SocketUtil uniqueInstance;

    private static Socket socket;
    private static Integer clientId; // 客户端唯一ID
    private Context context;
    private static Handler heartbeatHandler;
    private static Runnable heartbeatRunnable;
    private static final long HEARTBEAT_INTERVAL = 30000; // 30秒
    private static WeakReference<MainActivity> activityRef;

    public void setContext(MainActivity activity) {
        this.activityRef = new WeakReference<>(activity);
        this.context = activity.getApplicationContext();
    }

    @Override
    public String toString() {
        return "SocketUtil{" +
                "client_id=" + clientId +
                '}';
    }

    //采用Double CheckLock(DCL)实现单例
    public static SocketUtil getInstance() {
        if (uniqueInstance == null) {
            synchronized (SocketUtil.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new SocketUtil();
                }
            }
        }
        return uniqueInstance;
    }

    private SocketUtil() {
    }


    public static void connect() {
        if (isConnected()) {
            return;
        }
        Log.d("===> SocketIO", "开始初始化 socket");
        new Thread(() -> {
            while (true) {
                int persId = MySharedPreferences.getSharedPreferences().getInt("pers_id", 0);
                Log.d("===> SocketIO", "成功获取到 pers_id: " + persId);
                if (persId != 0) {
                    try {
                        clientId = persId;
                        Log.d("===> SocketIO", "准备开始连接socket" + NsyyConfig.SERVER_URL);
                        // 配置连接选项
                        IO.Options opts = new IO.Options();
                        opts.reconnection = true; // 允许自动重连
                        opts.reconnectionAttempts = Integer.MAX_VALUE; // 无限重连尝试
                        opts.reconnectionDelay = 1000; // 初始重连延迟1秒
                        opts.reconnectionDelayMax = 5000; // 最大重连延迟5秒
                        opts.path = "/socket.io"; // 默认路径（若服务端未修改）

                        // 初始化 Socket
                        socket = IO.socket(NsyyConfig.SERVER_URL, opts);

                        // 设置事件监听
                        setupListeners();

                        // 建立连接
                        socket.connect();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("===> SocketIO", "连接异常: " + e.getMessage());
                    }
                    break;
                }

                try {
                    // Check every second
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private static void setupListeners() {
        // 1. 监听连接成功事件
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d("===> SocketIO", "成功连接, socket_id = " + socket.id());

                sendClientId();
                // 启动心跳检测
                startHeartbeat();
            }
        });

        // 2. 监听服务端消息（对应服务端的 'message' 事件）
        socket.on("message", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    if (args[0] instanceof String) {
                        // 处理字符串消息
                        String message = (String) args[0];
                        Log.d("===> SocketIO", "Received string message: " + message);
                    } else if (args[0] instanceof JSONObject) {
                        // 处理JSON对象消息
                        JSONObject jsonMessage = (JSONObject) args[0];
                        Log.d("===> SocketIO", "Received JSON message: " + jsonMessage.toString());

                        int socketType = jsonMessage.optInt("type");
                        // 100=聊天消息  400=通知消息
                        if (socketType == 400 || socketType == 100) {
                            // 转为 Map<String, Object>
                            Map<String, Object> map = JSON.parseObject(
                                    jsonMessage.toString(),  // 先转为 String
                                    new TypeReference<Map<String, Object>>() {}
                            );
                            // 将消息保存至本地
                            if (map.containsKey("socket_data")){
                                Map<String, Object> data = (Map<String, Object>) map.get("socket_data");
                                MainActivity.getDatabaseHelper().notificationSaveToLocal(0, clientId, data);
                            }

                        } else if (socketType == 500) {
                            // 500=邮件
                            Map<String, Object> map = JSON.parseObject(
                                    jsonMessage.toString(),  // 先转为 String
                                    new TypeReference<Map<String, Object>>() {}
                            );
                            if (map.containsKey("socket_data")){
                                Map<String, Object> data = (Map<String, Object>) map.get("socket_data");
                                String opType = (String) data.get("op_type");
                                if (opType.isEmpty()) {
                                    return;
                                }
                                if (opType.equals("sync")) {
                                    MainActivity.getEmailHelper().readEmailFromServerAndSave((String) data.get("message_id"),
                                            (String) data.get("user_account"), (String) data.get("mailbox"));
                                } else if (opType.equals("delete")) {
                                    MainActivity.getEmailHelper().deleteEmail((String) data.get("message_id"));
                                }
                            }
                        }

                        // 接收到的消息，通过js传递给前端
                        MainActivity activity = activityRef.get();
                        if (activity != null) {
                            System.out.println("===> onSocketMessageReceived " + jsonMessage.toString());
                            activity.onSocketMessageReceived(jsonMessage.toString());
                        }
                    } else {
                        Log.d("===> SocketIO", "Received unknown message type: " + args[0].getClass().getName());
                    }
                } catch (Exception e) {
                    Log.e("===> SocketIO", "Error processing message", e);
                }
            }
        });

        // 4. 监听断开事件
        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d("===> SocketIO", "断开 socket 连接");
                stopHeartbeat();

                // 更新 UI（需切到主线程）
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("与服务器断开连接");
//                        Toast.makeText(context, "与服务器断开连接", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // 5. 监听异常事件
        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d("===> SocketIO", "连接错误: " + args[0]);
            }
        });

    }

    // 发送客户端ID获取session ID
    private static void sendClientId() {
        try {
            JSONObject data = new JSONObject();
            data.put("m_app", clientId);

            socket.emit("message", data, new Ack() {
                @Override
                public void call(Object... args) {
                    // 服务器返回的响应会在 args 中
                    if (args != null && args.length > 0) {
                        Object response = args[0];
                        Log.d("===> SocketIO", "Sent client ID: " + clientId
                                + "   " + socket.id() + "Server response: " + response.toString());
                    } else {
                        Log.d("===> SocketIO", "Sent client ID: "
                                + clientId + "   " + "Server acknowledged but no data returned.");
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("===> SocketIO", "发送client ID失败: " + e.getMessage());
        }
    }

    // 启动心跳检测
    private static void startHeartbeat() {
        stopHeartbeat(); // 先停止已有的心跳

        heartbeatHandler = new Handler(Looper.getMainLooper());
        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (socket != null && socket.connected()) {
                    Log.d("===> SocketIO", "Socket连接正常，socket id: " + socket.id());
                    heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL);
                }
            }
        };
        heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL);
    }

    // 停止心跳检测
    private static void stopHeartbeat() {
        if (heartbeatHandler != null && heartbeatRunnable != null) {
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
        }
    }


    // 断开连接
    public static void disconnect() {
        if (socket != null) {
            stopHeartbeat();
            socket.disconnect();
            socket = null;
        }
        clientId = 0;
    }

    // 获取当前连接状态
    public static boolean isConnected() {
        return socket != null && socket.connected();
    }

}

