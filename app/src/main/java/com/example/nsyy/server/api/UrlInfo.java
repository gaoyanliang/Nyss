package com.example.nsyy.server.api;

public class UrlInfo {
    public String url;

    public UrlInfo() {
    }

    public UrlInfo(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "UrlInfo{" +
                "url='" + url + '\'' +
                '}';
    }
}
