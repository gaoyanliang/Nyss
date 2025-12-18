package com.nsyy.nsyy.server.api;

public class SpeechInfo {

    // 语速 默认为 1.3f
    public float rate;

    // 语音播报人 名字 默认为 xiaozhang
    public String name;

    // 国家 默认 zh
    public String locale;

    public SpeechInfo() {
    }

    public SpeechInfo(float rate, String name, String locale) {
        this.rate = rate;
        this.name = name;
        this.locale = locale;
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

    @Override
    public String toString() {
        return "SpeechInfo{" +
                "rate=" + rate +
                ", name='" + name + '\'' +
                ", locale='" + locale + '\'' +
                '}';
    }
}

