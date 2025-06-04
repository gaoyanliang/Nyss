package com.example.nsyy.server.api;

import java.util.Map;

public class NotificationParam {

    // 通知标题
    public String title;

    // 通知内容
    public String context;

    public int cur_user_id;
    public int in_chat;
    public Map<String, Object> message;

    public NotificationParam() {
    }

    public NotificationParam(String title, String context, int cur_user_id, int in_chat, Map<String, Object> message) {
        this.title = title;
        this.context = context;
        this.cur_user_id = cur_user_id;
        this.in_chat = in_chat;
        this.message = message;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public int getCurUserId() {
        return cur_user_id;
    }

    public void setCurUserId(int curUserId) {
        this.cur_user_id = curUserId;
    }

    public int getIn_chat() {
        return in_chat;
    }

    public void setIn_chat(int in_chat) {
        this.in_chat = in_chat;
    }

    public Map<String, Object> getMessage() {
        return message;
    }

    public void setMessage(Map<String, Object> message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "NotificationParam{" +
                "title='" + title + '\'' +
                ", context='" + context + '\'' +
                ", curUserId=" + cur_user_id +
                ", in_chat=" + in_chat +
                ", message=" + message +
                '}';
    }
}
