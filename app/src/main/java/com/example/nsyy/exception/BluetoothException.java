package com.example.nsyy.exception;

/**
 * 蓝牙相关操作异常
 */
public class BluetoothException extends Exception{

    public static final int EXCEPTION_CODE = 1000;

    public int code;
    public String msg;

    public BluetoothException(String msg) {
        this.code = EXCEPTION_CODE;
        this.msg = msg;
    }
}
