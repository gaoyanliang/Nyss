package com.example.nsyy.server.api;

import android.content.Intent;

public class SpeechInfo {

    // 语音播报内容
    public String info;

    // 间隔时间（分钟）
    public Integer interval;

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public SpeechInfo(String info, Integer interval) {
        this.info = info;
        this.interval = interval;
    }

    public SpeechInfo() {
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    @Override
    public String toString() {
        return "SpeechInfo{" +
                "info='" + info + '\'' +
                ", interval=" + interval +
                '}';
    }
}
