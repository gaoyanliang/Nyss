package com.example.nsyy.server.api;

import android.content.Intent;

public class SpeechInfo {

    // 语速 默认为 1.3f
    public float rate;

    // 语音播报人 名字 默认为 xiaozhang
    public String name;

    // 国家 默认 zh
    public String locale;

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


    public SpeechInfo(float rate, String name, String locale, String info, int interval) {
        this.rate = rate;
        this.name = name;
        this.locale = locale;
        this.interval = interval;
        this.info = info;
    }

    public SpeechInfo() {
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    public float getRate() {
        return rate;
    }

    public void setRate(float rate) {
        this.rate = rate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }


}
