/*
 * Copyright 2018 Zhenjie Yan.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.nsyy.server.api;

import com.alibaba.fastjson.annotation.JSONField;


public class ReturnData {

    @JSONField(name = "isSuccess")
    public boolean isSuccess;

    @JSONField(name = "code")
    public int code;

    @JSONField(name = "errorMsg")
    public String errorMsg;

    @JSONField(name = "data")
    public Object data;

    public ReturnData(boolean isSuccess, int code, String errorMsg, Object data) {
        this.isSuccess = isSuccess;
        this.code = code;
        this.errorMsg = errorMsg;
        this.data = data;
    }

    public ReturnData() {
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

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "ReturnData{" +
                "isSuccess=" + isSuccess +
                ", code=" + code +
                ", errorMsg='" + errorMsg + '\'' +
                ", data=" + data +
                '}';
    }



    /**
     * 约定异常
     */
    public static class ERROR {

        /**
         * 未知错误
         */
        public static final int UNKNOWN = 5000;

        /**
         * 获取地址异常
         */
        public static final int FAILED_TO_GET_LOCATION = UNKNOWN + 1;

        /**
         * 消息通知异常
         */
        public static final int FAILED_NOTIFICATION = FAILED_TO_GET_LOCATION + 1;

        /**
         * 搜索蓝牙设备异常
         */
        public static final int FAILED_SEARCH_BLUETOOTH = FAILED_NOTIFICATION + 1;

    }
}