package com.example.nsyy.server.api.base;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
 * 提供的默认的标注返回api
 */
public class ApiResult<T> implements Parcelable {

    public final static String CODE = "code";
    public final static String MSG = "msg";
    public final static String DATA = "data";

    @SerializedName(value = CODE)
    private int code;
    @SerializedName(value = MSG)
    private String msg = "";
    @SerializedName(value = DATA)
    private T data;

    public ApiResult() {

    }

    protected ApiResult(Parcel in) {
        code = in.readInt();
        msg = in.readString();
    }

    public static final Creator<ApiResult> CREATOR = new Creator<ApiResult>() {
        @Override
        public ApiResult createFromParcel(Parcel in) {
            return new ApiResult(in);
        }

        @Override
        public ApiResult[] newArray(int size) {
            return new ApiResult[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(code);
        dest.writeString(msg);
    }

    public int getCode() {
        return code;
    }

    public ApiResult<T> setCode(int code) {
        this.code = code;
        return this;
    }

    public String getMsg() {
        return msg;
    }

    public ApiResult<T> setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public ApiResult<T> setData(T data) {
        this.data = data;
        return this;
    }

    public ApiResult<T> setError(int code, String msg) {
        this.code = code;
        this.msg = msg;
        return this;
    }

    /**
     * 获取请求响应的数据，自定义api的时候需要重写【很关键】
     *
     * @return 响应的数据
     */
    public T getData() {
        return data;
    }

    /**
     * 是否请求成功,自定义api的时候需要重写【很关键】
     *
     * @return 是否请求成功
     */
    public boolean isSuccess() {
        return getCode() == 0;
    }

    @Override
    public String toString() {
        return "ApiResult{" +
                "code='" + code + '\'' +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }
}
