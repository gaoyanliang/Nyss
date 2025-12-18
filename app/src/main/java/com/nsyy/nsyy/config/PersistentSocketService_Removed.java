package com.nsyy.nsyy.config;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.nsyy.nsyy.MainActivity;
import com.nsyy.Nsyy.R;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


// 废弃⚠️

public class PersistentSocketService_Removed extends Service {
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "socket_channel";
    private Socket socket;
    private static final String SERVER_URL = "http://192.168.124.53:6080/echo"; // 替换为服务端地址
    private Integer clientId; // 客户端唯一ID
    private Context context;
    private Handler heartbeatHandler;
    private Runnable heartbeatRunnable;
    private static final long HEARTBEAT_INTERVAL = 30000; // 30秒

    private final WeakReference<MainActivity> activityRef;
    private PowerManager.WakeLock wakeLock;

    public PersistentSocketService_Removed(MainActivity activity, Integer clientId) {
        this.activityRef = new WeakReference<>(activity);
        this.context = activity.getApplicationContext();
        this.clientId = clientId;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        acquireWakeLock();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());
        connect();
        return START_STICKY;
    }

    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::SocketWakeLock"
        );
        wakeLock.acquire();
    }

    public void connect() {
        try {
            Log.d("===> SocketIO", "准备开始连接socket" + SERVER_URL);
            // 配置连接选项
            IO.Options opts = new IO.Options();
            opts.reconnection = true; // 允许自动重连
            opts.reconnectionAttempts = Integer.MAX_VALUE; // 无限重连尝试
            opts.reconnectionDelay = 1000; // 初始重连延迟1秒
            opts.reconnectionDelayMax = 5000; // 最大重连延迟5秒
            opts.path = "/socket.io"; // 默认路径（若服务端未修改）

            // 初始化 Socket
            socket = IO.socket(SERVER_URL, opts);

            // 设置事件监听
            setupListeners();

            // 建立连接
            socket.connect();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("===> SocketIO", "连接异常: " + e.getMessage());
        }
    }

    private void setupListeners() {
        // 1. 监听连接成功事件
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d("===> SocketIO", "Connected to server    " + socket.id());

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
                            Map<String, Object> data = (Map<String, Object>) map.get("data");

                            // 弹框通知
                            Map<String, String> title = extractTitleFromMap(data);
                            Log.d("===> SocketIO", "title " + title.toString());
//                            if (!title.get("title").isEmpty()) {
//                                NotificationUtil.getInstance().createNotificationForHigh(title.get("title"), title.get("context"));
//                            }

                            // 将消息保存至本地
                            if (data.containsKey("message")) {
                                Map<String, Object> message = (Map<String, Object>) data.get("message");
                                MainActivity.getDatabaseHelper().notificationSaveToLocal(0, clientId, message);
//                                FileHelper.getInstance().notificationSaveToLocal(0, clientId, message);
                            }
                        }

                        // 接收到的消息，通过js传递给前端
                        MainActivity activity = activityRef.get();
                        if (activity != null) {
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
                Log.d("===> SocketIO", "Disconnected");
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

    // 提取 title 的方法
    public static Map<String, String> extractTitleFromMap(Map<String, Object> map) {
        Map<String, String> retMap = new HashMap<>();
        retMap.put("title", "");
        retMap.put("context", "");
        try {
            // 1. 先尝试从顶层直接获取 title
            if (map.containsKey("title") && map.get("title") != null) {
                retMap.put("title", map.get("title").toString());
                retMap.put("context", map.getOrDefault("context", "").toString());
            }

            // 2. 如果顶层没有，尝试从 message.context 获取
            if (map.containsKey("message")) {
                Map<String, Object> data = (Map<String, Object>) map.get("message");
                Object chatTypeObj = data.get("chat_type");
                int chatType;
                if (chatTypeObj instanceof Double) {
                    chatType = ((Double) chatTypeObj).intValue(); // Double → int
                } else if (chatTypeObj instanceof Integer) {
                    chatType = (Integer) chatTypeObj; // 直接取 Integer
                } else {
                    chatType = 0; // 默认值或抛异常
                }

                if (chatType == 0) {
                    // 如果是通知消息 取 context 中的title
                    Map<String, Object> context = (Map<String, Object>) data.get("context");
                    retMap.put("title", context.getOrDefault("title", "").toString());
                    retMap.put("context", context.getOrDefault("description", "").toString());
                }
            }

            return retMap;
        } catch (Exception e) {
            // 处理类型转换错误等异常
            Log.d("===> SocketIO", "解析通知消息异常: " + e);
            return retMap;
        }
    }




    // 发送客户端ID获取session ID
    private void sendClientId() {
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
    private void startHeartbeat() {
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
    private void stopHeartbeat() {
        if (heartbeatHandler != null && heartbeatRunnable != null) {
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
        }
    }

    // 发送消息到服务端
    public void sendMessage(String data) {
        if (socket != null && socket.connected()) {
            socket.emit("message", data, new Ack() {
                @Override
                public void call(Object... args) {
                    // 服务器返回的响应会在 args 中
                    if (args != null && args.length > 0) {
                        Object response = args[0];
                        Log.d("===> SocketIO", "Server response: " + response.toString());
                    } else {
                        Log.d("===> SocketIO", "Server acknowledged but no data returned.");
                    }
                }
            });
        } else {
            Log.e("===> SocketIO", "发送失败: 连接未建立");
        }
    }

    // 断开连接
    public void disconnect() {
        if (socket != null) {
            stopHeartbeat();
            socket.disconnect();
            socket = null;
        }
    }

    // 获取当前连接状态
    public boolean isConnected() {
        return socket != null && socket.connected();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Socket连接中")
                .setContentText("保持后台连接")
                .setSmallIcon(R.drawable.ic_notifications_24)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Socket连接",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("保持Socket后台连接");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
}