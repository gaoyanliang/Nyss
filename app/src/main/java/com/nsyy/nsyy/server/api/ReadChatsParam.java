package com.nsyy.nsyy.server.api;

public class ReadChatsParam {

    public Integer user_id;
    public ReadChatsParam() {

    }

    public ReadChatsParam(Integer user_id) {
        this.user_id = user_id;
    }

    public Integer getUser_id() {
        return user_id;
    }

    public void setUser_id(Integer user_id) {
        this.user_id = user_id;
    }

    @Override
    public String toString() {
        return "ReadChatsParam{" +
                "user_id=" + user_id +
                '}';
    }
}
