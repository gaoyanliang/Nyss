package com.example.nsyy.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 监听 web server 服务状态
 */
public class NsyyServerBroadcastReceiver extends BroadcastReceiver {

    private static final String ACTION = "NsyyServerBroadcastReceiver";

    private static final String CMD_KEY = "CMD_KEY";
    private static final String MESSAGE_KEY = "MESSAGE_KEY";

    private static final int CMD_VALUE_START = 1;
    private static final int CMD_VALUE_ERROR = 2;
    private static final int CMD_VALUE_STOP = 4;
    private ServerStateListener serverStateListener;

    public NsyyServerBroadcastReceiver(ServerStateListener serverStateListener) {
        this.serverStateListener = serverStateListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION.equals(action)){
            int cmd = intent.getIntExtra(CMD_KEY, 0);
            switch (cmd){
                case CMD_VALUE_START:
                    if (serverStateListener!=null){
                        String ip = intent.getStringExtra(MESSAGE_KEY);
                        serverStateListener.onStart(ip);
                    }
                    break;
                case CMD_VALUE_STOP:
                    if (serverStateListener!=null){
                        serverStateListener.onStop();
                    }
                    break;
                case CMD_VALUE_ERROR:
                    String error = intent.getStringExtra(MESSAGE_KEY);
                    serverStateListener.onError(error);
                    break;
                default:
            }
        }


    }

    public static void onServerStart(Context context, String hostAddress) {
        sendBroadcast(context, CMD_VALUE_START, hostAddress);
    }

    public static void onServerStop(Context context) {
        sendBroadcast(context, CMD_VALUE_STOP,null);
    }

    public static void onServerError(Context context, String error) {
        sendBroadcast(context, CMD_VALUE_ERROR, error);
    }

    private static void sendBroadcast(Context context, int cmd, String message) {
        Intent broadcast = new Intent(ACTION);
        broadcast.putExtra(CMD_KEY, cmd);
        broadcast.putExtra(MESSAGE_KEY, message);
        context.sendBroadcast(broadcast);
    }

    public static abstract class ServerStateListener{
        public abstract void onStart(String hostAddress);
        public abstract void onStop();
        public void onError(String error){}
    }
}
