package com.nsyy.nsyy.server.api;

public class UpdateLocalMessageParam {

    public String update_msg_url;
    public String update_chat_list_url;
    public Integer cur_user_id;

    public UpdateLocalMessageParam() {

    }

    public UpdateLocalMessageParam(String update_msg_url, String update_chat_list_url, Integer cur_user_id) {
        this.update_msg_url = update_msg_url;
        this.update_chat_list_url = update_chat_list_url;
        this.cur_user_id = cur_user_id;
    }

    public String getUpdate_msg_url() {
        return update_msg_url;
    }

    public void setUpdate_msg_url(String update_msg_url) {
        this.update_msg_url = update_msg_url;
    }

    public String getUpdate_chat_list_url() {
        return update_chat_list_url;
    }

    public void setUpdate_chat_list_url(String update_chat_list_url) {
        this.update_chat_list_url = update_chat_list_url;
    }

    public Integer getCur_user_id() {
        return cur_user_id;
    }

    public void setCur_user_id(Integer cur_user_id) {
        this.cur_user_id = cur_user_id;
    }

    @Override
    public String toString() {
        return "UpdateLocalMessageParam{" +
                "update_msg_url='" + update_msg_url + '\'' +
                ", update_chat_list_url='" + update_chat_list_url + '\'' +
                ", cur_user_id=" + cur_user_id +
                '}';
    }
}
