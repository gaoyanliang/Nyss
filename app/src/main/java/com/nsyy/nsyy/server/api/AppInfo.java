package com.nsyy.nsyy.server.api;

import com.alibaba.fastjson.annotation.JSONField;

public class AppInfo {
    @JSONField(name = "isSuccess")
    public boolean isSuccess;

    @JSONField(name = "code")
    public int code;

    @JSONField(name = "errorMsg")
    public String errorMsg;

    @JSONField(name = "version")
    public Double version;

    @JSONField(name = "type")
    public String type;

    @JSONField(name = "detail")
    public String detail;

    public AppInfo() {
    }

    public AppInfo(boolean isSuccess, int code, String errorMsg, Double version, String type, String detail) {
        this.isSuccess = isSuccess;
        this.code = code;
        this.errorMsg = errorMsg;
        this.version = version;
        this.type = type;
        this.detail = detail;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public Double getVersion() {
        return version;
    }

    public void setVersion(Double version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    @Override
    public String toString() {
        return "AppInfo{" +
                "isSuccess=" + isSuccess +
                ", code=" + code +
                ", errorMsg='" + errorMsg + '\'' +
                ", version=" + version +
                ", type='" + type + '\'' +
                ", detail='" + detail + '\'' +
                '}';
    }
}
