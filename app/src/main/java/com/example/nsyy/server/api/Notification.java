package com.example.nsyy.server.api;

public class Notification {

    // 通知标题
    public String title;

    // 通知内容
    public String context;

    public Notification() {
    }

    public Notification(String title, String context) {
        this.title = title;
        this.context = context;
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

    @Override
    public String toString() {
        return "Notification{" +
                "title='" + title + '\'' +
                ", context='" + context + '\'' +
                '}';
    }
}
